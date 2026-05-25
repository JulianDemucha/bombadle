package com.bombadle.service.admin.queue;

import com.bombadle.dto.request.AdminCharacterCardRequest;

public record PendingCardCreatePayload(
        AdminCharacterCardRequest card,
        String tempImagePath
) {
}

