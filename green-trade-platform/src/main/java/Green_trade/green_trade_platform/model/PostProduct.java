package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "post_product")
public class PostProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "title", nullable = false, unique = false)
    private String title;

    @Column(name = "brand", nullable = false, unique = false)
    private String brand;

    @Column(name = "model", nullable = false, unique = false)
    private String model;

    @Column(name = "manufacture_year", nullable = false, unique = false)
    private Long manufactureYear;

    @Column(name = "used_duration", nullable = false, unique = false)
    public String usedDuration;

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Column(name = "condition_level", nullable = false, unique = false)
    private String conditionLevel;

    @Column(name = "price", nullable = false, unique = false)
    private BigDecimal price;

    @Column(name = "width", nullable = true, unique = false)
    private String width;

    @Column(name = "height", nullable = true, unique = false)
    private String height;

    @Column(name = "length", nullable = true, unique = false)
    private String length;

    @Column(name = "weight", nullable = true, unique = false)
    private String weight;

    @Column(name = "description", nullable = false, unique = false)
    public String description;

    @Column(name = "location_trading", nullable = false, unique = false)
    private String locationTrading;

    @Column(name = "is_sold", nullable = false, unique = false)
    private boolean sold;

    @Column(name = "is_active", nullable = false, unique = false)
    private boolean active;

    @Column(name = "verified_decision_status", nullable = false, unique = false)
    @Enumerated(EnumType.STRING)
    private VerifiedDecisionStatus verifiedDecisionstatus;

    @Column(name = "is_verified", nullable = false, unique = false)
    private boolean verified;

    @Column(name = "created_at", nullable = true, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true, unique = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", nullable = true, unique = false)
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "postProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @ToString.Exclude
    private List<Order> order;

    @OneToMany(mappedBy = "postProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Conversation> conversations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    @JsonManagedReference
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @ToString.Exclude
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @ToString.Exclude
    private Seller seller;

    @OneToMany(mappedBy = "postProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductImage> productImages;

    @OneToMany(mappedBy = "postProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<WishListing> wishListings;
}
