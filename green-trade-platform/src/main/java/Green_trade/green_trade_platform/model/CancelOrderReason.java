package Green_trade.green_trade_platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.engine.spi.CascadeStyles;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cancel_order_reason")
public class CancelOrderReason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancel_order_id")
    private Long id;

    @Column(name = "cancel_reason_name", nullable = false, unique = true)
    private String cancelReasonName;

    @OneToMany(mappedBy = "cancelOrderReason", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Order> orders;
}
