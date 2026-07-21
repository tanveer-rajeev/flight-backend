package com.aerionsoft.application.repository.client;

import com.aerionsoft.application.entity.client.InvoiceDynamicItem;
import com.aerionsoft.application.entity.client.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceDynamicItemRepository extends JpaRepository<InvoiceDynamicItem, Long> {
    List<InvoiceDynamicItem> findByInvoiceItemId(Long invoiceItemId);

    void deleteByInvoiceItemIn(List<InvoiceItem> existingItems);
}
