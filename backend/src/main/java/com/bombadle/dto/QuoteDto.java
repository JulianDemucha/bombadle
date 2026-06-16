package com.bombadle.dto;

import lombok.Builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record QuoteDto(
        Long id,
        String quoteBeginning,
        List<String> options
) {
    @Builder
    public QuoteDto {
        List<String> shuffledOptions = new ArrayList<>(options); // in case of List.of() etc
        Collections.shuffle(shuffledOptions);
        options = Collections.unmodifiableList(shuffledOptions);
    }
}