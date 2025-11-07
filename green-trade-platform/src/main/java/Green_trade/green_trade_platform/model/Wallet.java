package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.WalletConcurrency;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wallet")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "concurrency")
    @Enumerated(EnumType.STRING)
    private WalletConcurrency concurrency;

    @Column(name = "provider")
    private String provider;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne()
    @JoinColumn(name = "buyer_id")
    @JsonManagedReference
    private Buyer buyer;

    @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    List<WalletTransaction> transactions;

    @PrePersist
    public void onCreate() {
        this.balance = BigDecimal.ZERO;
        this.provider = "VNPay";
        this.createdAt = LocalDateTime.now();
        this.concurrency = WalletConcurrency.VND;
    }

}
