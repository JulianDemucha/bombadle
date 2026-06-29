package com.bombadle.dto.queue;

import com.bombadle.dto.request.AdminCharacterCardUpdateRequest;

public record PendingCardUpdatePayload(
        Long id,
        AdminCharacterCardUpdateRequest card,
        String tempImagePath,
        String tempGuessImagePath
) {
}
