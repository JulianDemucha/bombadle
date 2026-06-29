package com.bombadle.dto;

import java.time.Instant;

public record ActivityChartSplitPointDto(Instant x, double loggedIn, double anonymous) {
}
