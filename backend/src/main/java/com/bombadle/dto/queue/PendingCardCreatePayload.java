package com.bombadle.dto.queue;

import com.bombadle.dto.request.AdminCharacterCardRequest;

public record PendingCardCreatePayload(
        AdminCharacterCardRequest card,
        String tempImagePath
) {
}

