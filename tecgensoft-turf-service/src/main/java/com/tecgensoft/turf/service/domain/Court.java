package com.tecgensoft.turf.service.domain;

import java.math.BigDecimal;
import java.util.*;

public record Court(
    UUID id,
    UUID organizationId,

    String name,
    String sport,
    String surface,
    boolean indoor,
    String size,
    String imageUrl,
    BigDecimal regularPrice,
    BigDecimal peakPrice,
    int players,
    Set<String> features,
    String description,
    boolean active) {}
