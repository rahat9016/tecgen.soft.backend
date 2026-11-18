package com.tecgensoft.service.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
public enum PermissionCode {
    // ROLE MANAGEMENT
    ROLE_CREATE("ROLE_CREATE", "ROLE", "Create roles"),
    ROLE_READ("ROLE_READ", "ROLE", "View roles"),
    ROLE_UPDATE("ROLE_UPDATE", "ROLE", "Update roles"),
    ROLE_DELETE("ROLE_DELETE", "ROLE", "Delete roles"),


    // USER ROLE MANAGEMENT
    USER_ROLE_ASSIGN("USER_ROLE_ASSIGN", "USER", "Assign roles to users"),
    USER_ROLE_REMOVE("USER_ROLE_REMOVE", "USER", "Remove roles from users"),

    // PERMISSION MANAGEMENT
    PERMISSION_READ("PERMISSION_READ", "PERMISSION", "View permissions"),
    ROLE_PERMISSION_MANAGE("ROLE_PERMISSION_MANAGE", "PERMISSION", "Manage role permissions"),

    // USER MANAGEMENT
    USER_CREATE("USER_CREATE", "USER", "Create users"),
    USER_READ("USER_READ", "USER", "View users"),
    USER_UPDATE("USER_UPDATE", "USER", "Update users"),
    USER_DELETE("USER_DELETE", "USER", "Delete users"),
    USER_ROLE_MANAGE("USER_ROLE_MANAGE", "USER", "Manage user role assignments"),

    // ORGANIZATION MANAGEMENT
    ORGANIZATION_CREATE("ORGANIZATION_CREATE", "ORGANIZATION", "Create organizations"),
    ORGANIZATION_READ("ORGANIZATION_READ", "ORGANIZATION", "View organizations"),
    ORGANIZATION_UPDATE("ORGANIZATION_UPDATE", "ORGANIZATION", "Update organizations"),
    ORGANIZATION_DELETE("ORGANIZATION_DELETE", "ORGANIZATION", "Delete organizations"),
    ORGANIZATION_LIST("ORGANIZATION_LIST", "ORGANIZATION", "List organizations"),

    // PRODUCT MANAGEMENT
    PRODUCT_CREATE("PRODUCT_CREATE", "PRODUCT", "Create products"),
    PRODUCT_READ("PRODUCT_READ", "PRODUCT", "View products"),
    PRODUCT_UPDATE("PRODUCT_UPDATE", "PRODUCT", "Update products"),
    PRODUCT_DELETE("PRODUCT_DELETE", "PRODUCT", "Delete products"),

    // PACKAGE AND SUBSCRIPTION MANAGEMENT (PLATFORM)
    PACKAGE_CREATE("PACKAGE_CREATE", "PACKAGE", "Create software packages"),
    PACKAGE_READ("PACKAGE_READ", "PACKAGE", "View software packages"),
    PACKAGE_UPDATE("PACKAGE_UPDATE", "PACKAGE", "Update software packages"),
    SUBSCRIPTION_ASSIGN("SUBSCRIPTION_ASSIGN", "SUBSCRIPTION", "Assign organization subscriptions"),
    SUBSCRIPTION_READ("SUBSCRIPTION_READ", "SUBSCRIPTION", "View organization subscriptions"),
    SUBSCRIPTION_UPDATE("SUBSCRIPTION_UPDATE", "SUBSCRIPTION", "Update organization subscriptions"),

