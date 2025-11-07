package Green_trade.green_trade_platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscription_packages")
public class SubscriptionPackages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_package_id")
    private Long id;

    @Column(name = "package_name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", nullable = false, unique = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false, unique = false)
    private boolean isActive;

    @Column(name = "max_product", nullable = false, unique = false)
    private Long maxProduct;

    @Column(name = "max_img_per_post", nullable = false, unique = false)
    private Long maxImgPerPost;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "subscriptionPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<PackagePrice> packagePrices;

    @OneToMany(mappedBy = "subscriptionPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Subscription> subscriptions;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
