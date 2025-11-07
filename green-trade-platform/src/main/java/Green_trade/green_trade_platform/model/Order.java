package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(name = "order_code", nullable = true, unique = false)
    @ToString.Include
    private String orderCode;

    @Column(name = "shipping_address", nullable = true, unique = false)
    @ToString.Include
    private String shippingAddress;

    @Column(name = "phone_number", nullable = true, unique = false)
    @ToString.Include
    private String phoneNumber;

    @Column(name = "price", nullable = true, unique = false)
    @ToString.Include
    private BigDecimal price;

    @Column(name = "shipping_fee", nullable = true, unique = false)
    @ToString.Include
    private BigDecimal shippingFee;

    @Column(name = "status", nullable = true, unique = false, length = 15)
    @Enumerated(EnumType.STRING)
    @ToString.Include
    private OrderStatus status;

    @Column(name = "created_at", nullable = true, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true, unique = false)
    private LocalDateTime updatedAt;

    @Column(name = "canceled_at", nullable = true, unique = false)
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancel_reason_id")
    @JsonManagedReference
    @ToString.Exclude
    private CancelOrderReason cancelOrderReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    @JsonManagedReference
    @ToString.Exclude
    private Buyer buyer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    @ToString.Exclude
    private List<Review> reviews;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, unique = false)
    @JsonBackReference
    @ToString.Exclude
    private PostProduct postProduct;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "order")
    @JsonBackReference
    @ToString.Exclude
    private Invoice invoice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    @ToString.Exclude
    private List<Dispute> disputes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    @ToString.Exclude
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    @ToString.Exclude
    private List<WalletTransaction> walletTransactions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_partner_id")
    @JsonIgnore
    @JsonBackReference
    @ToString.Exclude
    private ShippingPartner shippingPartner;

    @OneToOne(mappedBy = "order")
    @JsonBackReference
    private SystemWallet systemWallet;

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
