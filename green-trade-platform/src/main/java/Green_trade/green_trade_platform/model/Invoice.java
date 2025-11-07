package Green_trade.green_trade_platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long id;

    @Column(name = "invoice_number", nullable = true, unique = true)
    private String invoiceNumber;

    @Column(name = "note", nullable = true, unique = false)
    private String note;

    @Column(name = "created_at", nullable = true, unique = false)
    private LocalDateTime createdAt;

    @Column(name = "concurrency", nullable = true, unique = false)
    private String concurrency;

    @Column(name = "tax_rate", nullable = true, unique = false)
    private double taxRate;

    @Column(name = "pdf_url", nullable = true, unique = false)
    private String pdfUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonManagedReference
    private Order order;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
