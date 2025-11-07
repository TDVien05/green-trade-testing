package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Conversation;
import Green_trade.green_trade_platform.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findAllByConversation(Conversation conversation, Pageable pageable);
}
