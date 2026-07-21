package com.aerionsoft.application.service.errorlog;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Maintains monthly PostgreSQL range partitions for {@code error_log}.
 * Rows that landed in the default partition are moved into monthly partitions when those
 * partitions are created; only out-of-range or not-yet-partitioned rows stay in default.
 */
@Service
@Slf4j
public class ErrorLogPartitionService {

    private static final DateTimeFormatter PARTITION_SUFFIX = DateTimeFormatter.ofPattern("yyyy_MM");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializePartitions() {
        ensureMonthlyPartitions();
    }

    /** Ensures partitions exist for the current month and the next two months. */
    @Scheduled(cron = "${error.log.partition.cron:0 0 1 1 * ?}")
    public void ensureMonthlyPartitions() {
        if (!isPartitionedTable()) {
            log.debug("error_log is not partitioned yet; skipping partition maintenance");
            return;
        }

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        for (int i = 0; i < 3; i++) {
            createPartitionIfMissing(monthStart.plusMonths(i));
        }
    }

    /**
     * Drops monthly partitions whose upper bound is older than the retention cutoff.
     * The default partition is cleaned with batched deletes instead.
     */
    public int dropExpiredPartitions(LocalDateTime cutoffDate) {
        if (!isPartitionedTable()) {
            return 0;
        }

        List<String> partitions = listMonthlyPartitions();
        int dropped = 0;
        for (String partitionName : partitions) {
            LocalDateTime partitionEnd = resolvePartitionUpperBound(partitionName);
            if (partitionEnd != null && !partitionEnd.isAfter(cutoffDate)) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + partitionName);
                log.info("Dropped expired error_log partition {}", partitionName);
                dropped++;
            }
        }
        return dropped;
    }

    private boolean isPartitionedTable() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_partitioned_table pt
                JOIN pg_class c ON c.oid = pt.partrelid
                WHERE c.relname = 'error_log'
                """, Integer.class);
        return count != null && count > 0;
    }

    private List<String> listMonthlyPartitions() {
        return jdbcTemplate.queryForList("""
                SELECT child.relname
                FROM pg_inherits
                JOIN pg_class parent ON parent.oid = pg_inherits.inhparent
                JOIN pg_class child ON child.oid = pg_inherits.inhrelid
                WHERE parent.relname = 'error_log'
                  AND child.relname <> 'error_log_default'
                  AND child.relname LIKE 'error_log_y%'
                ORDER BY child.relname
                """, String.class);
    }

    private void createPartitionIfMissing(LocalDate monthStart) {
        String partitionName = "error_log_y" + monthStart.format(PARTITION_SUFFIX);
        if (partitionExists(partitionName)) {
            return;
        }

        LocalDate nextMonth = monthStart.plusMonths(1);
        String fromTs = monthStart + " 00:00:00";
        String toTs = nextMonth + " 00:00:00";

        Integer rowsInDefault = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM error_log_default
                WHERE created_at >= ?::timestamp
                  AND created_at < ?::timestamp
                """, Integer.class, fromTs, toTs);

        if (rowsInDefault != null && rowsInDefault > 0) {
            createPartitionByMovingDefaultRows(partitionName, fromTs, toTs, rowsInDefault);
            return;
        }

        try {
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS %s PARTITION OF error_log
                    FOR VALUES FROM ('%s') TO ('%s')
                    """, partitionName, fromTs, toTs));
            log.debug("Ensured error_log partition {}", partitionName);
        } catch (Exception e) {
            if (partitionExists(partitionName)) {
                log.debug("error_log partition {} already exists", partitionName);
                return;
            }
            throw e;
        }
    }

    /**
     * PostgreSQL rejects {@code CREATE TABLE ... PARTITION OF} when matching rows still
     * live in the default partition. Move those rows first, then attach the new partition.
     */
    private void createPartitionByMovingDefaultRows(
            String partitionName, String fromTs, String toTs, int expectedRows) {
        try {
            jdbcTemplate.execute((Connection connection) -> {
                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute(String.format(
                            "CREATE TABLE IF NOT EXISTS %s (LIKE error_log INCLUDING DEFAULTS INCLUDING GENERATED)",
                            partitionName));
                    statement.execute(String.format("""
                            WITH moved AS (
                                DELETE FROM error_log_default
                                WHERE created_at >= '%s'::timestamp
                                  AND created_at < '%s'::timestamp
                                RETURNING *
                            )
                            INSERT INTO %s
                            SELECT * FROM moved
                            """, fromTs, toTs, partitionName));
                    if (!partitionExists(partitionName)) {
                        statement.execute(String.format("""
                                ALTER TABLE error_log ATTACH PARTITION %s
                                FOR VALUES FROM ('%s') TO ('%s')
                                """, partitionName, fromTs, toTs));
                    }
                    connection.commit();
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
                return null;
            });
            log.info("Ensured error_log partition {} by moving {} rows out of default",
                    partitionName, expectedRows);
        } catch (Exception e) {
            if (partitionExists(partitionName)) {
                log.debug("error_log partition {} was created concurrently", partitionName);
                return;
            }
            throw e;
        }
    }

    private boolean partitionExists(String partitionName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_class child
                JOIN pg_inherits ON pg_inherits.inhrelid = child.oid
                JOIN pg_class parent ON parent.oid = pg_inherits.inhparent
                WHERE child.relname = ?
                  AND parent.relname = 'error_log'
                """, Integer.class, partitionName);
        return count != null && count > 0;
    }

    private LocalDateTime resolvePartitionUpperBound(String partitionName) {
        List<Map<String, Object>> bounds = jdbcTemplate.queryForList("""
                SELECT pg_get_expr(c.relpartbound, c.oid) AS bound
                FROM pg_class c
                WHERE c.relname = ?
                """, partitionName);

        if (bounds.isEmpty() || bounds.get(0).get("bound") == null) {
            return null;
        }

        String bound = bounds.get(0).get("bound").toString();
        int toIndex = bound.indexOf("TO ('");
        if (toIndex < 0) {
            return null;
        }

        int valueStart = toIndex + 5;
        int valueEnd = bound.indexOf('\'', valueStart);
        if (valueEnd <= valueStart) {
            return null;
        }

        String timestamp = bound.substring(valueStart, valueEnd);
        return LocalDateTime.parse(timestamp.replace(' ', 'T'));
    }
}
