package com.swisscom.userregister.unit;

import com.swisscom.userregister.domain.entity.Session;
import com.swisscom.userregister.repository.SessionRepository;
import com.swisscom.userregister.service.OpaServerService;
import com.swisscom.userregister.service.SessionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SessionServiceUnitTest {

    public static final String TOKEN = "428034dd06a4465ba1d4995338b90e85";
    public static final String EMAIL = "alice@test.com";
    private final SessionRepository sessionRepository;
    private final SessionService sessionService;

    private final OpaServerService opaServerService;

    SessionServiceUnitTest() {
        this.sessionRepository = mock(SessionRepository.class);
        this.opaServerService = mock(OpaServerService.class);
        this.sessionService = new SessionService(sessionRepository, opaServerService);
    }

    @Test
    void testGenerateAndRegisterWhenEmailHasNoToken() {
        when(sessionRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var token = sessionService.generateAndRegisterToken(EMAIL);

        assertNotNull(token);
        assertNotEquals("", token);

        verify(sessionRepository, times(1)).findByEmail(anyString());
        verify(sessionRepository, times(1)).save(any(Session.class));
        verify(opaServerService, times(1)).synchronizeTokenToOpa(anyString(), anyString());
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void testGenerateAndRegisterWhenEmailHasValidToken() {
        var updateAt = LocalDateTime.now();
        var expirationAt = LocalDateTime.now().plusMinutes(30);

        var existSession = new Session(TOKEN, updateAt, expirationAt, EMAIL);

        when(sessionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existSession));

        var token = sessionService.generateAndRegisterToken(EMAIL);

        assertNotNull(token);
        assertNotEquals("", token);
        assertEquals(TOKEN, token);

        verify(sessionRepository, times(1)).findByEmail(any(String.class));
        verifyNoMoreInteractions(sessionRepository);

        verifyNoInteractions(opaServerService);
    }

    @Test
    void testGenerateAndRegisterWhenEmailHasInvalidToken() {
        var updateAt = LocalDateTime.now().minusDays(2);
        var expirationAt = LocalDateTime.now().minusDays(1);

        var existSession = new Session(TOKEN, updateAt, expirationAt, EMAIL);

        when(sessionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existSession));

        var token = sessionService.generateAndRegisterToken(EMAIL);

        assertNotNull(token);
        assertNotEquals("", token);
        assertNotEquals(TOKEN, token);

        verify(sessionRepository, times(1)).findByEmail(any(String.class));
        verify(sessionRepository, times(1)).save(any(Session.class));
        verify(opaServerService, times(1)).synchronizeTokenToOpa(anyString(), anyString());
        verifyNoMoreInteractions(sessionRepository);
    }

}