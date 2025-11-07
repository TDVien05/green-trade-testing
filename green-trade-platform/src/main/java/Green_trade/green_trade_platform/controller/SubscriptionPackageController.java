package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.mapper.SubscriptionMapper;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.model.Subscription;
import Green_trade.green_trade_platform.request.SignPackageRequest;
import Green_trade.green_trade_platform.response.SubscriptionPackageResponse;
import Green_trade.green_trade_platform.service.SellerService;
import Green_trade.green_trade_platform.service.implement.SellerServiceImpl;
import Green_trade.green_trade_platform.service.implement.SubscriptionPackageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/packages")
@RequiredArgsConstructor
public class SubscriptionPackageController {

    private final SubscriptionPackageServiceImpl subscriptionPackageService;
    private final ResponseMapper responseMapper;
    private final SellerServiceImpl sellerService;
    private final SubscriptionMapper subscriptionMapper;

    @Operation(
            summary = "Get active subscription packages for sellers",
            description = """
                        Returns a paginated list of active subscription packages available in the system for sellers to register.  
                        Each package contains details such as package name, description, duration, price, and features.
                    
                        **Workflow:**
                        1. The client calls this endpoint with optional pagination parameters (`page`, `size`).
                        2. The system retrieves all active (enabled) subscription packages from the database.
                        3. The results are returned in a paginated format, sorted by creation or activation date.
                    
                        **Use cases:**
                        - Displaying available subscription plans for sellers on the pricing or upgrade page.
                        - Allowing sellers to choose which package to subscribe to when upgrading their account.
                        - Used by admins or frontend dashboards to show currently active plans.
                    
                        **Security Notes:**
                        - This endpoint may be public or restricted depending on your system configuration.
                        - Data shown includes only active packages (`status = ACTIVE`).
                    """
    )
    @GetMapping("/active")
    public ResponseEntity<?> getActivePackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(responseMapper.toDto(true,
                "Đã lấy thành công các gói.",
                subscriptionPackageService.getActivePackageResponses(PageRequest.of(page, size)), null));
    }

    @Operation(
            summary = "Register or subscribe to a seller package",
            description = """
                        Allows a seller to register (sign) for an active subscription package.  
                        The system validates the seller’s wallet balance, deducts the required amount, 
                        and activates the selected package upon successful payment.
                    
                        **Workflow:**
                        1. The seller selects an active subscription package from the available list.
                        2. The frontend sends a `SignPackageRequest` containing the package ID and optional payment details.
                        3. The system checks the seller’s wallet balance:
                           - If sufficient funds exist, the package is activated and wallet balance is deducted.
                           - If funds are insufficient, the system returns an error response.
                        4. A transaction record is saved, and the subscription is linked to the seller’s account.
                    
                        **Use cases:**
                        - Sellers subscribing to premium plans for additional posting limits or advanced features.
                        - Enabling monetization via recurring or one-time package purchases.
                    
                        **Security Notes:**
                        - Requires JWT authentication (`ROLE_SELLER`).
                        - The authenticated seller can only register packages for their own account.
                    """
    )
    @PreAuthorize("hasRole('ROLE_SELLER')")
    @PostMapping("/sign-package")
    public ResponseEntity<?> signPackage(@RequestBody SignPackageRequest request) {
        Map<String, Object> ans = subscriptionPackageService.handlesignPackage(request);
        if (true == (Boolean) ans.get("success")) {
            return ResponseEntity.ok(responseMapper.toDto(true,
                    "Đăng kí gói người bán thành công.",
                    ans, null));
        } else {
            return ResponseEntity.badRequest().body(responseMapper.toDto(false,
                    "Số dư ví không đủ, vui lòng nạp thêm.",
                    null, ans));
        }
    }

    @PreAuthorize("hasRole('ROLE_SELLER')")
    @Operation(
            summary = "Cancel current seller subscription package",
            description = """
                    Allows an authenticated seller to cancel their active subscription package.  
                    Once this endpoint is called, the seller's current package will be marked as canceled, 
                    and no further benefits or billing will be applied after the current billing cycle.  
                    
                    **Access control:** Only users with the `ROLE_SELLER` authority can call this API.  
                    
                    **Example use case:**  
                    A seller wants to stop their current premium plan and revert to a basic account.  
                    They trigger this endpoint to mark the package as canceled.
                    """
    )
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription() {
        try {
            Seller seller = sellerService.getCurrentUser();
            subscriptionPackageService.cancelSubscription(seller);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "CANCEL SUBSCRIPTION PACKAGE SUCCESSFULLY.",
                    null, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "CANCEL SUBSCRIPTION PACKAGE FAILED.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Get current active subscription package",
            description = """
                    Retrieve the seller's currently active subscription package.  
                    This endpoint returns detailed information about the seller's current plan, including 
                    its type, status, start date, end date, and any remaining duration or benefits.  
                    
                    **Access control:**  
                    - Only authenticated users with the role `ROLE_SELLER` can call this endpoint.  
                    
                    **Example use case:**  
                    A seller opens their account dashboard and wants to view details of their current 
                    subscription plan — for example, to see when it expires or whether auto-renewal is active.
                    """
    )
    @PreAuthorize("hasRole('ROLE_SELLER')")
    @GetMapping("current-subscription")
    public ResponseEntity<?> getCurrentSubscription() {
        log.info(">>> [Subscription controller] Starting");
        try {
            Seller seller = sellerService.getCurrentUser();
            Subscription subscription = subscriptionPackageService.getCurrentSubscription(seller);
            Map<String, Object> data = new HashMap<>();
            String username = seller.getSellerName();

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET CURRENT SUBSCRIPTION SUCCESSFULLY.",
                    subscriptionMapper.toDto(subscription), null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET CURRENT SUBSCRIPTION FAILED.",
                    null, e.getMessage()
            ));
        }
    }


}