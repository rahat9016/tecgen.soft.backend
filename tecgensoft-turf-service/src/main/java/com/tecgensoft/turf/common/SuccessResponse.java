package com.tecgensoft.turf.common;

/** Consistent success envelope, matching the {success, message, data} shape used across services. */
public record SuccessResponse<T>(boolean success, String message, T data) {
  public static <T> SuccessResponse<T> of(String message, T data) {
    return new SuccessResponse<>(true, message, data);
  }
}
