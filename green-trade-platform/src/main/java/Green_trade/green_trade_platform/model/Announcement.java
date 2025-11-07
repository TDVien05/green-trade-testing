package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.enumerate.Gender;
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
@Table(name = "announcement")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id")
    private Long id;

    @Column(name = "title", nullable = false, unique = false)
    private String title;

    @Column(name = "content", nullable = false, unique = false)
    private String content;

    @Column(name = "target_receiver", nullable = false, unique = false)
    private String targetReceiver;

    @Column(name = "created_at", nullable = false, unique = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
