package com.bombadle.dto;

import com.bombadle.enums.MatchType;
import lombok.Builder;

@Builder
public record CardField<T>(T value, MatchType match) {}
