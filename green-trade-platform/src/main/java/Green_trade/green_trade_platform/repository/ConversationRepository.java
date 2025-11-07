package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByBuyer(Buyer buyer);

    <S extends Conversation> S insert(S entity);

    <S extends Conversation> S update(S entity);
}
