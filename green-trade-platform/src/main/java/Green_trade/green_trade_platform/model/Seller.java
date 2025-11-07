package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Table(name = "seller")
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sellerId;

    @Column(name = "front_of_identity_url", nullable = false, unique = true)
    private String identityFrontImageUrl;

    @Column(name = "back_of_identity_url", nullable = false, unique = true)
    private String identityBackImageUrl;

    @Column(name = "business_license_url", nullable = false, unique = true)
    private String businessLicenseUrl;

    @Column(name = "selfie_image_url", nullable = false, unique = true)
    private String selfieUrl;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerStatus status;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "ghn_shop_id", nullable = true, unique = false)
    private String ghnShopId;

    @Column(name = "store_policy_url", nullable = false, unique = true)
    private String storePolicyUrl;

    @Column(name = "tax_number", unique = true, nullable = false)
    private String taxNumber;

    @Column(name = "identity_number", unique = true, nullable = false)
    private String identityNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Addition fields
    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "nationality", nullable = false)
    private String nationality;

    // Quê quán
    @Column(name = "home", nullable = false)
    private String home;

    @OneToOne()
    @JoinColumn(name = "buyer_id", nullable = false, unique = true)
    @JsonManagedReference
    private Buyer buyer;

    @PrePersist
    public void onCreate() {
        this.status = SellerStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Following> followings;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    @JsonManagedReference
    private Admin admin;

}
