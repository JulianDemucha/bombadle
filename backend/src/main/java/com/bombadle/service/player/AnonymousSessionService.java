package com.bombadle.service.player;

import com.bombadle.dto.AnonymousSessionDto;
import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.repository.AnonymousSessionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AnonymousSessionService {
    private final AnonymousSessionRepository repo;

    public AnonymousSessionDto getAnonymousSession(UUID anonymousSessionId) {
        AnonymousSession anonymousSession;

        if (anonymousSessionId == null) {
            anonymousSession = new AnonymousSession(new AnonymousGuessList());
        } else {
            Optional<AnonymousSession> anonymousSessionOpt = repo.findById(anonymousSessionId);
            anonymousSession = anonymousSessionOpt.orElseGet(() -> new AnonymousSession(new AnonymousGuessList()));
        }

        return AnonymousSessionDto.toDto(
                save(anonymousSession)
        );
    }

    public GuessListDto getGuessList(UUID anonymousSessionId) {
        if (anonymousSessionId == null) {
            return new GuessListDto(Collections.emptyList());
        }
        return repo.findById(anonymousSessionId)
                .map(session -> GuessListDto.toDto(session.getGuessList()))
                .orElse(new GuessListDto(Collections.emptyList()));

    }

    public AnonymousSession save(AnonymousSession anonymousSession) {
        return repo.save(anonymousSession);
    }

    public Optional<AnonymousSession> findById(UUID id) {
        return repo.findById(id);
    }

    public void delete(AnonymousSession anonymousSession) {
        repo.delete(anonymousSession);
    }

    public void truncateTable() {
        repo.truncateTable();
    }
}
