package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.Gender;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
@Table(
        name = "buyer",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username", "deleted_at"})}
)
public class Buyer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long buyerId;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "street")
    private String street;

    @Column(name = "ward_name")
    private String wardName;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dob;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(mappedBy = "buyer", cascade = CascadeType.REMOVE)
    @JsonBackReference
    private Seller seller;

    @OneToOne(mappedBy = "buyer", cascade = CascadeType.REMOVE)
    @JsonBackReference
    private Wallet wallet;

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Order> orders;

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Conversation> conversations;

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<WishListing> wishListings;

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Following> followings;

    @PrePersist
    public void onCreate() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