    PRODUCT_VIEW("PRODUCT_VIEW", "PRODUCT", "View products"),
    PRODUCT_ACTIVATE("PRODUCT_ACTIVATE", "PRODUCT", "Activate products"),
    PRODUCT_DEACTIVATE("PRODUCT_DEACTIVATE", "PRODUCT", "Deactivate products"),
    PACKAGE_VIEW("PACKAGE_VIEW", "PACKAGE", "View packages"),
    PACKAGE_ACTIVATE("PACKAGE_ACTIVATE", "PACKAGE", "Activate packages"),
    PACKAGE_DEACTIVATE("PACKAGE_DEACTIVATE", "PACKAGE", "Deactivate packages"),
    FEATURE_VIEW("FEATURE_VIEW", "FEATURE", "View product features"),
    FEATURE_CREATE("FEATURE_CREATE", "FEATURE", "Create product features"),
    FEATURE_UPDATE("FEATURE_UPDATE", "FEATURE", "Update product features"),
    FEATURE_ACTIVATE("FEATURE_ACTIVATE", "FEATURE", "Activate product features"),
    FEATURE_DEACTIVATE("FEATURE_DEACTIVATE", "FEATURE", "Deactivate product features"),
    SUBSCRIPTION_CREATE("SUBSCRIPTION_CREATE", "SUBSCRIPTION", "Create subscriptions"),
    SUBSCRIPTION_ACTIVATE("SUBSCRIPTION_ACTIVATE", "SUBSCRIPTION", "Activate subscriptions"),
    SUBSCRIPTION_SUSPEND("SUBSCRIPTION_SUSPEND", "SUBSCRIPTION", "Suspend subscriptions"),
    SUBSCRIPTION_CANCEL("SUBSCRIPTION_CANCEL", "SUBSCRIPTION", "Cancel subscriptions"),
    SUBSCRIPTION_RENEW("SUBSCRIPTION_RENEW", "SUBSCRIPTION", "Renew subscriptions"),
    SUBSCRIPTION_EXTEND_TRIAL("SUBSCRIPTION_EXTEND_TRIAL", "SUBSCRIPTION", "Extend free trials"),
    SUBSCRIPTION_PAYMENT_VIEW("SUBSCRIPTION_PAYMENT_VIEW", "SUBSCRIPTION_PAYMENT", "View subscription payments"),
    SUBSCRIPTION_PAYMENT_CREATE("SUBSCRIPTION_PAYMENT_CREATE", "SUBSCRIPTION_PAYMENT", "Create subscription payments"),
    SUBSCRIPTION_PAYMENT_REFUND("SUBSCRIPTION_PAYMENT_REFUND", "SUBSCRIPTION_PAYMENT", "Refund subscription payments"),
    SUBSCRIPTION_VIEW("SUBSCRIPTION_VIEW", "SUBSCRIPTION", "View subscriptions"),
    TURF_COURT_VIEW("TURF_COURT_VIEW", "TURF_COURT", "View turf courts"),
    TURF_COURT_CREATE("TURF_COURT_CREATE", "TURF_COURT", "Create turf courts"),
    TURF_COURT_UPDATE("TURF_COURT_UPDATE", "TURF_COURT", "Update turf courts"),
    TURF_COURT_DELETE("TURF_COURT_DELETE", "TURF_COURT", "Delete turf courts"),
    TURF_COURT_MANAGE("TURF_COURT_MANAGE", "TURF_COURT", "Manage turf courts"),
    TURF_BOOKING_VIEW("TURF_BOOKING_VIEW", "TURF_BOOKING", "View turf bookings"),
    TURF_BOOKING_CREATE("TURF_BOOKING_CREATE", "TURF_BOOKING", "Create turf bookings"),
    TURF_BOOKING_UPDATE("TURF_BOOKING_UPDATE", "TURF_BOOKING", "Update turf bookings"),
    TURF_BOOKING_CANCEL("TURF_BOOKING_CANCEL", "TURF_BOOKING", "Cancel turf bookings"),
    TURF_BOOKING_CONFIRM("TURF_BOOKING_CONFIRM", "TURF_BOOKING", "Confirm turf bookings"),
    TURF_BOOKING_MANAGE("TURF_BOOKING_MANAGE", "TURF_BOOKING", "Manage turf bookings"),
    TURF_PAYMENT_VIEW("TURF_PAYMENT_VIEW", "TURF_PAYMENT", "View turf payments"),
    TURF_PAYMENT_CREATE("TURF_PAYMENT_CREATE", "TURF_PAYMENT", "Create turf payments"),
    TURF_PAYMENT_UPDATE("TURF_PAYMENT_UPDATE", "TURF_PAYMENT", "Update turf payments"),
    TURF_PAYMENT_REFUND("TURF_PAYMENT_REFUND", "TURF_PAYMENT", "Refund turf payments"),
    TURF_PAYMENT_MANAGE("TURF_PAYMENT_MANAGE", "TURF_PAYMENT", "Manage turf payments"),
    TURF_REPORT_VIEW("TURF_REPORT_VIEW", "TURF_REPORT", "View turf reports"),
    TURF_ADVANCED_REPORT_VIEW("TURF_ADVANCED_REPORT_VIEW", "TURF_REPORT", "View advanced turf reports"),
    TURF_SETTINGS_VIEW("TURF_SETTINGS_VIEW", "TURF_SETTINGS", "View turf settings"),
    TURF_SETTINGS_UPDATE("TURF_SETTINGS_UPDATE", "TURF_SETTINGS", "Update turf settings"),
    TURF_SCHEDULE_MANAGE("TURF_SCHEDULE_MANAGE", "TURF_SETTINGS", "Manage turf schedules"),
    TURF_PRICE_MANAGE("TURF_PRICE_MANAGE", "TURF_SETTINGS", "Manage turf prices"),

