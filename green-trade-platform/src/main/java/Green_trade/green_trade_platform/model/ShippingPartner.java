package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "shipping_partner")
public class ShippingPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipping_partner_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "partner_name", nullable = false, unique = false)
    private String partnerName;

    @Column(name = "address", nullable = false, unique = false)
    private String address;

    @Column(name = "website_url", nullable = false, unique = false)
    private String websiteUrl;

    @Column(name = "hotline", nullable = false, unique = false)
    private String hotline;

    @Column(name = "created_at", nullable = false, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, unique = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shippingPartner", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @JsonManagedReference
    private List<Order> orders;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
