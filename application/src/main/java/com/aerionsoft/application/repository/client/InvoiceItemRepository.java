package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.Invoice;
import com.aerionsoft.application.entity.client.InvoiceItem;
import com.aerionsoft.application.entity.client.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    List<InvoiceItem> findByInvoice(Invoice invoice);

    List<InvoiceItem> findBySupplier(Supplier supplier);

    List<InvoiceItem> findByAccountHeadIdIn(List<Long> accountHeadIds);

    @Query("SELECT COALESCE(SUM((ii.sellPrice * ii.quantity) - (ii.buyPrice * ii.quantity)), 0) " +
            "FROM InvoiceItem ii " +
            "WHERE ii.accountHead.id IN :accountHeadIds")
    BigDecimal getTotalInvoiceRevenueByAccountHeadIds(@Param("accountHeadIds") List<Long> accountHeadIds);
}
