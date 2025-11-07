package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.enumerate.Gender;
import Green_trade.green_trade_platform.request.ApproveSellerRequest;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

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
@Table(name = "admin")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @Column(name = "employee_number", nullable = false, unique = true)
    private String employeeNumber;

    @Column(name = "password", nullable = false, unique = false)
    private String password;

    @Column(name = "full_name", nullable = false, unique = false)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "is_super_admin", nullable = false, unique = false)
    private boolean isSuperAdmin;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "status", nullable = false, unique = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PostProduct> postProducts;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonManagedReference
    private List<Dispute> disputes;

    @OneToMany(mappedBy = "admin", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonManagedReference
    private List<Seller> sellers;

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Announcement> announcements;

    @OneToMany(mappedBy = "admin")
    @ToString.Exclude
    private List<SystemWallet> systemWallets;

    @OneToMany(mappedBy = "admin")
    @ToString.Exclude
    private List<SystemPolicy> systemPolicies;

    @PrePersist
    public void onCreate() {
        this.isSuperAdmin = false;
        this.gender = Gender.MALE;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

}
