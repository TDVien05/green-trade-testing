package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.enumerate.Gender;
import Green_trade.green_trade_platform.enumerate.SystemWalletStatus;
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
@Table(name = "system_wallet")
public class SystemWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_wallet_id")
    private Long id;

    @Column(name = "buyer_wallet_id", nullable = false, unique = false)
    private Long buyerWalletId;

    @Column(name = "seller_wallet_id", nullable = false, unique = false)
    private Long sellerWalletId;

    @Column(name = "concurrency", nullable = false, unique = false)
    private String concurrency;

    @Column(name = "balance", nullable = false, unique = false)
    private BigDecimal balance;

    @Column(name = "status", nullable = false, unique = false)
    @Enumerated(EnumType.STRING)
    private SystemWalletStatus status;

    @Column(name = "created_at", nullable = false, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @ManyToOne()
    @JoinColumn(name = "admin_id")
    @JsonManagedReference
    private Admin admin;

    @OneToOne()
    @JoinColumn(name = "order_id")
    @JsonManagedReference
    private Order order;
}
