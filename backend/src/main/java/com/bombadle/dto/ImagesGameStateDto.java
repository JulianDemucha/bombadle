package com.bombadle.dto;

import javax.naming.Name;
import java.util.List;

public record ImagesGameStateDto(
        List<Name> guesses,
        String currentImageUrl,
        boolean isGuessed
) {}