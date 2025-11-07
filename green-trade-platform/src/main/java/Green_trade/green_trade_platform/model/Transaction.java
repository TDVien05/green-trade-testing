package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.PaymentMethod;
import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.enumerate.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @Column(name = "amount", nullable = false, unique = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, unique = false)
    private String currency;

    @Column(name = "status", nullable = false, unique = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "payment_method", nullable = false, unique = false)
    private String paymentMethod;

    @Column(name = "created_at", nullable = false, unique = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonManagedReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    @JsonManagedReference
    private Payment payment;
}
