package com.tecgensoft.turf;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingTest {
  @Test
  void mixedPeakPricing() {
    assertEquals(
        new BigDecimal("500.00"),
        com.tecgensoft.turf.service.TurfService.price(
            new BigDecimal("100"), new BigDecimal("200"), 17, 3, 18));
  }
}
