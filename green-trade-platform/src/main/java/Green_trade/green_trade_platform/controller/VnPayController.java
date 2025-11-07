package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.mapper.WalletMapper;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.service.implement.VnPayServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vnpay")
@Slf4j
public class VnPayController {
    @Value(("${vnp_HashSecret}"))
    private String secretKey;
    @Value("${vnp_HashSecret}")
    private String vnpHashSecret;

    private final VnPayServiceImpl vnPayService;
    private final ResponseMapper responseMapper;
    private final WalletServiceImpl walletServiceImpl;
    private final WalletMapper walletMapper;

    public VnPayController(VnPayServiceImpl vnPayService,
                           ResponseMapper responseMapper,
                           WalletServiceImpl walletServiceImpl,
                           WalletMapper walletMapper) {
        this.vnPayService = vnPayService;
        this.responseMapper = responseMapper;
        this.walletServiceImpl = walletServiceImpl;
        this.walletMapper = walletMapper;
    }

    @Operation(
            summary = "Create VNPay payment link",
            description = """
                        Creates a VNPay payment URL for users to initiate a payment process.  
                        The system communicates with the VNPay API, generates a secure payment URL, 
                        and returns it to the client. The user can then be redirected to this URL to complete the transaction.
                    
                        **Workflow:**
                        1. The client sends a request with the desired payment `amount`.
                        2. The server constructs a payment request containing order details, amount, timestamp, 
                           and the client's IP address (extracted from the `HttpServletRequest`).
                        3. The request is signed using VNPay’s secure hash algorithm (SHA256 or HMAC-SHA512).
                        4. A VNPay payment URL is generated and returned to the frontend.
                        5. The user is redirected to VNPay to complete the transaction.
                    
                        **Use cases:**
                        - Wallet top-up for buyers or sellers.
                        - Paying for premium packages or transactions through VNPay.
                        - Integrating VNPay checkout functionality into your e-commerce flow.
                    
                        **Security Notes:**
                        - The endpoint should be protected if it’s part of an authenticated payment workflow.
                        - The `amount` should be validated server-side to prevent tampering.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(HttpServletRequest request, @RequestParam long amount) {
        try {
            Map<String, Object> result = vnPayService.createPaymentUrl(request, amount);
            return ResponseEntity.ok(
                    responseMapper.toDto(true,
                            "Tạo liên kết thanh toán thành công.",
                            result,
                            null));
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("code", "99");
            error.addProperty("message", "error: " + e.getMessage());
            return ResponseEntity.badRequest().body(
                    responseMapper.toDto(false,
                            "Không thể tạo link thanh toán.",
                            error,
                            e));
        }
    }

    @Operation(
            summary = "Handle VNPay payment return callback",
            description = """
                        Handles the return callback from VNPay after a user completes or cancels a payment.  
                        This endpoint is invoked by VNPay once the transaction process finishes, providing details such as 
                        transaction status, order information, and payment amount.
                    
                        **Workflow:**
                        1. VNPay redirects the user to this endpoint after payment completion.
                        2. The system validates the VNPay return parameters (signature, response code, etc.).
                        3. If `vnp_ResponseCode = "00"`, the payment is successful:
                           - The corresponding wallet is credited with the transaction amount.
                           - Transaction details are saved in the system.
                        4. If the response code is not `"00"`, the payment failed or was canceled.
                        5. A message indicating the payment result is returned to the frontend.
                    
                        **Use cases:**
                        - Processing wallet top-up success or failure.
                        - Confirming order payment results from VNPay.
                        - Automatically updating wallet balance and transaction logs.
                    
                        **Security Notes:**
                        - This endpoint is typically accessed by VNPay's redirect (user + VNPay callback).
                        - Request validation (signature, hash integrity) should be implemented server-side.
                    """
    )
    @GetMapping("/return")
    public ResponseEntity<?> handleVnPayReturn(HttpServletRequest request) {
        log.info(">>> [VNPay Return] New request received at {}", new Date());
        log.info(">>> [VNPay param]: {}", request.toString());
        Map<String, Object> result = vnPayService.processReturn(request);
        if (result.get("response_code").equals("00")) {
            Map<String, String> inputData = new HashMap<>();

            // Lấy toàn bộ tham số vnp_ gửi về
            request.getParameterMap().forEach((key, value) -> {
                if (key.startsWith("vnp_")) {
                    inputData.put(key, value[0]);
                }
            });

            Wallet wallet = walletServiceImpl.processDepositMoneyIntoWallet(inputData);
            return ResponseEntity.ok(responseMapper.toDto(
                    true, "Nạp tiền thành công.",
                    walletMapper.toDto(wallet), null));
        }
        return ResponseEntity.ok(responseMapper.toDto(
                false, "Nạp tiền không thành công.",
                result.get("response_code"), null));
    }


    //    Nếu deploy được thì lấy code này để làm
    @Operation(
            summary = "Handle VNPay Instant Payment Notification (IPN)",
            description = """
                        Handles VNPay's Instant Payment Notification (IPN) — a **server-to-server** callback sent by VNPay to confirm the final status of a payment.  
                        This endpoint is called automatically by VNPay’s system, even if the user closes the browser before returning to the site.
                    
                        **Workflow:**
                        1. VNPay sends a GET request with multiple parameters (`vnp_Amount`, `vnp_TxnRef`, `vnp_ResponseCode`, `vnp_SecureHash`, etc.).
                        2. The system verifies the integrity of the data and validates the `vnp_SecureHash` signature.
                        3. If `vnp_ResponseCode = "00"`, the transaction is successful:
                           - The corresponding wallet is credited with the deposited amount.
                           - Transaction details are logged in the database.
                        4. If the response code is not `"00"`, the transaction is considered failed or canceled.
                        5. The system responds to VNPay with an acknowledgment of the processing result.
                    
                        **Difference between `/ipn` and `/return`:**
                        - `/ipn` → Called automatically by VNPay (server-to-server). Used for **final confirmation**.
                        - `/return` → Called via user redirection after payment. Used for **frontend acknowledgment**.
                    
                        **Use cases:**
                        - Synchronizing payment success/failure status in the backend.
                        - Crediting wallet balances reliably, even if user doesn’t return to the site.
                        - Ensuring payment data integrity between VNPay and internal systems.
                    
                        **Security Notes:**
                        - Public endpoint, but requires signature validation (`vnp_SecureHash`) for authentication.
                        - Idempotent processing is strongly recommended to prevent double deposits.
                    """
    )
    @GetMapping("/ipn")
    public ResponseEntity<?> ipn(HttpServletRequest request) {
        Map<String, Object> result = vnPayService.processReturn(request);
        if (result.get("response_code").equals("00")) {
            Map<String, String> inputData = new HashMap<>();

            // Lấy toàn bộ tham số vnp_ gửi về
            request.getParameterMap().forEach((key, value) -> {
                if (key.startsWith("vnp_")) {
                    inputData.put(key, value[0]);
                }
            });

            Wallet wallet = walletServiceImpl.processDepositMoneyIntoWallet(inputData);
            return ResponseEntity.ok(responseMapper.toDto(
                    true, "Nạp tiền thành công.",
                    walletMapper.toDto(wallet), null));
        }
        return ResponseEntity.ok(responseMapper.toDto(
                false, "Nạp tiền không thành công.",
                result.get("response_code"), null));
    }

}
