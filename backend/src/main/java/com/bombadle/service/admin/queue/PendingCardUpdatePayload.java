package com.bombadle.service.admin.queue;

import com.bombadle.dto.request.AdminCharacterCardRequest;

public record PendingCardUpdatePayload(
        Long id,
        AdminCharacterCardRequest card,
        String tempImagePath,
        String previousName
) {
}

