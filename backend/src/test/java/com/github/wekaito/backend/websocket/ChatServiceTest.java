package com.github.wekaito.backend.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ChatService chatService;
    private WebSocketSession session1;
    private WebSocketSession session2;
    private WebSocketSession session3;

    @BeforeEach
    void setUp() {
        chatService = new ChatService();

        session1 = createMockSession("testUser1");
        session2 = createMockSession("testUser2");
        session3 = createMockSession("testUser3");
    }

    private WebSocketSession createMockSession(String username) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getPrincipal()).thenReturn(() -> username);
        return session;
    }

    @Test
    @DirtiesContext
    void testConnection() {
        chatService.afterConnectionEstablished(session1);

        Set<WebSocketSession> activeSessions = new HashSet<>();
        activeSessions.add(session1);

        assertThat(chatService.getActiveSessions()).isEqualTo(activeSessions);
        assertThat(chatService.getConnectedUsernames()).contains("testUser1");

        chatService.afterConnectionClosed(session1, CloseStatus.NORMAL);
        assertThat(chatService.getActiveSessions()).isEmpty();
        assertThat(chatService.getConnectedUsernames()).isEmpty();
    }

    @Test
    @DirtiesContext
    void testBroadcastConnectedUsernames() throws Exception {
        chatService.afterConnectionEstablished(session1);
        chatService.afterConnectionEstablished(session2);
        chatService.afterConnectionEstablished(session3);

        Set<WebSocketSession> activeSessions = new HashSet<>();
        activeSessions.add(session1);
        activeSessions.add(session2);
        activeSessions.add(session3);

        String userListMessage = String.join(", ", chatService.getConnectedUsernames());
        TextMessage message = new TextMessage(userListMessage);

        for (WebSocketSession session : activeSessions) {
            // broadcastConnectedUsernames() is called in afterConnectionEstablished()
            verify(session, times(1)).sendMessage(message);
        }
    }

    @Test
    @DirtiesContext
    void testHandleTextMessage() throws Exception {
        TextMessage incomingMessage1 = new TextMessage("Hello!");
        TextMessage incomingMessage2 = new TextMessage("Test message.");

        chatService.afterConnectionEstablished(session1);
        chatService.afterConnectionEstablished(session2);

        chatService.handleTextMessage(session1, incomingMessage1);
        chatService.handleTextMessage(session2, incomingMessage2);

        TextMessage outgoingMessage1 = new TextMessage("CHAT_MESSAGE:testUser1: Hello!");
        TextMessage outgoingMessage2 = new TextMessage("CHAT_MESSAGE:testUser2: Test message.");

        verify(session1, times(1)).sendMessage(outgoingMessage1);
        verify(session2, times(1)).sendMessage(outgoingMessage2);
        verify(session3, never()).sendMessage(any());
    }
}
