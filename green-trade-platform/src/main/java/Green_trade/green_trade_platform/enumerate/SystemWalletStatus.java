package Green_trade.green_trade_platform.enumerate;

public enum SystemWalletStatus {
    //Tiền đang được giữ tạm trong escrow
    ESCROW_HOLD,

    //tiền đã được giải phóng đến người bán
    RELEASED,

    //tiền hoàn lại cho người mua
    REFUNDED,

    // Đánh dấu là đã được giải quyết
    IS_SOLVED
}
