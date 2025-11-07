package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.enumerate.SystemWalletStatus;
import Green_trade.green_trade_platform.exception.OrderNotFound;
import Green_trade.green_trade_platform.mapper.*;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.request.CancelOrderRequest;
import Green_trade.green_trade_platform.request.ReviewRequest;
import Green_trade.green_trade_platform.response.*;
import Green_trade.green_trade_platform.service.implement.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/order")
@Slf4j
@AllArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderService;
    private final ResponseMapper responseMapper;
    private final OrderListMapper orderListMapper;
    private final BuyerServiceImpl buyerService;
    private final GhnServiceImpl ghnService;
    private final OrderMapper orderMapper;
    private final ReviewServiceImpl reviewService;
    private final ReviewMapper reviewMapper;
    private final SystemWalletServiceImpl systemWalletService;
    private final ShippingPartnerMapper shippingPartnerMapper;
    private final PaymentMapper paymentMapper;
    private final PostProductMapper postProductMapper;
    private final BuyerMapper buyerMapper;
    private final OrderHistoryMapper orderHistoryMapper;
    private final OrderHistoryListMapper orderHistoryListMapper;

    @Operation(
            summary = "Get order history of current user",
            description = """
                        Retrieves a paginated list of past orders belonging to the currently authenticated buyer.  
                        The system uses the access token to identify the buyer and fetches their order history, 
                        including details such as order ID, total amount, status, and order date.
                    
                        **Workflow:**
                        1. The frontend sends a request with pagination parameters (`page`, `size`).
                        2. The backend identifies the buyer from the JWT token.
                        3. The system retrieves a paginated list of the buyer’s orders, sorted by date (latest first).
                        4. Pagination metadata (current page, total pages, total elements) is included in the response.
                    
                        **Use cases:**
                        - Displaying a user’s order history in their profile dashboard.
                        - Fetching paginated order records for mobile or web apps.
                    
                        **Security Notes:**
                        - Requires authentication via JWT (`ROLE_BUYER`).
                        - Each user can only access their own order history.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @GetMapping("/history")
    public ResponseEntity<RestResponse<OrderHistoryListResponse, Object>> getOrdersHistoryOfCurrentUser(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            log.info(">>> [OrderController] came history");
            Buyer buyer = buyerService.getCurrentUser();
            log.info(">>> [OrderController] get current buyer successfully");
            Page<Order> orderPaging = orderService.getOrdersOfCurrentUserPaging(size, page, buyer);
            log.info(">>> [OrderController] get paging with order successfully");
            Map<String, Object> meta = Map.of(
                    "currentPage", orderPaging.getNumber(),
                    "totalElements", orderPaging.getTotalElements(),
                    "totalPage", orderPaging.getTotalPages()
            );
            log.info(">>> [OrderController] created meta data successfully");

            OrderHistoryListResponse orderHistoryListResponse = orderHistoryListMapper.toDto(orderPaging, meta);


            RestResponse<OrderHistoryListResponse, Object> response = responseMapper.toDto(
                    true,
                    "FETCH ORDER HISTORY SUCCESSFULLY",
                    orderHistoryListResponse,
                    null
            );
            log.info(">>> [OrderController] created response successfully");

            return ResponseEntity.status(HttpStatus.OK.value()).body(response);
        } catch (Exception e) {
            log.info(">>> Error at getOrdersHistoryOfCurrentUser: {}", e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @Operation(
            summary = "Cancel an order",
            description = """
                        Cancels an existing order for the currently authenticated user (buyer).  
                        This API updates the order status in the system and notifies the external GHN shipping service 
                        to cancel the corresponding shipment.
                    
                        **Workflow:**
                        1. The client sends a `POST` request with the order ID in the URL path.
                        2. The system validates that the order exists and belongs to the authenticated user.
                        3. The order status is updated to `CANCELED`.
                        4. The system calls the GHN shipping service API to cancel the shipping request.
                        5. The updated order information is returned in the response.
                    
                        **Use cases:**
                        - Buyers canceling an order before it is shipped.
                        - Sellers or system administrators canceling orders with failed payments or stock issues.
                        - Synchronizing order cancellations between internal system and GHN shipping API.
                    
                        **Security Notes:**
                        - Requires JWT authentication (either `ROLE_BUYER` or `ROLE_SELLER`).
                        - A user can only cancel orders they own.
                    """
    )
    @PostMapping("/cancel/{id}")
    public ResponseEntity<RestResponse<OrderResponse, Object>> cancelOrder(
            @PathVariable Long id,
            @RequestBody CancelOrderRequest request
    ) throws Exception {
        log.info(">>> [OrderController] came cancelOrder");
        Order canceledOrder = orderService.cancelOrder(id, request);
        log.info(">>> [OrderController] cancelOrder pass");
        systemWalletService.updateEscrowRecordStatus(canceledOrder.getSystemWallet(), SystemWalletStatus.REFUNDED);
        log.info(">>> [OrderController] update system wallet status successfully");
        ghnService.createCancelOrderShippingServiceResponseToDto(canceledOrder.getOrderCode(), canceledOrder.getPostProduct().getSeller().getGhnShopId());
        log.info(">>> [OrderController] cancel order ghn successfully");
        OrderResponse responseData = orderMapper.toDto(canceledOrder);
        log.info(">>> [OrderController] created responseData successfully");
        RestResponse<OrderResponse, Object> response = responseMapper.toDto(
                true,
                "CANCELED ORDER SUCCESSFULLY",
                responseData,
                null
        );
        log.info(">>> [OrderController] created response successfully");
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @Operation(
            summary = "Create a product review with optional images",
            description = """
                    This endpoint allows customers to create a review for an electrical product they have purchased.
                    
                    The request should include:
                    - **Review details** (order ID, rating, feedback text) as a JSON object named `request`.
                    - **Optional product images** (photos of the product or proof of use) as `pictures`.
                    
                    The API automatically checks the feedback text for inappropriate or offensive language (Vietnamese supported).
                    Uploaded images will be stored on Cloudinary and associated with the review record.
                    
                    **Content type:** multipart/form-data  
                    **Authentication:** Required if the platform uses user accounts.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @PostMapping(
            value = "/review",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createReview(@ModelAttribute ReviewRequest request,
                                          @RequestPart(name = "pictures", required = false) List<MultipartFile> reviewImages) {
        log.info(">>> [Order Controller] Create Review: Started.");
        log.info(">>> [Order Controller] Request: {}.", request);
        try {
            Review savedReview = reviewService.createReview(request, reviewImages);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "MAKE REVIEW SUCCESSFULLY.",
                    reviewMapper.toDto(savedReview), null));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "MAKE REVIEW FAILED.",
                    null, e.getMessage()));
        }
    }

    @Operation(
            summary = "Get all reviews by order ID",
            description = """
                    This endpoint allows an authenticated user (buyer or seller) to retrieve all reviews 
                    associated with a specific order.
                    
                    - The `orderId` parameter must correspond to an existing order.
                    - Each review may include its rating, feedback text, and attached review images.
                    """
    )
    @GetMapping("/get-review/{orderId}")
    public ResponseEntity<?> getReviewByOrderId(@PathVariable(name = "orderId") long id) {
        try {
            Review reviews = reviewService.getReviewsByOrderId(id);
            ReviewResponse response = reviewMapper.toDto(reviews);

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET REVIEWS BY ORDER SUCCESSFULLY.",
                    response,
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "GET REVIEWS BY ORDER FAILED.",
                    null,
                    e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Get payment information by order ID",
            description = """
                    This endpoint retrieves the payment details associated with a specific order. 
                    It returns the payment ID, description, and gateway name linked to that order.
                    If the order or transaction does not exist, an error message will be returned.
                    """
    )
    @GetMapping("/payment/{orderId}")
    public ResponseEntity<?> getPayment(@PathVariable(name = "orderId") long id) {
        log.info(">>> [Order Controller] Get payment: Started.");
        try {
            Transaction transaction = orderService.getTransactionByOrderId(id);
            log.info(">>> [Order Controller] Get transaction: {}", transaction);
            Payment payment = transaction.getPayment();
            log.info(">>> [Order Controller] Get payment: {}", payment);
            Map<String, Object> data = new HashMap<>();
            data.put("id", payment.getId());
            data.put("description", payment.getDescription());
            data.put("gatewayName", payment.getGatewayName());
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET ORDER PAYMENT SUCCESSFULLY.",
                    data, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "GET ORDER PAYMENT FAILED.",
                    null, e.getMessage()
            ));
        }

    }

    @GetMapping("/{orderId}")
    public ResponseEntity<RestResponse<Map<String, Object>, Object>> getOrderDetailByOrderId(@PathVariable Long orderId) {
        Order foundOrder = orderService.getOrderById(orderId);
        if (foundOrder == null) {
            throw new OrderNotFound();
        }

        OrderResponse orderResponse = orderMapper.toDto(foundOrder);
        PaymentResponse paymentResponse = paymentMapper.toDto(foundOrder.getTransactions().getLast().getPayment());
        ShippingPartnerResponse shippingPartnerResponse = shippingPartnerMapper.toDto(foundOrder.getShippingPartner());
        PostProductResponse productResponse = postProductMapper.toDto(foundOrder.getPostProduct());
        BuyerResponse buyerResponse = buyerMapper.toDto(foundOrder.getBuyer());


        Map<String, Object> responseData = Map.of(
                "order", orderResponse,
                "product", productResponse,
                "buyer", buyerResponse,
                "payment", paymentResponse,
                "shippingPartner", shippingPartnerResponse
        );

        RestResponse<Map<String, Object>, Object> response = responseMapper.toDto(
                true,
                "FETCH ORDER DETAIL SUCCESSFULLY",
                responseData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @Operation(
            summary = "Get all orders of current seller",
            description = """
                    Retrieve all orders associated with the current logged-in seller.
                    The result is paginated using 'page' and 'size' query parameters.
                    
                    Requirements:
                    - User must have ROLE_SELLER
                    - Authorization header with a valid JWT token is required
                    
                    Example:
                    GET /api/orders?page=0&size=10
                    """
    )
    @PreAuthorize("hasRole('ROLE_SELLER')")
    @GetMapping("")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            Seller seller = buyerService.getCurrentUser().getSeller();
            Page<Order> orders = orderService.getAllOrders(page, size, seller);
            Page<OrderResponse> orderResponses = orders.map(orderMapper::toDto);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET ALL ORDER SUCCESSFULLY.",
                    orderResponses, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET ALL ORDER FAILED.",
                    null, e.getMessage()
            ));
        }
    }
}
