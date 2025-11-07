package Green_trade.green_trade_platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dispute_category")
public class DisputeCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_category_id")
    private Long id;

    @Column(name = "title", nullable = false, unique = false)
    private String title;

    @Column(name = "reason", nullable = false, unique = false)
    private String reason;

    @Column(name = "description", nullable = false, unique = false)
    private String description;

    @OneToMany(mappedBy = "disputeCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Dispute> disputes;
}
