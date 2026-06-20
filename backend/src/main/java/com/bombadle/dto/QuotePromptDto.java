package com.bombadle.dto;

import com.bombadle.enums.QuoteTarget;
import lombok.Builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record QuotePromptDto(
        Long id,
        String quoteBeginning,
        List<String> options,
        Integer appearanceEpisode,
        QuoteTarget quoteTarget
) {
    @Builder
    public QuotePromptDto {
        List<String> shuffledOptions = new ArrayList<>(options); // in case of List.of() etc
        Collections.shuffle(shuffledOptions);
        options = Collections.unmodifiableList(shuffledOptions);
    }
}