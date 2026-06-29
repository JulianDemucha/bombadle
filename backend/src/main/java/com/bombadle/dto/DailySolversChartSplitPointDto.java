package com.bombadle.dto;

import java.time.LocalDate;

public record DailySolversChartSplitPointDto(LocalDate x, double loggedIn, double anonymous) {
}
