package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.mapper.AdminMapper;
import Green_trade.green_trade_platform.mapper.PostProductListMapper;
import Green_trade.green_trade_platform.mapper.PostProductMapper;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.request.ApproveSellerRequest;
import Green_trade.green_trade_platform.request.CreateAdminRequest;
import Green_trade.green_trade_platform.request.NeedVerifyPostRequest;
import Green_trade.green_trade_platform.request.PostProductDecisionRequest;
import Green_trade.green_trade_platform.response.*;
import Green_trade.green_trade_platform.service.implement.AdminServiceImpl;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.PostProductServiceImpl;
import Green_trade.green_trade_platform.service.implement.SellerServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final SellerServiceImpl sellerService;
    private final PostProductServiceImpl postProductServiceImpl;
    private final ResponseMapper responseMapper;
    private final AdminServiceImpl adminService;
    private final AdminMapper adminMapper;
    private final PostProductMapper postProductMapper;
    private final PostProductListMapper postProductListMapper;
    private final NotificationSocketController socketController;
    private final BuyerServiceImpl buyerService;

    @Operation(
            summary = "Get all pending seller accounts",
            description = """
                        Retrieves a paginated list of seller accounts that are currently in a pending verification or approval state.
                        This endpoint is restricted to administrators only (requires ROLE_ADMIN authority).
                    
                        The API supports pagination using the 'page' and 'size' query parameters.
                    
                        Response includes:
                        - A list of sellers awaiting approval (`sellers`)
                        - Pagination details such as current page, total elements, and total pages
                    
                        Typical use cases:
                        - Admin dashboard for managing seller approvals
                    """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/pending-seller")
    public ResponseEntity<?> findAllPendingSeller(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<SellerResponse> ans = sellerService.getAllPendingSeller(page, size);
        Map<String, Object> body = new HashMap<>();
        body.put("sellers", ans.getContent());
        body.put("currentPage", ans.getNumber());
        body.put("totalElements", ans.getTotalElements());
        body.put("totalPage", ans.getTotalPages());

        return ResponseEntity.ok(body);
    }

    @Operation(
            summary = "Approve or reject a pending seller account",
            description = """
                        Handles the approval or rejection process for a pending seller registration request. 
                        This endpoint is restricted to administrators and requires a valid bearer token.
                    
                        The request body should contain seller information along with an approval decision. 
                        If approved, the seller's account status is updated and a notification is sent to the user 
                        in real time via WebSocket.
                    
                        **Workflow:**
                        1. Admin submits approval/rejection data through this endpoint.
                        2. The system updates the seller’s status.
                        3. A notification is constructed and timestamped (`sendAt`).
                        4. The notification is sent to the corresponding seller user through a socket event.
                    
                        **Use cases:**
                        - Approving verified sellers after document validation.
                        - Rejecting invalid or incomplete seller registration requests.
                    """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/approve-seller")
    public ResponseEntity<RestResponse<?, ?>> handlePendingSeller(@RequestBody ApproveSellerRequest request) throws JsonProcessingException {
        ApproveSellerResponse sellerNotification = sellerService.handlePendingSeller(request);
        sellerNotification.getNotification().setSendAt(LocalDateTime.now());
        socketController.sendUpgradeNotificationToUser(sellerNotification);
        return ResponseEntity.ok(responseMapper.toDto(true,
                "Approve request was be solved.",
                sellerNotification, null));
    }

    @Operation(
            summary = "Create a new admin account",
            description = """
                        Allows an existing administrator to create a new admin account in the system.
                        This endpoint accepts both form data and a profile image file (`avatar_url`) for the new admin.
                    
                        The request must include valid admin details (username, email, password, role, etc.) 
                        and an avatar image. The uploaded avatar will be processed and linked to the new account.
                    
                        **Workflow:**
                        1. Admin submits a multipart/form-data request containing admin details and an avatar image.
                        2. The system validates the request and saves the image.
                        3. The new admin account is created and persisted in the database.
                        4. A success response is returned with the created admin's information.
                    
                        **Use cases:**
                        - Registering additional admin users for system management.
                        - Managing multi-admin access in the platform.
                    """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("creating-admin")
    public ResponseEntity<?> handleCreatingAdmin(
            @Valid @ModelAttribute CreateAdminRequest request,
            @RequestPart(value = "avatar_url", required = true) MultipartFile avatarFile
    ) {
        try {
            Admin data = adminService.handleCreateAdminAccount(avatarFile, request);
            return ResponseEntity.ok(responseMapper.toDto(true,
                    "Create admin account successfully.",
                    adminMapper.toDto(data),
                    null));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(false,
                    "Create admin account failed.",
                    null,
                    e));
        }

    }

    @Operation(
            summary = "Review Post Product List (Admin Only)",
            description = """
                        Retrieves a paginated list of post products that are pending verification or review by administrators.  
                        This API is restricted to users with the `ROLE_ADMIN` authority.
                    
                        **Workflow:**
                        1. The admin calls this endpoint with pagination parameters (`page`, `size`).
                        2. The system queries all post products that are awaiting verification or moderation.
                        3. The endpoint returns a paginated response containing post details, along with metadata.
                    
                        **Use cases:**
                        - Admins reviewing newly submitted product posts before approval.
                        - Moderators checking flagged or edited posts that require re-verification.
                        - Ensuring quality control and compliance of product listings before publication.
                    
                        **Security Notes:**
                        - Requires JWT authentication with `ROLE_ADMIN`.
                        - Unauthorized users (buyers/sellers) will be denied access.
                    """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/review-post-seller-list")
    public ResponseEntity<RestResponse<PostProductListResponse, Object>> getAllPostProductForReview(
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "0") int page
    ) throws Exception {
        log.info(">>> Server came getAllPostProductForReview API");
        Page<PostProduct> postProducts = postProductServiceImpl.getAllPostProductForVerifiedReview(size, page);
        log.info(">>> Server ran postProductServiceImpl.getAllPostProduct()");

        Map<String, Object> meta = Map.of(
                "currentPage", postProducts.getNumber(),
                "totalElements", postProducts.getTotalElements(),
                "totalPage", postProducts.getTotalPages()
        );

        PostProductListResponse responseData = postProductListMapper.toDto(postProducts.getContent(), meta);

        RestResponse<PostProductListResponse, Object> response = responseMapper.toDto(
                true,
                "POST PRODUCT LIST",
                responseData,
                null
        );

        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER', 'ROLE_ADMIN')")
    @Operation(
            summary = "View post product details by ID",
            description = """
                        Retrieves detailed information for a specific post product based on its unique ID.  
                        Accessible to **Buyers**, **Sellers**, and **Admins** with appropriate privileges.
                    
                        **Workflow:**
                        1. The client sends a request containing the post product ID as a path variable.
                        2. The system retrieves the corresponding post product record from the database.
                        3. The product details are returned as a structured response, including product info, seller data, and review status.
                    
                        **Use cases:**
                        - **Admin:** Reviewing pending or verified posts before approval or publication.
                        - **Seller:** Viewing or verifying their own product submission details.
                        - **Buyer:** Viewing detailed product information for browsing or purchasing decisions.
                    
                        **Security Notes:**
                        - Requires JWT authentication (`ROLE_BUYER`, `ROLE_SELLER`, or `ROLE_ADMIN`).
                        - Different roles may have access to different levels of detail based on internal authorization rules.
                    """
    )
    @GetMapping("/{postProductId}/post-details")
    public ResponseEntity<RestResponse<PostProductResponse, Object>> viewPostProductDetail(
            @PathVariable Long postProductId
    ) throws Exception {
        PostProduct postProduct = postProductServiceImpl.getPostProductById(postProductId);
        PostProductResponse responseData = postProductMapper.toDto(postProduct);
        RestResponse<PostProductResponse, Object> response = responseMapper.toDto(
                true,
                "POST PRODUCT DETAIL",
                responseData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
            summary = "Review and decide post product verification",
            description = """
                        Allows an admin or moderator to approve or reject a seller's post product after manual review.
                        This endpoint records the decision, updates the product's verification status, 
                        and returns the updated product details.
                    
                        **Workflow:**
                        1. Admin sends a decision request containing the post product ID and decision details (approve or reject).
                        2. The system validates the product and applies the verification decision.
                        3. The updated post product entity is returned in the response.
                    
                        **Use cases:**
                        - Approving a verified product for listing.
                        - Rejecting a product submission with a reason or remark.
                        - Managing product moderation workflows.
                    """
    )
    @PostMapping("/review-post-product-decision")
    public ResponseEntity<RestResponse<PostProductResponse, Object>> reviewPostProductDecision(@Valid @RequestBody PostProductDecisionRequest request) throws Exception {
        PostProduct result = postProductServiceImpl.checkPostProductVerification(request);
        PostProductResponse responseData = postProductMapper.toDto(result);
        RestResponse response = responseMapper.toDto(
                true,
                "POST HAS BEEN CHECKED",
                responseData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @Operation(
            summary = "Block or unblock user account (buyer, seller, or admin)",
            description = """
                    This endpoint allows an **administrator** to block or unblock a user account based on its type and ID.  
                    Supported account types include **buyer**, **seller**, and **admin**.
                    
                    The action performed (block or unblock) depends on the provided `activity` parameter.  
                    A message explaining the reason for the action can also be passed.
                    
                    - For **buyer** and **seller** accounts: the platform will call their respective services to perform the action.  
                    - For **admin** accounts: only a **super admin** can perform block or unblock operations.  
                    - If an invalid account type is provided, the system returns a **400 Bad Request** response.
                    
                    **Access Control:** Only users with the role `ROLE_ADMIN` are authorized to use this API.
                    
                    **Path Parameters:**
                    - `accountId` – The ID of the account to block or unblock.  
                    - `accountType` – The type of the account (`buyer`, `seller`, or `admin`).  
                    - `message` – A short explanation or note about the action (e.g., "Violation of policy").  
                    - `activity` – Defines the action to perform: `"block"` or `"unblock"`.
                    
                    **Response:**
                    - **Success:** Returns a message confirming that the account was blocked or unblocked successfully.  
                    - **Failure:** Returns an error message describing the issue (e.g., invalid type, insufficient permission, or internal error).
                    """
    )
    @PreAuthorize(("hasRole('ROLE_ADMIN')"))
    @PostMapping("/block-account/{accountId}/{accountType}/{message}/{activity}")
    public ResponseEntity<?> blockAccount(
            @PathVariable(name = "accountId") long id,
            @PathVariable(name = "accountType") String type,
            @PathVariable(name = "message") String message,
            @PathVariable(name = "activity") String activity
    ) {
        try {
            String successMessage;
            String actionText;

            // ✅ Xác định hành động (Block hoặc Unblock)
            if ("block".equalsIgnoreCase(activity)) {
                actionText = "BLOCK";
            } else if ("unblock".equalsIgnoreCase(activity)) {
                actionText = "UNBLOCK";
            } else {
                return ResponseEntity.badRequest().body(responseMapper.toDto(
                        false,
                        "INVALID ACTIVITY.",
                        null,
                        "Activity must be either 'block' or 'unblock'."
                ));
            }

            // ✅ Xử lý theo loại tài khoản
            if ("buyer".equalsIgnoreCase(type)) {
                buyerService.blockAccount(id, message, activity);
                successMessage = String.format("%s BUYER ACCOUNT SUCCESSFULLY.", actionText);
            } else if ("seller".equalsIgnoreCase(type)) {
                sellerService.blockAccount(id, message, activity);
                successMessage = String.format("%s SELLER ACCOUNT SUCCESSFULLY.", actionText);
            } else if ("admin".equalsIgnoreCase(type)) {
                Admin admin = adminService.getCurrentUser();
                if (!admin.isSuperAdmin()) {
                    throw new IllegalArgumentException("You do not have permission to block or unblock admin accounts.");
                }
                adminService.blockAccount(id, message, activity);
                successMessage = String.format("%s ADMIN ACCOUNT SUCCESSFULLY.", actionText);
            } else {
                return ResponseEntity.badRequest().body(responseMapper.toDto(
                        false,
                        "INVALID ACCOUNT TYPE.",
                        null,
                        "Type must be either 'buyer', 'seller', or 'admin'."
                ));
            }

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    successMessage,
                    null,
                    null
            ));

        } catch (Exception e) {
            String actionText = activity.equalsIgnoreCase("unblock") ? "UNBLOCK" : "BLOCK";

            String errorMsg = switch (type.toLowerCase()) {
                case "buyer" -> String.format("%s BUYER ACCOUNT FAILED.", actionText);
                case "seller" -> String.format("%s SELLER ACCOUNT FAILED.", actionText);
                case "admin" -> String.format("%s ADMIN ACCOUNT FAILED.", actionText);
                default -> String.format("%s ACCOUNT FAILED.", actionText);
            };

            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    errorMsg,
                    null,
                    e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Retrieve paginated list of admin accounts",
            description = """
                    This endpoint allows an **administrator** to retrieve a paginated list of all admin accounts in the system.  
                    It supports pagination parameters (`page`, `size`) to efficiently navigate large datasets.
                    
                    **Access Control:** Only users with the role `ROLE_ADMIN` are authorized to access this endpoint.
                    
                    **Query Parameters:**
                    - `page` – (optional) The page number to retrieve, default is `0`.
                    - `size` – (optional) The number of records per page, default is `10`.
                    
                    **Response:**
                    - On success: Returns a paginated list of `AdminResponse` objects containing admin details.
                    - On failure: Returns an error message and exception details if the retrieval fails.
                    """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> getAdminList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            Page<Admin> admins = adminService.getAdminList(page, size);
            Page<AdminResponse> responses = admins.map(adminMapper::toDto);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET LIST ADMIN ACCOUNT SUCCESSFULLY.",
                    responses, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET LIST ADMIN ACCOUNT FAILED.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Get admin profile by account ID",
            description = """
        Retrieve the detailed profile information of an admin by their account ID.
        Only users with the role ROLE_ADMIN can access this endpoint.
        
        Example:
        GET /profile/5
        Requires Authorization header with a valid JWT token.
        """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/profile/{accountId}")
    public ResponseEntity<?> getProfile(
            @PathVariable(name = "accountId") long id
    ) {
        try {
            Admin admin = adminService.getAdminProfile(id);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET ADMIN PROFILE SUCCESSFULLY.",
                    adminMapper.toDto(admin), null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "GET ADMIN PROFILE FAILED.",
                    null, e.getMessage()
            ));
        }
    }
}
