package com.bombadle.service.stats;

import com.bombadle.entity.ActivitySnapshot;
import com.bombadle.repository.ActivitySnapshotRepository;
import com.bombadle.repository.AnonymousSessionRepository;
import com.bombadle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityTrackingService {

    private final PlayerRepository playerRepository;
    private final AnonymousSessionRepository anonymousSessionRepository;
    private final ActivitySnapshotRepository snapshotRepository;

    private Set<Long> activePlayersBuffer = ConcurrentHashMap.newKeySet();
    private Set<UUID> activeAnonymousBuffer = ConcurrentHashMap.newKeySet();

    public void markPlayerActive(Long playerId) {
        activePlayersBuffer.add(playerId);
    }

    public void markAnonymousActive(UUID sessionId) {
        activeAnonymousBuffer.add(sessionId);
    }

    @Scheduled(fixedRate = 600000) // 10 min
    @Transactional
    public void flushActivityToDatabase() {
        Set<Long> playersToUpdate = activePlayersBuffer;
        activePlayersBuffer = ConcurrentHashMap.newKeySet();

        Set<UUID> anonymousToUpdate = activeAnonymousBuffer;
        activeAnonymousBuffer = ConcurrentHashMap.newKeySet();

        Instant now = Instant.now();

        if (!playersToUpdate.isEmpty()) {
            playerRepository.updateLastActiveAtBulk(playersToUpdate, now);
            log.debug("Flushed {} active players to DB", playersToUpdate.size());
        }

        if (!anonymousToUpdate.isEmpty()) {
            anonymousSessionRepository.updateLastActiveAtBulk(anonymousToUpdate, now);
            log.debug("Flushed {} active anonymous sessions to DB", anonymousToUpdate.size());
        }
    }

    @Scheduled(fixedRate = 3600000) // 60min
    @Transactional
    public void createActivitySnapshot() {
        Instant threshold = Instant.now().minus(1, ChronoUnit.HOURS);

        int loggedInCount = playerRepository.countByLastActiveAtAfter(threshold);
        int anonymousCount = anonymousSessionRepository.countByLastActiveAtAfter(threshold);

        ActivitySnapshot snapshot = ActivitySnapshot.builder()
                .timestamp(Instant.now())
                .loggedInActiveCount(loggedInCount)
                .anonymousActiveCount(anonymousCount)
                .build();

        snapshotRepository.save(snapshot);
        log.info("Created activity snapshot: {} logged in, {} anonymous", loggedInCount, anonymousCount);
    }
}