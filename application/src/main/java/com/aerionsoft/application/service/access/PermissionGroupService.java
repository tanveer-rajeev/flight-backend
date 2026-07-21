package com.aerionsoft.application.service.access;

import com.aerionsoft.application.dto.rolepermission.PermissionGroupDto;
import com.aerionsoft.application.dto.rolepermission.response.PermissionGroupResponseDto;
import com.aerionsoft.application.entity.rolePermission.PermissionGroup;
import com.aerionsoft.application.repository.access.PermissionGroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionGroupService {
    @Autowired
    private PermissionGroupRepository permissionGroupRepository;

    public List<PermissionGroupResponseDto> getPermissionGroups() {
        return permissionGroupRepository.findAll()
                .stream()
                .map(pGroup -> {
                    PermissionGroupResponseDto permissionGroupResponseDto = new PermissionGroupResponseDto();
                    permissionGroupResponseDto.setId(pGroup.getId());
                    permissionGroupResponseDto.setName(pGroup.getName());

                    return permissionGroupResponseDto;
                }).collect(Collectors.toList());
    }

    /**
     * Create permission group service
     * @param permissionGroupDto request data to permission group
     */
    public void createPermissionGroup(PermissionGroupDto permissionGroupDto) {
        PermissionGroup permissionGroup = PermissionGroup
                .builder()
                .name(permissionGroupDto.getName().toUpperCase())
                .build();

        permissionGroupRepository.save(permissionGroup);
    }

    public void updatePermissionGroup(Long id, PermissionGroupDto permissionGroupDto) {
        PermissionGroup permissionGroup = permissionGroupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission group not found with id: " + id));

        permissionGroup.setName(permissionGroupDto.getName().toUpperCase());
        permissionGroupRepository.save(permissionGroup);
    }
}
