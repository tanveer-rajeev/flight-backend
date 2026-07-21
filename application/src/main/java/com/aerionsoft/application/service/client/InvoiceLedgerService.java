package com.aerionsoft.application.service.client;

import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ResourceNotFoundException;
import com.aerionsoft.application.controller.BaseController;
import com.aerionsoft.application.dto.client.invoice.LedgerDto;
import com.aerionsoft.application.dto.client.invoice.response.LedgerResponseDto;
import com.aerionsoft.application.entity.client.Ledger;
import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.repository.user.UserRepository;
import com.aerionsoft.application.repository.client.InvoiceLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceLedgerService extends BaseController {

    private final InvoiceLedgerRepository ledgerRepository;
    private final  UserRepository userRepository;
    private final TimestampMapper timestampMapper;

    public InvoiceLedgerService(InvoiceLedgerRepository ledgerRepository, UserRepository userRepository, TimestampMapper timestampMapper) {
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.timestampMapper = timestampMapper;
    }

    /**
     * Create Ledger Service
     *
     * @param ledgerDto the Ledger data to create
     */
    public void createLedger(String provider, Long authUserId, LedgerDto ledgerDto) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        User agencyUser = null;

        if (!isAdmin) {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            agencyUser = user.getParentUser() != null ? user.getParentUser() : user;
        }

        Ledger ledger = Ledger.builder()
                .agencyId(agencyUser == null ? null : agencyUser.getId())
                .title(ledgerDto.getTitle())
                .image(ledgerDto.getImage())
                .description(ledgerDto.getDescription())
                .createdBy(authUserId)
                .createdAt(UserDateTimeUtil.now()).build();

        ledgerRepository.save(ledger);
    }

    /**
     * List of Ledger service
     *
     * @return list of Ledgers
     */
    @Transactional(readOnly = true)
    public List<LedgerResponseDto> getAllLedgers(String provider, Long authUserId) {

        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        User agencyUser = null;

        if (!isAdmin) {
            User user = userRepository.findById(authUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

            return ledgerRepository.findAllByAgencyId(agencyUser.getId()).stream().map(ledger -> {
                LedgerResponseDto dto = new LedgerResponseDto();
                dto.setId(ledger.getId());
                dto.setTitle(ledger.getTitle());
                dto.setDescription(ledger.getDescription());
                dto.setImage(ledger.getImage());
                dto.setCreatedBy(ledger.getCreatedBy());
                dto.setCreatedAt(timestampMapper.createdAt(ledger));
                dto.setUpdatedBy(ledger.getUpdatedBy());
                dto.setUpdatedAt(timestampMapper.updatedAt(ledger, ledger.getCreatedTimeOffset()));
                return dto;
            }).collect(Collectors.toList());
        }

        return ledgerRepository.findAllByAgencyIdIsNull().stream().map(ledger -> {
            LedgerResponseDto dto = new LedgerResponseDto();
            dto.setId(ledger.getId());
            dto.setTitle(ledger.getTitle());
            dto.setDescription(ledger.getDescription());
            dto.setImage(ledger.getImage());
            dto.setCreatedBy(ledger.getCreatedBy());
            dto.setCreatedAt(timestampMapper.createdAt(ledger));
            dto.setUpdatedBy(ledger.getUpdatedBy());
            dto.setUpdatedAt(timestampMapper.updatedAt(ledger, ledger.getCreatedTimeOffset()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Retrieve a ledger by its ID Service.
     *
     * @param id ledger to retrieve
     * @return the ledger
     */
    public LedgerResponseDto getLedgerById(String provider, Long userId, Long id) {
        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        User agencyUser = null;

        if (!isAdmin) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

            Ledger ledger = ledgerRepository.findByIdAndAgencyId(id, agencyUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Ledger", id));

            LedgerResponseDto dto = new LedgerResponseDto();

            dto.setId(ledger.getId());
            dto.setTitle(ledger.getTitle());
            dto.setDescription(ledger.getDescription());
            dto.setImage(ledger.getImage());
            dto.setCreatedBy(ledger.getCreatedBy());
            dto.setCreatedAt(timestampMapper.createdAt(ledger));
            dto.setUpdatedBy(ledger.getUpdatedBy());
            dto.setUpdatedAt(timestampMapper.updatedAt(ledger, ledger.getCreatedTimeOffset()));

            return dto;
        }

        Ledger ledger = ledgerRepository.findByIdAndAgencyIdIsNull(id).orElseThrow(() -> new ResourceNotFoundException("Ledger", id));

        LedgerResponseDto dto = new LedgerResponseDto();

        dto.setId(ledger.getId());
        dto.setTitle(ledger.getTitle());
        dto.setDescription(ledger.getDescription());
        dto.setImage(ledger.getImage());
        dto.setCreatedBy(ledger.getCreatedBy());
        dto.setCreatedAt(timestampMapper.createdAt(ledger));
        dto.setUpdatedBy(ledger.getUpdatedBy());
        dto.setUpdatedAt(timestampMapper.updatedAt(ledger, ledger.getCreatedTimeOffset()));

        return dto;
    }

    /**
     * Updates an existing ledger by its ID.
     *
     * @param id the ID of the ledger to update
     * @param ledgerDto the updated ledger data
     */
    public void updateLedgerById(String provider, Long userId, Long id, LedgerDto ledgerDto) {

        boolean isAdmin = "admin".equalsIgnoreCase(provider);
        User agencyUser = null;

        if (!isAdmin) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            agencyUser = user.getParentUser() != null ? user.getParentUser() : user;

            Ledger ledger = ledgerRepository.findByIdAndAgencyId(id, agencyUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ledger", id));

            ledger.setTitle(ledgerDto.getTitle());
            ledger.setDescription(ledgerDto.getDescription());
            ledger.setImage(ledgerDto.getImage());
            ledger.setUpdatedAt(UserDateTimeUtil.now());
            ledger.setUpdatedBy(this.getUserIdFromAuthentication());

            ledgerRepository.save(ledger);
        } else {
            Ledger ledger = ledgerRepository.findByIdAndAgencyIdIsNull(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Ledger", id));

            ledger.setTitle(ledgerDto.getTitle());
            ledger.setDescription(ledgerDto.getDescription());
            ledger.setImage(ledgerDto.getImage());
            ledger.setUpdatedAt(UserDateTimeUtil.now());
            ledger.setUpdatedBy(this.getUserIdFromAuthentication());

            ledgerRepository.save(ledger);
        }

    }
}
