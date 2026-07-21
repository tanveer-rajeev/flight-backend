package com.aerionsoft.application.service.business;

import com.aerionsoft.application.dto.salesperson.SalesPersonResponseDto;

import java.util.List;

public interface BusinessSalesPersonService {

    List<SalesPersonResponseDto> getSalesPersons(Long businessId);

    void addSalesPerson(Long businessId, Long salesPersonId);

    void removeSalesPerson(Long businessId, Long salesPersonId);

    void setSalesPersons(Long businessId, List<Long> salesPersonIds);
}
