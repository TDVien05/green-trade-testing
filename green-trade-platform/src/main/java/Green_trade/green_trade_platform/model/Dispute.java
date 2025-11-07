package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.DisputeDecision;
import Green_trade.green_trade_platform.enumerate.DisputeStatus;
import Green_trade.green_trade_platform.enumerate.ResolutionType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "dispute")
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id")
    private Long id;

    @Column(name = "created_at", nullable = false, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true, unique = false)
    private LocalDateTime updatedAt;

    @Column(name = "decision", nullable = true, unique = false)
    @Enumerated(EnumType.STRING)
    private DisputeDecision decision;

    @Column(name = "resolution_type", nullable = true, unique = false)
    @Enumerated(EnumType.STRING)
    private ResolutionType resolutionType;

    @Column(name = "resolution", nullable = true, unique = false)
    private String resolution;

    @Column(name = "status", nullable = false, unique = false)
    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    @Column(name = "description", nullable = true, unique = false)
    private String description;

    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Evidence> evidences;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_category_id", nullable = false)
    @JsonBackReference
    private DisputeCategory disputeCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = true)
    @JsonBackReference
    private Admin admin;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
