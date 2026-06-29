package com.bombadle.dto.queue;

import com.bombadle.dto.request.AdminCharacterCardRequest;

public record PendingCardUpdatePayload(
        Long id,
        AdminCharacterCardRequest card,
        String tempImagePath
) {
}
