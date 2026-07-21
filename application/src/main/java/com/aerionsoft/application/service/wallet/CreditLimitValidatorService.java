package com.aerionsoft.application.service.wallet;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.service.business.BusinessService;

import com.aerionsoft.application.dto.business.BusinessDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/**
 * Service for validating user balance including credit limits.
 * Consolidates repeated credit limit validation logic to prevent duplication.
 */
@Service
public class CreditLimitValidatorService {

    private static final Logger log = Logger.getLogger(CreditLimitValidatorService.class.getName());

    @Autowired
    private BusinessService businessService;

    /**
     * Total spendable amount: positive wallet balance plus remaining credit limit.
     * Existing debt does not reduce available credit (e.g. balance -2000 + credit 1000 → can spend 1000).
     */
    public static double calculateAvailableBalance(double userBalance, double creditLimit) {
        return Math.max(0, userBalance) + creditLimit;
    }

    /**
     * Amount of a debit that must be drawn from credit limit rather than wallet balance.
     */
    public static double calculateCreditUsed(double balanceBefore, double amount) {
        return Math.max(0, amount - Math.max(0, balanceBefore));
    }

    /**
     * Agency child users share the parent (mother) wallet balance.
     */
    public static User resolveWalletUser(
            User user) {
        return user.getParentUser() != null ? user.getParentUser() : user;
    }

    public static Long resolveWalletUserId(User user) {
        return resolveWalletUser(user).getId();
    }

    /**
     * Checks if a user has sufficient balance (wallet + available credit limit) for a transaction.
     *
     * @param userId The user ID to check
     * @param userBalance The user's current wallet balance (can be null)
     * @param requiredAmount The amount needed for the transaction
     * @return true if the user has sufficient balance/credit, false otherwise
     */
    public boolean hasSufficientBalance(Long userId, Double userBalance, Double requiredAmount) {
        if (userBalance == null) {
            userBalance = 0.0;
        }

        if (userBalance >= requiredAmount) {
            return true;
        }

        try {
            BusinessDto businessDto = businessService.getBusinessByUserId(userId);
            if (businessDto != null && businessDto.getId() != null &&
                    businessDto.getCreditLimit() != null &&
                    businessDto.getCreditLimit().compareTo(java.math.BigDecimal.ZERO) > 0) {

                double availableBalance = calculateAvailableBalance(
                        userBalance, businessDto.getCreditLimit().doubleValue());
                return availableBalance >= requiredAmount;
            }
        } catch (Exception e) {
            log.warning("Could not check credit limit for user " + userId + ": " + e.getMessage());
        }

        return false;
    }

    /**
     * Gets the available balance for a user including credit limit.
     *
     * @param userId The user ID to check
     * @param userBalance The user's current wallet balance (can be null)
     * @return The total available balance (positive wallet + credit), or just wallet if credit check fails
     */
    public double getAvailableBalance(Long userId, Double userBalance) {
        if (userBalance == null) {
            userBalance = 0.0;
        }

        try {
            BusinessDto businessDto = businessService.getBusinessByUserId(userId);
            if (businessDto != null && businessDto.getId() != null &&
                    businessDto.getCreditLimit() != null &&
                    businessDto.getCreditLimit().compareTo(java.math.BigDecimal.ZERO) > 0) {

                return calculateAvailableBalance(userBalance, businessDto.getCreditLimit().doubleValue());
            }
        } catch (Exception e) {
            log.warning("Could not check credit limit for user " + userId + ": " + e.getMessage());
        }

        return Math.max(0, userBalance);
    }
}