    // SALES MANAGEMENT
    SALE_CREATE("SALE_CREATE", "SALE", "Create sales"),
    SALE_READ("SALE_READ", "SALE", "View sales"),
    SALE_UPDATE("SALE_UPDATE", "SALE", "Update sales"),
    SALE_DELETE("SALE_DELETE", "SALE", "Delete sales"),
    SALE_REFUND("SALE_REFUND", "SALE", "Refund sales"),

    // ORDER MANAGEMENT
    ORDER_CREATE("ORDER_CREATE", "ORDER", "Create orders"),
    ORDER_READ("ORDER_READ", "ORDER", "View orders"),
    ORDER_UPDATE("ORDER_UPDATE", "ORDER", "Update orders"),
    ORDER_CANCEL("ORDER_CANCEL", "ORDER", "Cancel orders");

    private final String code;
    private final String module;
    private final String description;

    PermissionCode(String code, String module, String description) {
        this.code = code;
        this.module = module;
        this.description = description;
    }

    /**
     * Returns the PermissionCode corresponding to the given code.
     *
     * @param code the permission code
     * @return the corresponding PermissionCode
     * @throws IllegalArgumentException if the code is invalid
     */
    public static PermissionCode fromCode(String code) {
        Objects.requireNonNull(code, "Permission code cannot be null");
        return Arrays.stream(PermissionCode.values()).filter(permission -> permission.getCode().equalsIgnoreCase(code)).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid permission code: " + code));
    }

    /**
     * Returns a list of all supported permission codes.
     *
     * @return a list of supported permission codes
     */
    public static List<String> getSupportedPermissions() {
        return Stream.of(PermissionCode.values()).map(PermissionCode::getCode).toList();
    }

    /**
     * Checks if a given permission code is supported.
     *
     * @param code the permission code to check
     * @return true if the code is supported, false otherwise
     */
    public static boolean isSuppoerted(String code) {
        return code != null && Stream.of(PermissionCode.values()).anyMatch(permission -> permission.getCode().equalsIgnoreCase(code));
    }


    public static List<String> getDefaultOrganizationPermissions() {

        return List.of(
                // ORGANIZATION MANAGEMENT
                ORGANIZATION_READ.getCode(), ORGANIZATION_UPDATE.getCode(),

                // USER MANAGEMENT
                USER_CREATE.getCode(), USER_READ.getCode(), USER_UPDATE.getCode(), USER_ROLE_MANAGE.getCode(),

                // ROLE MANAGEMENT
                ROLE_CREATE.getCode(), ROLE_READ.getCode(), ROLE_UPDATE.getCode(), ROLE_PERMISSION_MANAGE.getCode(), PERMISSION_READ.getCode(),

                // PRODUCT MANAGEMENT
                PRODUCT_CREATE.getCode(), PRODUCT_READ.getCode(), PRODUCT_UPDATE.getCode(), PRODUCT_DELETE.getCode(),

                // SALES MANAGEMENT
                SALE_CREATE.getCode(), SALE_READ.getCode(), SALE_UPDATE.getCode(),

                // ORDER MANAGEMENT
                ORDER_CREATE.getCode(), ORDER_READ.getCode(), ORDER_UPDATE.getCode(), ORDER_CANCEL.getCode());
    }

    public static List<String> getPlatformPermissions() {
        return List.of(ORGANIZATION_CREATE.getCode(), ORGANIZATION_READ.getCode(), ORGANIZATION_UPDATE.getCode(), ORGANIZATION_DELETE.getCode(), ORGANIZATION_LIST.getCode(),

                ROLE_CREATE.getCode(), ROLE_READ.getCode(), ROLE_UPDATE.getCode(), ROLE_DELETE.getCode(),

                PERMISSION_READ.getCode(), ROLE_PERMISSION_MANAGE.getCode(),

                USER_CREATE.getCode(), USER_READ.getCode(), USER_UPDATE.getCode(), USER_DELETE.getCode(), USER_ROLE_MANAGE.getCode(),

                PACKAGE_CREATE.getCode(), PACKAGE_READ.getCode(), PACKAGE_UPDATE.getCode(),
                SUBSCRIPTION_ASSIGN.getCode(), SUBSCRIPTION_READ.getCode(), SUBSCRIPTION_UPDATE.getCode());
    }
}
