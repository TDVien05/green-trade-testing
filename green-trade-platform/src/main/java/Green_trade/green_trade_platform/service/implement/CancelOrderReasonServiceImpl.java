package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.CancelOrderReason;
import Green_trade.green_trade_platform.repository.CancelOrderReasonRepository;
import Green_trade.green_trade_platform.service.CancelOrderReasonService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CancelOrderReasonServiceImpl implements CancelOrderReasonService {

    private final CancelOrderReasonRepository cancelOrderReasonRepository;

    public CancelOrderReasonServiceImpl(CancelOrderReasonRepository cancelOrderReasonRepository) {
        this.cancelOrderReasonRepository = cancelOrderReasonRepository;
    }

    public List<CancelOrderReason> getAllCancelOrderReasons() {
        List<CancelOrderReason> reasons = cancelOrderReasonRepository.findAll();

        if (reasons == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(reasons);
    }
}
