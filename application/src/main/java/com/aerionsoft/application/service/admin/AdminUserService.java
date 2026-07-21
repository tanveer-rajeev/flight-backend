package com.aerionsoft.application.service.admin;

import com.aerionsoft.application.util.TimestampMapper;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.exception.ServiceExceptions;
import com.aerionsoft.application.enums.common.ErrorCode;
import com.aerionsoft.application.exception.BusinessException;
import com.aerionsoft.application.dto.admin.subadmin.SubAdminDto;
import com.aerionsoft.application.dto.admin.subadmin.response.SubAdminResponseDto;
import com.aerionsoft.application.dto.client.user.UpdateProfileRequest;
import com.aerionsoft.application.dto.client.user.UserDto;
import com.aerionsoft.application.entity.admin.AdminUser;
import com.aerionsoft.application.entity.rolePermission.Role;
import com.aerionsoft.application.repository.user.AdminUserRepository;
import com.aerionsoft.application.service.common.FileStorageService;
import com.aerionsoft.application.service.access.RoleService;
import com.aerionsoft.application.util.EmailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TimestampMapper timestampMapper;

    public UserDto updateProfile(Long authUserId, UpdateProfileRequest req) {
        AdminUser user = adminUserRepository.findById(authUserId).orElseThrow();

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress() != null) user.setAddress(req.getAddress());

        adminUserRepository.save(user);
        return mapToResponse(user);
    }


    public String uploadProfileImage(String email, MultipartFile file) throws IOException {
        AdminUser user = adminUserRepository.findByEmail(email).orElseThrow();
        String filename = fileStorageService.saveFile(file, email);
        user.setImage(filename);
        adminUserRepository.save(user);
        return filename;
    }

    public UserDto getProfile(String email) {
        AdminUser user = adminUserRepository.findByEmail(email).orElseThrow();
        return mapToResponse(user);
    }

    public UserDto getUserByEmail(String email) {
        AdminUser user = adminUserRepository.findByEmail(email).orElseThrow();
        return mapToResponse(user);
    }

    public UserDto getUserById(Long id) {
        if (id == null) return null;
        AdminUser user = adminUserRepository.findById(id).orElse(null);
        return mapToResponse(user);
    }

    private UserDto mapToResponse(AdminUser user) {
        if (user == null) return null;
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFullName(user.getFullName());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setImage(user.getImage());
        userDto.setAddress(user.getAddress());
        userDto.setCreatedAt(timestampMapper.toRequestUserTimeString(user.getCreatedAt(), null));
        userDto.setCurrency(user.getCurrency());
        userDto.setRole(user.getRoles().toString());

        Role roleNames = roleService.getRoleByAdminId(user.getId());
        System.out.println("Role Names: " + (roleNames != null ? roleNames.getName() : "No Role Found"));
        if (roleNames != null) {
            userDto.setRole(roleNames.getSlug());
        }

        return userDto;
    }

    // Create SubAdmin User Service
    public void createSubAdminUser(SubAdminDto subAdminDto) {
        String email = EmailUtils.normalize(subAdminDto.getEmail());

        if (adminUserRepository.findByEmail(email).isPresent())
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "User already exists!");

        AdminUser user = AdminUser.builder()
                .fullName(subAdminDto.getFullName())
                .email(email)
                .phoneNumber(subAdminDto.getPhoneNumber())
                .password(passwordEncoder.encode(subAdminDto.getPassword()))
                .address(subAdminDto.getAddress())
                .image(subAdminDto.getImage())
                .currency(subAdminDto.getCurrency().toUpperCase())
                .isActive(true)
                .isVerified(true)
                .createdAt(UserDateTimeUtil.now())
                .build();

        adminUserRepository.save(user);
    }

    // Get List of Sub Admin
    public List<SubAdminResponseDto> getSubAdminUsers(String currencyCode) {

        String currency = (currencyCode == null || currencyCode.isBlank())
                ? null
                : currencyCode.toUpperCase();

        return adminUserRepository.findActiveAdminsWithoutAdminRole(currency)
                .stream()
                .map(adminUser -> {
                    SubAdminResponseDto dto = new SubAdminResponseDto();
                    dto.setId(adminUser.getId());
                    dto.setFullName(adminUser.getFullName());
                    dto.setEmail(adminUser.getEmail());
                    dto.setPhoneNumber(adminUser.getPhoneNumber());
                    dto.setAddress(adminUser.getAddress());
                    dto.setImage(adminUser.getImage());
                    dto.setCurrency(adminUser.getCurrency());
                    Role role = roleService.getRoleByAdminId(adminUser.getId());
                    dto.setRoleName(role != null ? role.getName() : null);
                    dto.setActive(adminUser.getIsActive());
                    dto.setVerified(adminUser.getIsVerified());
                    dto.setCreatedDate(timestampMapper.toRequestUserTimeString(adminUser.getCreatedAt(), null));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Delete Sub Admin
    public void deleteSubAdminUser(Long id) {
        AdminUser adminUser = adminUserRepository.findById(id).orElseThrow(() -> ServiceExceptions.notFound("User not found!"));
        boolean isSubAdmin = !adminUserRepository.hasRole(adminUser.getId(), "admin");

        if (!isSubAdmin) {
            throw ServiceExceptions.business("User is not sub-admin");
        }

        adminUser.setIsActive(false);
        adminUser.setIsVerified(false);

        adminUserRepository.save(adminUser);
    }
}
