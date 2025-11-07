package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.DisputeCategory;
import Green_trade.green_trade_platform.repository.DisputeCategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DisputeCategoryServiceImpl {
    private final DisputeCategoryRepository disputeCategoryRepository;

    public DisputeCategoryServiceImpl(DisputeCategoryRepository disputeCategoryRepository) {
        this.disputeCategoryRepository = disputeCategoryRepository;
    }

    public List<DisputeCategory> getAllDisputeCategory() {
        List<DisputeCategory> disputeCategories = disputeCategoryRepository.findAll();
        return disputeCategories;
    }
}
