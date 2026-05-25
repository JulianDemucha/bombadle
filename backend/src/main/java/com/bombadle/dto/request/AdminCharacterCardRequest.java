package com.bombadle.dto.request;

import java.util.Set;

public record AdminCharacterCardRequest(
        String name,
        String gender,
        String race,
        Boolean alive,
        Set<String> colors,
        Set<String> affiliations,
        Integer firstAppearanceEpisode,
        Set<String> aliases
) {
}

