
package com.tecgensoft.common.policy;

import com.tecgensoft.service.enums.PermissionCode;
import com.tecgensoft.service.enums.ProductCode;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
                    PermissionCode.PRODUCT_CREATE, PermissionCode.PRODUCT_READ,
                    PermissionCode.PRODUCT_UPDATE, PermissionCode.PRODUCT_DELETE,

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
                    PermissionCode.SUBSCRIPTION_UPDATE,
                    PermissionCode.PRODUCT_VIEW, PermissionCode.PRODUCT_ACTIVATE, PermissionCode.PRODUCT_DEACTIVATE
                    ,PermissionCode.PRODUCT_VIEW, PermissionCode.PRODUCT_ACTIVATE, PermissionCode.PRODUCT_DEACTIVATE,
                    PermissionCode.PACKAGE_VIEW, PermissionCode.PACKAGE_ACTIVATE, PermissionCode.PACKAGE_DEACTIVATE,
                    PermissionCode.FEATURE_VIEW, PermissionCode.FEATURE_CREATE, PermissionCode.FEATURE_UPDATE,
                    PermissionCode.FEATURE_ACTIVATE, PermissionCode.FEATURE_DEACTIVATE,
                    PermissionCode.SUBSCRIPTION_CREATE, PermissionCode.SUBSCRIPTION_ACTIVATE,
                    PermissionCode.SUBSCRIPTION_SUSPEND, PermissionCode.SUBSCRIPTION_CANCEL,
                    PermissionCode.SUBSCRIPTION_RENEW, PermissionCode.SUBSCRIPTION_EXTEND_TRIAL,
                    PermissionCode.SUBSCRIPTION_PAYMENT_VIEW, PermissionCode.SUBSCRIPTION_PAYMENT_CREATE,
                    PermissionCode.SUBSCRIPTION_PAYMENT_REFUND
                    ,PermissionCode.SUBSCRIPTION_VIEW
            );

    // ========================================
    // BASIC ORGANIZATION PERMISSIONS
    // Granted to every organization at creation,
    // independent of any subscription.
    // ========================================

    private static final Set<PermissionCode> BASIC =
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
                    PermissionCode.ROLE_PERMISSION_MANAGE
            );

    // ========================================
    // TURF PRODUCT PERMISSIONS
    // Granted only when a TURF subscription is assigned.
    // ========================================

    private static final Set<PermissionCode> TURF =
            EnumSet.of(
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
                    PermissionCode.ORDER_CANCEL,

                    // TURF OPERATIONS
                    PermissionCode.TURF_COURT_VIEW,
                    PermissionCode.TURF_COURT_CREATE,
                    PermissionCode.TURF_COURT_UPDATE,
                    PermissionCode.TURF_COURT_DELETE,
                    PermissionCode.TURF_COURT_MANAGE,
                    PermissionCode.TURF_BOOKING_VIEW,
                    PermissionCode.TURF_BOOKING_CREATE,
                    PermissionCode.TURF_BOOKING_UPDATE,
                    PermissionCode.TURF_BOOKING_CANCEL,
                    PermissionCode.TURF_BOOKING_CONFIRM,
                    PermissionCode.TURF_BOOKING_MANAGE,
                    PermissionCode.TURF_PAYMENT_VIEW,
                    PermissionCode.TURF_PAYMENT_CREATE,
                    PermissionCode.TURF_PAYMENT_UPDATE,
                    PermissionCode.TURF_PAYMENT_REFUND,
                    PermissionCode.TURF_PAYMENT_MANAGE,
                    PermissionCode.TURF_REPORT_VIEW,
                    PermissionCode.TURF_ADVANCED_REPORT_VIEW,
                    PermissionCode.TURF_SETTINGS_VIEW,
                    PermissionCode.TURF_SETTINGS_UPDATE,
                    PermissionCode.TURF_SCHEDULE_MANAGE,
                    PermissionCode.TURF_PRICE_MANAGE
            );

    // ========================================
    // PRODUCT -> PERMISSION GROUP MAPPING
    // ========================================

    private static final Map<ProductCode, Set<PermissionCode>> PRODUCT_PERMISSIONS =
            Map.of(ProductCode.TURF, TURF);

    // ========================================
    // ALL ORGANIZATION-SCOPED PERMISSIONS
    // Union of BASIC + every product group. Used for
    // membership checks (is this an organization permission?).
    // ========================================

    private static final Set<PermissionCode> ORGANIZATION =
            buildOrganizationUnion();

    private static Set<PermissionCode> buildOrganizationUnion() {
        EnumSet<PermissionCode> union = EnumSet.copyOf(BASIC);
        PRODUCT_PERMISSIONS.values().forEach(union::addAll);
        return union;
    }

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
    // GET BASIC ORGANIZATION PERMISSION CODES
    // Granted to every organization at creation.
    // ========================================

    public static List<String> getOrganizationPermissions() {
        return BASIC.stream()
                .map(PermissionCode::getCode)
                .sorted()
                .toList();
    }

    // ========================================
    // GET PRODUCT PERMISSION CODES
    // Granted when the product's subscription is assigned.
    // ========================================

    public static List<String> getProductPermissions(ProductCode product) {
        return PRODUCT_PERMISSIONS.getOrDefault(product, Set.of())
                .stream()
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
