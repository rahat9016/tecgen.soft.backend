package com.familiemunshi.common.policy;

import java.util.*;

public final class TurfFeatures {
  private TurfFeatures() {}

  public static String forPermission(String permission) {
    if (permission == null) return null;
    if (permission.startsWith("TURF_COURT_")) return "COURT_MANAGEMENT";
    if (permission.startsWith("TURF_BOOKING_") || permission.equals("TURF_BLOCK_MANAGE"))
      return "BOOKING_MANAGEMENT";
    if (permission.equals("TURF_PAYMENT_REFUND")) return "REFUND_MANAGEMENT";
    if (permission.startsWith("TURF_PAYMENT_")) return "PAYMENT_MANAGEMENT";
    if (permission.startsWith("TURF_SETTINGS_")) return "VENUE_SETTINGS";
    if (permission.startsWith("TURF_CONTENT_") || permission.equals("TURF_GALLERY_MANAGE"))
      return "WEBSITE_CONTENT";
    if (permission.equals("TURF_REPORT_READ")) return "BASIC_REPORTS";
    return null;
  }

  public static final Set<String> ALL =
      Set.of(
          "COURT_MANAGEMENT",
          "BOOKING_MANAGEMENT",
          "ONLINE_BOOKING",
          "PAYMENT_MANAGEMENT",
          "REFUND_MANAGEMENT",
          "VENUE_SETTINGS",
          "WEBSITE_CONTENT",
          "BASIC_REPORTS");
}
