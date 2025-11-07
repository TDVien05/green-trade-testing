package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Conversation;
import Green_trade.green_trade_platform.repository.ConversationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ConversationServiceImpl {
    private final ConversationRepository conversationRepository;

    public Conversation createConversation(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getConversation(Buyer buyer) {
        return conversationRepository.findByBuyer(buyer);
    }

    public Conversation findById(Long conversationId) {
        return conversationRepository.findById(conversationId).orElseThrow(
                () -> new EntityNotFoundException("Can not find conversation with this id: " + conversationId)
        );
    }
}
