package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Conversation;
import Green_trade.green_trade_platform.repository.ConversationRepository;
import Green_trade.green_trade_platform.service.implement.ConversationServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private Conversation conversation;
    private Buyer buyer;

    @BeforeEach
    void setUp() {
        buyer = Buyer.builder().buyerId(1L).username("buyer1").email("b1@example.com").password("pwd").build();
        conversation = Conversation.builder().id(10L).buyer(buyer).build();
    }

    @Test
    void shouldSaveConversationSuccessfully() {
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation input = Conversation.builder().buyer(buyer).build();
        Conversation result = conversationService.createConversation(input);

        assertNotNull(result);
        assertEquals(conversation, result);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, times(1)).save(captor.capture());
        assertEquals(buyer, captor.getValue().getBuyer());
    }

    @Test
    void shouldReturnConversationsForBuyer() {
        List<Conversation> expected = Arrays.asList(
                Conversation.builder().id(1L).buyer(buyer).build(),
                Conversation.builder().id(2L).buyer(buyer).build()
        );
        when(conversationRepository.findByBuyer(buyer)).thenReturn(expected);

        List<Conversation> result = conversationService.getConversation(buyer);

        assertNotNull(result);
        assertEquals(expected, result);
        verify(conversationRepository, times(1)).findByBuyer(buyer);
    }

    @Test
    void shouldFindConversationByIdWhenPresent() {
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        Conversation result = conversationService.findById(10L);

        assertNotNull(result);
        assertEquals(conversation, result);
        verify(conversationRepository, times(1)).findById(10L);
    }

    @Test
    void shouldThrowWhenConversationIdNotFound() {
        Long missingId = 999L;
        when(conversationRepository.findById(missingId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> conversationService.findById(missingId));

        assertTrue(ex.getMessage().contains(String.valueOf(missingId)));
        verify(conversationRepository, times(1)).findById(missingId);
    }

    @Test
    void shouldReturnEmptyListWhenBuyerHasNoConversations() {
        when(conversationRepository.findByBuyer(buyer)).thenReturn(Collections.emptyList());

        List<Conversation> result = conversationService.getConversation(buyer);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(conversationRepository, times(1)).findByBuyer(buyer);
    }

    @Test
    void shouldPropagateExceptionWhenSaveFails() {
        RuntimeException repoException = new RuntimeException("DB down");
        when(conversationRepository.save(any(Conversation.class))).thenThrow(repoException);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> conversationService.createConversation(conversation));

        assertSame(repoException, thrown);
        verify(conversationRepository, times(1)).save(conversation);
    }
}
