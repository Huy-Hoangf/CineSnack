package com.cinesnack.catalog.dto;

import java.math.BigDecimal;

public record AdminSnack(
    long id,
    String kind,
    String name,
    BigDecimal price,
    String status,
    String productType
) {}
