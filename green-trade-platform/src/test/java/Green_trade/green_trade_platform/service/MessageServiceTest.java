package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.controller.ChattingSocketController;
import Green_trade.green_trade_platform.enumerate.MessageType;
import Green_trade.green_trade_platform.model.Conversation;
import Green_trade.green_trade_platform.model.Message;
import Green_trade.green_trade_platform.repository.MessageRepository;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ChattingSocketController chattingSocketController;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Conversation conversation;

    @BeforeEach
    void setUp() {
        conversation = Conversation.builder().id(123L).build();
    }

    @Test
    void shouldHandleImageMessageAndNotify() throws Exception {
        Message input = Message.builder()
                .id(1L)
                .senderId(10L)
                .receiverId(20L)
                .conversation(conversation)
                .build();

        Map<String, String> uploadRes = Map.of("publicId", "pub-123", "fileUrl", "https://cdn/img.jpg");
        when(cloudinaryService.upload(any(MultipartFile.class), eq("message/" + conversation.getId()))).thenReturn(uploadRes);

        ArgumentCaptor<Message> saveCaptor = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.save(saveCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Message result = messageService.handleImageMessage(input, multipartFile);

        assertNotNull(result);
        assertEquals(MessageType.FILE, result.getMessageType());
        assertEquals("pub-123", result.getPublicImageId());
        assertEquals("https://cdn/img.jpg", result.getAttachedUrl());

        verify(messageRepository, times(1)).save(any(Message.class));
        verify(chattingSocketController, times(1)).sendMessage(result);

        Message saved = saveCaptor.getValue();
        assertEquals(MessageType.FILE, saved.getMessageType());
        assertEquals("pub-123", saved.getPublicImageId());
        assertEquals("https://cdn/img.jpg", saved.getAttachedUrl());
    }

    @Test
    void shouldHandleTextMessageAndNotify() {
        Message input = Message.builder()
                .id(2L)
                .senderId(10L)
                .receiverId(30L)
                .conversation(conversation)
                .content("Hello")
                .build();

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message result = messageService.handleTextmessage(input);

        assertNotNull(result);
        assertEquals(MessageType.TEXT, result.getMessageType());
        verify(messageRepository, times(1)).save(result);
        verify(chattingSocketController, times(1)).sendMessage(result);
    }

    @Test
    void shouldGetConversationMessagesWithPaginationAndSorting() {
        int page = 1, size = 5;
        List<Message> messages = List.of(
                Message.builder().id(3L).conversation(conversation).build(),
                Message.builder().id(4L).conversation(conversation).build()
        );
        Page<Message> expectedPage = new PageImpl<>(messages, PageRequest.of(page, size, Sort.by("sentAt").descending()), 10);

        when(messageRepository.findAllByConversation(eq(conversation), any(Pageable.class))).thenReturn(expectedPage);

        Page<Message> result = messageService.getConversationMessages(page, size, conversation);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(10, result.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findAllByConversation(eq(conversation), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertEquals(page, used.getPageNumber());
        assertEquals(size, used.getPageSize());
        assertTrue(used.getSort().getOrderFor("sentAt").isDescending());
    }

    @Test
    void shouldNotSaveOrNotifyWhenImageUploadFails() throws Exception {
        Message input = Message.builder()
                .id(5L)
                .senderId(11L)
                .receiverId(21L)
                .conversation(conversation)
                .build();

        when(cloudinaryService.upload(any(MultipartFile.class), anyString())).thenThrow(new IOException("upload failed"));

        assertThrows(IOException.class, () -> messageService.handleImageMessage(input, multipartFile));

        verify(messageRepository, never()).save(any());
        verify(chattingSocketController, never()).sendMessage(any());
    }

    @Test
    void shouldHandleMissingUploadFieldsForImageMessage() throws Exception {
        Message input = Message.builder()
                .id(6L)
                .senderId(12L)
                .receiverId(22L)
                .conversation(conversation)
                .build();

        Map<String, String> uploadRes = new HashMap<>();
        // intentionally missing "publicId" and "fileUrl"
        when(cloudinaryService.upload(any(MultipartFile.class), anyString())).thenReturn(uploadRes);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Message result = messageService.handleImageMessage(input, multipartFile);

        assertNotNull(result);
        assertEquals(MessageType.FILE, result.getMessageType());
        assertNull(result.getPublicImageId());
        assertNull(result.getAttachedUrl());

        verify(messageRepository, times(1)).save(result);
        verify(chattingSocketController, times(1)).sendMessage(result);
    }

    @Test
    void shouldHandleInvalidPaginationParameters() {
        Conversation conv = conversation;

        assertThrows(IllegalArgumentException.class, () ->
                messageService.getConversationMessages(-1, 10, conv)
        );
        assertThrows(IllegalArgumentException.class, () ->
                messageService.getConversationMessages(0, 0, conv)
        );
        assertThrows(IllegalArgumentException.class, () ->
                messageService.getConversationMessages(1, -5, conv)
        );

        verify(messageRepository, never()).findAllByConversation(any(), any());
    }
}
