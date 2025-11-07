package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Notification;
import Green_trade.green_trade_platform.service.implement.AdminServiceImpl;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.NotificationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationServiceImpl notificationService;
    private final NotificationSocketController socketController;
    private final BuyerServiceImpl buyerService;
    private final AdminServiceImpl adminService;

    @Operation(
            summary = "Get all notifications for current user",
            description = """
                        Retrieves a list of notifications for the currently authenticated user (either Buyer or Admin).
                        The system determines the receiver based on the access token provided in the request header.
                    
                        **Workflow:**
                        1. The system extracts user information from the authentication context.
                        2. If the user is a Buyer, notifications are fetched using the Buyer's ID.
                        3. If the user is an Admin, notifications are fetched using the Admin's ID.
                        4. The endpoint returns all notifications associated with that user.
                    
                        **Use cases:**
                        - Buyers checking updates such as order status, dispute results, or account changes.
                        - Admins viewing system alerts or KYC-related notifications.
                    
                        **Security Notes:**
                        - Requires authentication via JWT (either Buyer or Admin).
                        - Returns notifications specific to the authenticated user only.
                    """
    )
    @GetMapping("")
    public ResponseEntity<List<Notification>> getAll() {
        Long receiverId = 0L;
        try {
            Buyer buyer = buyerService.getCurrentUser();
            receiverId = buyer.getBuyerId();
        } catch (Exception e) {
            Admin admin = adminService.getCurrentUser();
            receiverId = admin.getId();
        }
        return ResponseEntity.ok(notificationService.getNotifications(receiverId));
    }

//    // Create a new notification (and push to user via WebSocket)
//    @PostMapping("/new-notification")
//    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
//        Notification saved = notificationService.createNotification(notification);
//        socketController.sendNotificationToUser(saved);
//        return ResponseEntity.ok(saved);
//    }

    @Operation(
            summary = "Mark notification as read",
            description = """
                        Marks a specific notification as read for the currently authenticated user.  
                        Once marked, the notification will no longer appear as unread in subsequent API calls or in the UI.
                    
                        **Workflow:**
                        1. The client sends a `PUT` request with the notification ID in the URL path.
                        2. The system validates that the notification exists and belongs to the authenticated user.
                        3. The notification status is updated to `read = true`.
                        4. A `204 No Content` response is returned on success.
                    
                        **Use cases:**
                        - Users marking individual notifications as read.
                        - Frontend applications updating notification badges in real time after user interaction.
                    
                        **Security Notes:**
                        - Requires JWT authentication (`ROLE_BUYER` or `ROLE_ADMIN`).
                        - A user can only mark their own notifications as read.
                    """
    )
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}
