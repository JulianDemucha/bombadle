package com.bombadle.service.admin;

import com.bombadle.dto.AdminAuditLogDto;
import com.bombadle.entity.AdminAuditLog;
import com.bombadle.repository.AdminAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminAuditServiceTest {

    @InjectMocks
    private AdminAuditService adminAuditService;

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Test
    void logAction_savesAuditLog() {
        adminAuditService.logAction(1L, "some-action", "some-message");
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(adminAuditLogRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getActorId());
        assertEquals("some-action", captor.getValue().getActionType());
        assertEquals("some-message", captor.getValue().getDescription());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void getById_logExists_returnsOptionalNotEmpty() {
        AdminAuditLog log = AdminAuditLog.builder()
                .id(1L)
                .actorId(67L)
                .actionType("some-action")
                .description("some-message")
                .createdAt(Instant.now())
                .build();

        when(adminAuditLogRepository.findById(1L)).thenReturn(Optional.of(log));

        Optional<AdminAuditLogDto> dtoOpt = adminAuditService.getById(1L);

        assertTrue(dtoOpt.isPresent());
        AdminAuditLogDto dto = dtoOpt.get();
        assertNotNull(dto);
        assertEquals(dto.actionType(), log.getActionType());
        assertEquals(dto.actorId(), log.getActorId());
        assertEquals(dto.description(), log.getDescription());
        assertEquals(dto.createdAt(), log.getCreatedAt());
    }

    @Test
    void getById_logDoesNotExist_returnsOptionalEmpty() {
        when(adminAuditLogRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<AdminAuditLogDto> dtoOpt = adminAuditService.getById(1L);
        assertFalse(dtoOpt.isPresent());
    }

    @Test
    void getByActorId_logsExists_returnsListOfDto() {
        Instant time  = Instant.now();
        when(adminAuditLogRepository.findAllByActorIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(
                        AdminAuditLog.builder()
                                .id(1L)
                                .createdAt(time)
                                .actionType("some-action-like-deleting-whole-player-base")
                                .build(),
                        AdminAuditLog.builder()
                                .id(2L)
                                .actorId(67L)
                                .description("ok")
                                .build()
                ));

        List<AdminAuditLogDto> dtos = adminAuditService.getByActorId(1L);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        AdminAuditLogDto dto = dtos.get(0);
        AdminAuditLogDto dto2 = dtos.get(1);
        assertNotNull(dto);
        assertNotNull(dto2);

        assertEquals(1L, dto.id());
        assertEquals(time, dto.createdAt());
        assertEquals("some-action-like-deleting-whole-player-base", dto.actionType());
        assertEquals(2L, dto2.id());
        assertEquals(67L, dto2.actorId());
        assertEquals("ok", dto2.description());


    }

    @Test
    void getByActorId_logDoesNotExist_returnsEmptyList() {
        when(adminAuditLogRepository.findAllByActorIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        List<AdminAuditLogDto> dtos = adminAuditService.getByActorId(1L);
        assertNotNull(dtos);
        assertEquals(0, dtos.size());
        assertEquals(List.of(), dtos);
    }

    @Test
    void getByActorId_logsExists_returnsPageOfDto() {
        Instant time  = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);
        when(adminAuditLogRepository.findAllByActorId(1L, pageable)).thenReturn(new PageImpl<>(List.of(
                AdminAuditLog.builder()
                        .id(1L)
                        .createdAt(time)
                        .actionType("some-action-like-deleting-whole-player-base")
                        .build(),
                AdminAuditLog.builder()
                        .id(2L)
                        .actorId(67L)
                        .description("ok")
                        .build()
        )));
        Page<AdminAuditLogDto> dtos = adminAuditService.getByActorId(1L, pageable);

        assertNotNull(dtos);
        assertEquals(2, dtos.getTotalElements());
        AdminAuditLogDto dto = dtos.getContent().get(0);
        AdminAuditLogDto dto2 = dtos.getContent().get(1);
        assertNotNull(dto);
        assertNotNull(dto2);

        assertEquals(1L, dto.id());
        assertEquals(time, dto.createdAt());
        assertEquals("some-action-like-deleting-whole-player-base", dto.actionType());
        assertEquals(2L, dto2.id());
        assertEquals(67L, dto2.actorId());
        assertEquals("ok", dto2.description());
    }
}
