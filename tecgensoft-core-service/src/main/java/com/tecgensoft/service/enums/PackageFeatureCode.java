package com.tecgensoft.service.enums;

import java.util.Arrays;

public enum PackageFeatureCode {
    COURT_MANAGEMENT(ProductCode.TURF),
    BOOKING_MANAGEMENT(ProductCode.TURF),
    ONLINE_BOOKING(ProductCode.TURF),
    PAYMENT_MANAGEMENT(ProductCode.TURF),
    REFUND_MANAGEMENT(ProductCode.TURF),
    VENUE_SETTINGS(ProductCode.TURF),
    WEBSITE_CONTENT(ProductCode.TURF),
    BASIC_REPORTS(ProductCode.TURF),
    ADMIN_BOOKING(ProductCode.TURF), PAYMENT_TRACKING(ProductCode.TURF),
    ADVANCE_PAYMENT(ProductCode.TURF), MULTIPLE_COURTS(ProductCode.TURF),
    REPORTS(ProductCode.TURF), ADVANCED_REPORTS(ProductCode.TURF),
    PAYMENT_INTEGRATION(ProductCode.TURF), CUSTOM_DOMAIN(ProductCode.TURF),

    HOTEL_MANAGEMENT(ProductCode.HOTEL),
    ROOM_MANAGEMENT(ProductCode.HOTEL),
    ROOM_AVAILABILITY(ProductCode.HOTEL),
    RESERVATION_MANAGEMENT(ProductCode.HOTEL),
    GUEST_MANAGEMENT(ProductCode.HOTEL),
    CHECK_IN_CHECK_OUT(ProductCode.HOTEL),
    HOTEL_PAYMENT_MANAGEMENT(ProductCode.HOTEL),
    HOTEL_SETTINGS(ProductCode.HOTEL);

    private final ProductCode product;

    PackageFeatureCode(ProductCode product) {
        this.product = product;
    }

    public static boolean supports(ProductCode product, String code) {
        return product != null && code != null && Arrays.stream(values())
                .anyMatch(feature -> feature.product == product && feature.name().equals(code));
    }
}
