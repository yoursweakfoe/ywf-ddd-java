package com.yoursweakfoe.common.ddd.fixtures.model;

import com.yoursweakfoe.common.ddd.domain.model.ValueObject;
import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements ValueObject {}
