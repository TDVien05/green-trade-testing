package Green_trade.green_trade_platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonManagedReference
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_package_id", nullable = false)
    @JsonManagedReference
    private SubscriptionPackages subscriptionPackage;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "start_day", nullable = false)
    private LocalDateTime startDay;

    @Column(name = "end_day", nullable = false)
    private LocalDateTime endDay;

    @Column(name = "remain_post", nullable = false)
    private long remainPost;

    @PrePersist
    public void onCreate() {
        this.isActive = true;
    }
}
