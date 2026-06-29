package com.bombadle.dto.request;

import java.util.List;
import java.util.Set;

public record AdminCharacterCardUpdateRequest(
        String name,
        String gender,
        String race,
        Boolean alive,
        Set<String> colors,
        Set<String> affiliations,
        Integer firstAppearanceEpisode,
        Set<String> aliases,
        List<AdminQuoteRequest> quotesToAdd,
        List<Long> quoteIdsToRemove
) {
}
