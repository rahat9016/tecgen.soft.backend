package com.tecgensoft.service;

import com.tecgensoft.common.policy.PermissionPolicy;
import com.tecgensoft.jpa.daos.PermissionDao;
import com.tecgensoft.jpa.daos.RoleDao;
import com.tecgensoft.jpa.repositories.PermissionRepository;
import com.tecgensoft.jpa.repositories.RoleRepository;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.RoleScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Grants product-specific permissions to an organization's admin role.
 *
 * <p>Every organization receives the BASIC permission set at creation. When a
 * subscription for a product is assigned, the matching product permission group
 * is merged into the ORGANIZATION_ADMIN role. The merge is idempotent: assigning
 * the same product again adds nothing new.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationPermissionGranter {

    private static final String ORGANIZATION_ADMIN_ROLE = "ORGANIZATION_ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void grantProductPermissions(UUID organizationId, ProductCode product) {
        List<String> permissionCodes = PermissionPolicy.getProductPermissions(product);
        if (permissionCodes.isEmpty()) {
            return;
        }

        RoleDao adminRole = roleRepository
                .findByNameAndOrganization_IdAndScope(
                        ORGANIZATION_ADMIN_ROLE, organizationId, RoleScope.ORGANIZATION)
                .orElseThrow(() -> new IllegalStateException(
                        "ORGANIZATION_ADMIN role not found for organization " + organizationId));

        List<PermissionDao> permissions = permissionRepository.findAllByNameIn(permissionCodes);
        if (permissions.size() != permissionCodes.size()) {
            throw new IllegalStateException(
                    product + " permissions are not fully seeded");
        }

        boolean added = adminRole.getPermissions().addAll(permissions);
        if (added) {
            roleRepository.saveAndFlush(adminRole);
            log.info("Granted {} permissions to organization {}", product, organizationId);
        }
    }
}
