package com.aerionsoft.application.dto.booking.core;

import com.aerionsoft.application.enums.booking.BookType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CoreBookingWrapper {

    private BookType type;

    private String resultIndex;

    private String bundleCode; // For USBANGLAAPI fare bundle selection

    @NotNull
    private String providerName;

    private String channel;

    private  List<CoreBookingRequest> bookInfoList;

}
