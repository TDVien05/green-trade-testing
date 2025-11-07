package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import Green_trade.green_trade_platform.enumerate.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallet_transaction")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "type", length = 50)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "balance_before")
    private BigDecimal balanceBefore;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "description")
    private String description;

    @Column(name = "external_transaction_reference")
    private String externalTransactionReference;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne()
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @ManyToOne()
    @JoinColumn(name = "order_id")
    private Order order;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
