package com.bombadle.dto;

import java.util.List;

public record AdminChartResponse<T>(List<T> points) {
}
