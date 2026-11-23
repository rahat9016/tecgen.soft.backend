package com.tecgensoft.common.utils;

public class Constants {
    public static final String APP_NAME = "Tecgensoft API";

    // API VERSIONING
    public static final String API = "/api";
    public static final String API_VERSION = "/v1";
    public static final String PUBLIC = "/public";
    public static final String API_BASE = API + API_VERSION;

    // AUTHENTICATION
    public static final String AUTH = "/auth";
    public static final String LOGIN = "/login";
    public static final String SIGNUP = "/signup";
    public static final String REFRESH_TOKEN = "/refresh-token";
    public static final String CHANGE_PASSWORD = "/change-password";

    // USERS
    public static final String USERS = "/users";

    // SYSTEM ROLES
    public static final String SYSTEM = "/system";
    // ROLES
    public static final String ROLES = "/roles";

    public static final String ID = "/{id}";
    public static final String ORGANIZATION_ID = "/{organizationId}";
    public static final String USER_ID = "/{userId}";
    public static final String ROLE_ID = "/{roleId}";

    // PERMISSIONS
    public static final String PERMISSIONS = "/permissions";

    // ORGANIZATIONS
    public static final String ORGANIZATIONS = "/organizations";

    // SUBSCRIPTIONS & BILLING
    public static final String SUBSCRIPTIONS = "/subscriptions";
    public static final String SUBSCRIPTION_ID = "/{subscriptionId}";
    public static final String PAYMENTS = "/payments";
    public static final String PAYMENT_ID = "/{paymentId}";
    public static final String SETTLE = "/settle";
    public static final String STATUS = "/status";
    public static final String RENEWAL = "/renewal";
    public static final String TRIAL_EXTENSION = "/trial-extension";
    public static final String REFUND = "/refund";

    // Swagger & Actuator
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String API_DOCS = "/v3/api-docs/**";
}
