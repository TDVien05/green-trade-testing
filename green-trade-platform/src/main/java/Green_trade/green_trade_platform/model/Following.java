package Green_trade.green_trade_platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table(name = "following")
public class Following {
    @EmbeddedId
    private FollowingId id;

    @ManyToOne
    @MapsId("buyerId")
    @JoinColumn(name = "buyer_id")
    @JsonManagedReference
    private Buyer buyer;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    @MapsId("sellerId")
    @JsonManagedReference
    private Seller seller;

    @Column(name = "followed_at", nullable = false)
    private LocalDateTime followedAt;

    @Column(name = "unfollowed_at")
    private LocalDateTime unfollowedAt;
}
