package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Payment;
import Green_trade.green_trade_platform.response.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toDto(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .gatewayName(payment.getGatewayName())
                .description(payment.getDescription())
                .build();
    }
}
