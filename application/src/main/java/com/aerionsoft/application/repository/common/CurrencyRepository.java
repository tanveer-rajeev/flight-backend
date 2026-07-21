package com.aerionsoft.application.repository.common;

import com.aerionsoft.application.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    List<Currency> findAllByOrderByIdAsc();
    Currency findByCode(String code);
    List<Currency> findByProviderId(String providerId);
    List<Currency> findByProviderIdAndChannelIsNullOrderByIdAsc(String providerId);
    List<Currency> findByProviderIdAndChannelOrderByIdAsc(String providerId, String channel);

    Currency findByCodeAndProviderId(String code, String providerId);
    Currency findByCodeAndProviderIdAndChannel(String code, String providerId, String channel);
    Currency findByCodeAndProviderIdAndChannelIsNull(String code, String providerId);

    Currency findByCodeIgnoreCaseAndProviderIdIgnoreCase(String code, String providerId);
    Currency findByCodeIgnoreCaseAndProviderIdIgnoreCaseAndChannelIgnoreCase(String code, String providerId, String channel);
    Currency findByCodeIgnoreCaseAndProviderIdIgnoreCaseAndChannelIsNull(String code, String providerId);

    List<Currency> findByCodeInOrderByIdAsc(List<String> codes);
    List<Currency> findByProviderIdAndCodeInOrderByIdAsc(String providerId, List<String> codes);

    @Query("""
            SELECT c FROM Currency c
            WHERE UPPER(c.code) = UPPER(:code)
              AND UPPER(c.providerId) = UPPER(:providerId)
              AND UPPER(c.channel) = UPPER(:channel)
            """)
    Currency findChannelCurrency(
            @Param("code") String code,
            @Param("providerId") String providerId,
            @Param("channel") String channel);

    @Query("""
            SELECT c FROM Currency c
            WHERE UPPER(c.code) = UPPER(:code)
              AND UPPER(c.providerId) = UPPER(:providerId)
              AND (c.channel IS NULL OR TRIM(c.channel) = '')
            """)
    Currency findProviderDefaultCurrency(
            @Param("code") String code,
            @Param("providerId") String providerId);
}
