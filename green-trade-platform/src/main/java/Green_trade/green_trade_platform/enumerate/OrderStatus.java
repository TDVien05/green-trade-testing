package Green_trade.green_trade_platform.enumerate;

public enum OrderStatus {
    // Đơn hàng bị hủy
    CANCELED,
    // Đơn hàng mới được tạo nhưng chưa thanh toán
    PENDING,
    // Đang chờ xác nhận từ người bán hoặc hệ thống
    CONFIRMED,
    // Đã thanh toán thành công
    PAID,
    // Đơn hàng đang được chuẩn bị để giao
    PROCESSING,
    // Đơn hàng đã được giao cho đơn vị vận chuyển
    SHIPPED,
    // Đơn hàng đang được giao cho khách hàng
    IN_TRANSIT,
    // Đơn hàng đã giao thành công
    DELIVERED,
    // Khách hàng đã nhận hàng
    COMPLETED,
    // Đơn hàng bị trả lại hoặc yêu cầu hoàn tiền
    RETURN_REQUESTED,
    // Đơn hàng đã được hoàn tiền
    REFUNDED,
    // Đơn hàng đã được người bán xác nhận bán
    VERIFIED
}
