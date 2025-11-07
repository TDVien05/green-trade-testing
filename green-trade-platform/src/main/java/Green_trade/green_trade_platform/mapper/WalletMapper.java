package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.model.WalletTransaction;
import Green_trade.green_trade_platform.response.WalletResponse;
import Green_trade.green_trade_platform.response.WalletTransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public WalletResponse toDto(Wallet wallet) {
        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .concurrency(wallet.getConcurrency())
                .build();
    }

    public WalletTransactionResponse toTransactionResponse(WalletTransaction walletTransaction) {
        return WalletTransactionResponse.builder()
                .id(walletTransaction.getTransactionId())
                .type(walletTransaction.getType())
                .amount(walletTransaction.getAmount())
                .balanceBefore(walletTransaction.getBalanceBefore())
                .status(walletTransaction.getStatus())
                .description(walletTransaction.getDescription())
                .createdAt(walletTransaction.getCreatedAt())
                .build();
    }
}
