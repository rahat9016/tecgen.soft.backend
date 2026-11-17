
package com.tecgensoft.common.policy;

import com.tecgensoft.service.enums.PermissionCode;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class PermissionPolicy {

    private PermissionPolicy() {
        throw new IllegalStateException(
                "Utility class cannot be instantiated"
        );
    }

    // ========================================
    // PLATFORM SUPER ADMIN PERMISSIONS
    // ========================================

    private static final Set<PermissionCode> PLATFORM =
            EnumSet.of(
                    PermissionCode.ORGANIZATION_CREATE,
                    PermissionCode.ORGANIZATION_READ,
                    PermissionCode.ORGANIZATION_LIST,
                    PermissionCode.ORGANIZATION_UPDATE,
                    PermissionCode.ORGANIZATION_DELETE,

                    PermissionCode.ROLE_CREATE,
                    PermissionCode.ROLE_READ,
                    PermissionCode.ROLE_UPDATE,
                    PermissionCode.ROLE_DELETE,

                    PermissionCode.PERMISSION_READ,
                    PermissionCode.ROLE_PERMISSION_MANAGE,

                    PermissionCode.USER_CREATE,
                    PermissionCode.USER_READ,
                    PermissionCode.USER_UPDATE,
                    PermissionCode.USER_DELETE,
                    PermissionCode.USER_ROLE_MANAGE,

                    PermissionCode.USER_ROLE_ASSIGN,
                    PermissionCode.USER_ROLE_REMOVE,

                    PermissionCode.PACKAGE_CREATE,
                    PermissionCode.PACKAGE_READ,
                    PermissionCode.PACKAGE_UPDATE,
                    PermissionCode.SUBSCRIPTION_ASSIGN,
                    PermissionCode.SUBSCRIPTION_READ,
                    PermissionCode.SUBSCRIPTION_UPDATE
            );

    // ========================================
    // DEFAULT ORGANIZATION ADMIN PERMISSIONS
    // ========================================

    private static final Set<PermissionCode> ORGANIZATION =
            EnumSet.of(
                    // ORGANIZATION MANAGEMENT
                    PermissionCode.ORGANIZATION_READ,
                    PermissionCode.ORGANIZATION_UPDATE,

                    // USER MANAGEMENT
                    PermissionCode.USER_CREATE,
                    PermissionCode.USER_READ,
                    PermissionCode.USER_UPDATE,
                    PermissionCode.USER_DELETE,
                    PermissionCode.USER_ROLE_MANAGE,
                    PermissionCode.USER_ROLE_ASSIGN,
                    PermissionCode.USER_ROLE_REMOVE,

                    // ROLE MANAGEMENT
                    PermissionCode.ROLE_CREATE,
                    PermissionCode.ROLE_READ,
                    PermissionCode.ROLE_UPDATE,
                    PermissionCode.ROLE_DELETE,

                    // PERMISSION MANAGEMENT
                    PermissionCode.PERMISSION_READ,
                    PermissionCode.ROLE_PERMISSION_MANAGE,

                    // PRODUCT MANAGEMENT
                    PermissionCode.PRODUCT_CREATE,
                    PermissionCode.PRODUCT_READ,
                    PermissionCode.PRODUCT_UPDATE,
                    PermissionCode.PRODUCT_DELETE,

                    // SALES MANAGEMENT
                    PermissionCode.SALE_CREATE,
                    PermissionCode.SALE_READ,
                    PermissionCode.SALE_UPDATE,
                    PermissionCode.SALE_DELETE,
                    PermissionCode.SALE_REFUND,

                    // ORDER MANAGEMENT
                    PermissionCode.ORDER_CREATE,
                    PermissionCode.ORDER_READ,
                    PermissionCode.ORDER_UPDATE,
                    PermissionCode.ORDER_CANCEL
            );

    // ========================================
    // GET PLATFORM PERMISSION CODES
    // ========================================

    public static List<String> getPlatformPermissions() {
        return PLATFORM.stream()
                .map(PermissionCode::getCode)
                .sorted()
                .toList();
    }

    // ========================================
    // GET ORGANIZATION PERMISSION CODES
    // ========================================

    public static List<String> getOrganizationPermissions() {
        return ORGANIZATION.stream()
                .map(PermissionCode::getCode)
                .sorted()
                .toList();
    }

    // ========================================
    // CHECK PLATFORM PERMISSION
    // ========================================

    public static boolean isPlatformPermission(String code) {
        return PermissionCode.isSuppoerted(code)
                && PLATFORM.contains(
                PermissionCode.fromCode(code)
        );
    }

    // ========================================
    // CHECK ORGANIZATION PERMISSION
    // ========================================

    public static boolean isOrganizationPermission(String code) {
        return PermissionCode.isSuppoerted(code)
                && ORGANIZATION.contains(
                PermissionCode.fromCode(code)
        );
    }
}
