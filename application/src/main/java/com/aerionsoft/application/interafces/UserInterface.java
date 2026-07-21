package com.aerionsoft.application.interafces;

import com.aerionsoft.application.dto.admin.bank.WalletDepositResponse;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.entity.client.User;

import java.util.List;

public interface UserInterface {

    Long getUserIdByEmail(String email);
    String countOfAgencies();
    String countOfUsers();
    String countOfTodayAgencies();
    String countOfTodayUsers();
    Double getUserBalance(Long userId);
    UserDto getUserById(Long userId);
    List<WalletDepositResponse> getAllDepositsByAgency(Long agencyId);
    void deductUserBalance(Long userId, Double amount, String providerName);
    void deductUserBalance(Long userId, Double amount, String providerName, boolean bypassBalanceCheck);
    void deductUserBalance(Long userId, Double amount, String providerName, boolean bypassBalanceCheck,
                           String source, Long referenceId, String referenceType, Long performedBy);

    void addUserBalance(Long userId, Double amount);
    void addUserBalance(Long userId, Double amount,
                        String reason, String source, Long referenceId, String referenceType, Long performedBy);

    /**
     * Restores wallet balance and removes matching debit audit rows without creating a visible credit entry.
     */
    void silentlyUndoBookingWalletDebits(Long actorUserId, Long bookingId, java.util.List<Double> purchaseAmounts);

    User getUser(Long userId);
}
