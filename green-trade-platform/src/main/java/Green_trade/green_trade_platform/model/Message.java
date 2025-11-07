package Green_trade.green_trade_platform.model;

import Green_trade.green_trade_platform.enumerate.MessageStatus;
import Green_trade.green_trade_platform.enumerate.MessageType;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "sender_id", nullable = false, unique = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false, unique = false)
    private Long receiverId;

    @Column(name = "status", nullable = false, unique = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "attached_url")
    private String attachedUrl;

    @Column(name = "public_image_id")
    private String publicImageId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    @JsonBackReference
    private Conversation conversation;

    @PrePersist
    public void onCreate() {
        this.status = MessageStatus.NOT_READ_YET;
        this.sentAt = LocalDateTime.now();
    }
}
