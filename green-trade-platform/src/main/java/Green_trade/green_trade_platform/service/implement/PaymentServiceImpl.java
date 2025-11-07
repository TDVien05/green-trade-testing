package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.Payment;
import Green_trade.green_trade_platform.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentServiceImpl {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment findPaymentMethodById(Long id) {
        Payment paymentFound = null;
        Optional<Payment> paymentOpt = paymentRepository.findById(id);
        if (paymentOpt.isPresent()) {
            paymentFound = paymentOpt.get();
        }
        return paymentFound;
    }
}
