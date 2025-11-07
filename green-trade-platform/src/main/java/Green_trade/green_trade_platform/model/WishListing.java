package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.WishListPriority;
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
@Table(
        name = "wish_listing",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_wish_list_buyer_post",
                        columnNames = {"buyer_id", "post_id"}
                )
        }
)
public class WishListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wish_list_id")
    private Long id;

    @Column(name = "priority", nullable = false, unique = false)
    @Enumerated(EnumType.STRING)
    private WishListPriority priority;

    @Column(name = "note", nullable = false, unique = false)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    @JsonManagedReference
    private Buyer buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostProduct postProduct;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
