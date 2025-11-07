package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.enumerate.WishListPriority;
import Green_trade.green_trade_platform.exception.PaymentMethodNotSupportedException;
import Green_trade.green_trade_platform.exception.PostProductNotFound;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.exception.SelfPurchaseNotAllowedException;
import Green_trade.green_trade_platform.mapper.*;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.*;
import Green_trade.green_trade_platform.request.PlaceOrderRequest;
import Green_trade.green_trade_platform.request.ProfileRequest;
import Green_trade.green_trade_platform.request.UpdateBuyerProfileRequest;
import Green_trade.green_trade_platform.request.WishListRequest;
import Green_trade.green_trade_platform.response.*;
import Green_trade.green_trade_platform.service.implement.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/buyer")
@Slf4j
public class BuyerController {
    private final BuyerServiceImpl buyerService;
    private final ResponseMapper responseMapper;
    private final BuyerMapper buyerMapper;
    private final WalletMapper walletMapper;
    private final PaymentRepository paymentRepository;
    private final TransactionServiceImpl transactionService;
    private final OrderMapper orderMapper;
    private final GhnServiceImpl ghnService;
    private final BuyerRepository buyerRepository;
    private final PostProductRepository postProductRepository;
    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final OrderServiceImpl orderService;
    private final PostProductServiceImpl postProductService;
    private final PaymentServiceImpl paymentService;
    private final SystemWalletServiceImpl systemWalletService;
    private final WalletServiceImpl walletService;
    private final WishListMapper wishListMapper;
    private final WishListingServiceImpl wishListingService;
    private final InvoiceServiceImpl invoiceService;

    public BuyerController(
            BuyerServiceImpl buyerService,
            ResponseMapper responseMapper,
            BuyerMapper buyerMapper,
            WalletMapper walletMapper,
            PaymentRepository paymentRepository,
            TransactionServiceImpl transactionService,
            OrderMapper orderMapper,
            GhnServiceImpl ghnService,
            BuyerRepository buyerRepository,
            PostProductRepository postProductRepository,
            TransactionRepository transactionRepository,
            OrderRepository orderRepository,
            OrderServiceImpl orderService,
            PostProductServiceImpl postProductService,
            PaymentServiceImpl paymentService,
            SystemWalletServiceImpl systemWalletService,
            WalletServiceImpl walletService,
            WishListMapper wishListMapper,
            WishListingServiceImpl wishListingService,
            InvoiceServiceImpl invoiceService) {
        this.buyerService = buyerService;
        this.responseMapper = responseMapper;
        this.buyerMapper = buyerMapper;
        this.walletMapper = walletMapper;
        this.paymentRepository = paymentRepository;
        this.transactionService = transactionService;
        this.orderMapper = orderMapper;
        this.ghnService = ghnService;
        this.buyerRepository = buyerRepository;
        this.postProductRepository = postProductRepository;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.postProductService = postProductService;
        this.paymentService = paymentService;
        this.systemWalletService = systemWalletService;
        this.walletService = walletService;
        this.wishListMapper = wishListMapper;
        this.wishListingService = wishListingService;
        this.invoiceService = invoiceService;
    }

    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @Operation(
            summary = "Upload buyer profile",
            description = """
                        Allows a buyer to upload or update their profile information including full name,
                        shipping address, contact details, and avatar image.  
                        This endpoint accepts multipart form data containing both profile fields and an image file.
                    
                        **Workflow:**
                        1. The buyer submits profile data (name, address, etc.) and an avatar image via multipart form.
                        2. The system uploads the avatar file, updates the buyer's profile in the database, 
                           and returns the updated profile data.
                        3. Only authenticated buyers (ROLE_BUYER) can access this endpoint.
                    
                        **Use cases:**
                        - Buyers updating their account profile for the first time.
                        - Allowing users to change their avatar or update shipping address information.
                    
                        **Security Notes:**
                        - Requires valid JWT token with `ROLE_BUYER` authority.
                        - The uploaded image must comply with allowed size and format restrictions.
                    """
    )
    @PostMapping(
            value = "/upload-profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadBuyerProfile(@Parameter(description = "profile request for buyer")
                                                @Valid @ModelAttribute ProfileRequest profileRequest,
                                                @Parameter(description = "avatar of buyer")
                                                @RequestPart(value = "avatar_url", required = true) MultipartFile avatarFile) throws IOException {
        Map<String, Object> body = buyerService.uploadBuyerProfile(profileRequest, avatarFile);
        Buyer tempProfile = (Buyer) body.get("profile");
        return ResponseEntity.ok(responseMapper.toDto(
                true,
                "UPLOAD PROFILE SUCCESS.",
                buyerMapper.toDto(tempProfile),
                null));
    }

    @Operation(
            summary = "Update Buyer Profile",
            description = """
                        Allows a buyer to update their existing profile information, including full name, 
                        contact details, shipping address, and optionally their avatar image.  
                        This endpoint accepts multipart/form-data requests where both text fields and a file may be included.
                    
                        **Workflow:**
                        1. The buyer submits updated profile details and, optionally, a new avatar image.
                        2. The system updates the corresponding fields in the buyer’s profile.
                        3. If a new avatar is provided, the image is uploaded and replaces the previous one.
                        4. The response returns the updated buyer profile information.
                    
                        **Use cases:**
                        - Buyers updating their personal information such as name, phone number, or address.
                        - Changing or removing an avatar profile picture.
                    
                        **Security Notes:**
                        - Requires authentication via JWT token (ROLE_BUYER).
                        - Only the owner of the account can update their own profile.
                    """
    )
    @PutMapping(
            value = "/update-profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    public ResponseEntity<RestResponse<BuyerResponse, Object>> updateProfile(
            @Valid @ModelAttribute UpdateBuyerProfileRequest updateProfileRequest,
            @RequestPart(value = "avatar_url", required = false) MultipartFile avatarFile
    ) throws Exception {
        log.info(">>> Passed came updateProfile API");
        log.info(">>> updateProfileRequest: {}", updateProfileRequest);
        log.info(">>> avatarFile: {}", avatarFile);

        Buyer buyer = buyerService.updateProfile(updateProfileRequest, avatarFile);
        BuyerResponse responseData = buyerMapper.toDto(buyer);

        return ResponseEntity.status(HttpStatus.OK.value()).body(
                responseMapper.toDto(
                        true,
                        "UPDATED PROFILE SUCCESSFULLY",
                        responseData,
                        null
                )
        );
    }

    @Operation(
            summary = "Get buyer profile",
            description = """
                        Retrieves the profile information of the currently authenticated buyer.  
                        The client must include a valid JWT access token in the `Authorization` header.  
                        The system will decode the token, identify the buyer, and return their corresponding profile details.
                    
                        **Workflow:**
                        1. The client sends a `GET /profile` request with an Authorization header:  
                           `Authorization: Bearer <access_token>`
                        2. The system validates the access token.
                        3. The system identifies the buyer associated with the token.
                        4. The buyer’s profile is fetched and returned as a response.
                    
                        **Use cases:**
                        - Retrieving current logged-in buyer’s profile for display in their dashboard.
                        - Ensuring front-end applications can show user-specific information without manually passing user IDs.
                    
                        **Security Notes:**
                        - Requires a valid access token (`ROLE_BUYER`).
                        - Access is limited to the authenticated buyer’s own profile.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @GetMapping("/profile")
    public ResponseEntity<RestResponse<Object, Object>> getProfile() {
        try {
            Buyer buyer = buyerService.getCurrentUser();
            return ResponseEntity.ok(responseMapper.toDto(true,
                    "Get user profile successfully.",
                    buyerMapper.toDto(buyer),
                    null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(responseMapper.toDto(false,
                    "Error occur during get user profile.",
                    null, e));
        }
    }

    @Operation(
            summary = "Get user wallet.",
            description = "Front-end put access token in the header request. " +
                    "Back-end will give user's wallet information."
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @GetMapping("/wallet")
    public ResponseEntity<RestResponse<Object, Object>> getWallet() {
        try {
            Wallet wallet = buyerService.getWallet();
            return ResponseEntity.ok(responseMapper.toDto(true,
                    "Get wallet's information successfully.",
                    walletMapper.toDto(wallet), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(responseMapper.toDto(false,
                    "Get wallet information failed.",
                    null, e));
        }
    }

    @Operation(
            summary = "Place a new order",
            description = """
                    This endpoint allows a buyer to place a new order in the Green Trade platform.
                    <br><br>
                    Workflow:
                    <ul>
                        <li>Validate the payment method and buyer information.</li>
                        <li>Fetch the product and verify its availability.</li>
                        <li>Reject the request if the buyer attempts to purchase their own product.</li>
                        <li>Calculate the shipping fee through the GHN API (depending on COD or online payment).</li>
                        <li>Create a new order, transaction, and GHN shipping order.</li>
                        <li>Return a response containing the new order details and GHN order code.</li>
                    </ul>
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @PostMapping("/place-order")
    public ResponseEntity<RestResponse<OrderResponse, Object>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) throws Exception {
        Order newOrder = null;
        RestResponse<OrderResponse, Object> response = null;
        OrderResponse responseData = null;
        String shippingFee = "0";
        try {
            log.info(">>> [START] placeOrder");

            log.info(">>> Fetch payment");
            Payment payment = paymentService.findPaymentMethodById(request.getPaymentId());
            if (payment == null) {
                throw new PaymentMethodNotSupportedException();
            }

            log.info(">>> Fetch buyer");
            Buyer buyer = buyerService.findBuyerByUsername(request.getUsername());
            if (buyer == null) {
                throw new ProfileException("Buyer with Username: " + request.getUsername() + "is not existed");
            }

            log.info(">>> Fetch post product");
            PostProduct postProduct = postProductService.findPostProductById(request.getPostProductId());
            if (postProduct == null) {
                throw new PostProductNotFound();
            }

            if (buyer.getBuyerId() == postProduct.getSeller().getBuyer().getBuyerId()) {
                throw new SelfPurchaseNotAllowedException();
            }

            log.info(">>> Calculate shipping fee");
            if (payment.getGatewayName().equals("COD")) {
                log.info(">>> Calculate shipping fee COD");
                shippingFee = ghnService.getShippingFeeDto(buyer, postProduct.getSeller(), postProduct, postProduct.getPrice().intValue()).get("service_fee");
            } else {
                log.info(">>> Calculate shipping fee Online Payment");
                shippingFee = ghnService.getShippingFeeDto(buyer, postProduct.getSeller(), postProduct, 0).get("service_fee");
            }

            log.info(">>> Place new order");
            newOrder = buyerService.placeOrder(request, shippingFee);

            if ("COD".equalsIgnoreCase(payment.getGatewayName())) {
                log.info(">>> COD payment flow");
                Transaction transaction = transactionService.checkoutCODPayment(
                        request.getUsername(),
                        request.getPostProductId(),
                        request.getPaymentId(),
                        newOrder
                );
                List<Transaction> transactions = transactionService.getTransactionsOfOrder(newOrder);
                log.info(">>> Passed get transactions");

                newOrder = orderService.updateOrderTransactions(newOrder, transactions);
                log.info(">>> Passed update transactions");

                String orderShippingCode = ghnService.createOrderShippingResponseToDto(
                        newOrder, transactionRepository.findAllByOrder(newOrder).getLast().getPayment()
                ).get("orderCode");

                String totalServiceFee = ghnService.createOrderShippingResponseToDto(
                        newOrder, transactionRepository.findAllByOrder(newOrder).getLast().getPayment()
                ).get("totalFee");
                log.info(">>> Passed get orderShippingCode");
                log.info(">>> orderShippingCode: {}", orderShippingCode);

                newOrder = orderService.updateOrderCode(orderShippingCode, newOrder);
                log.info(">>> Passed set Order Code");
                SystemWallet systemWallet = systemWalletService.createEscrowRecordAfterReduceFeeCOD(newOrder, totalServiceFee);
                newOrder = orderService.updateSystemWallet(systemWallet, newOrder);
            } else {
                log.info(">>> Wallet payment flow");
                Transaction transaction = transactionService.checkoutWalletPayment(
                        request.getUsername(),
                        request.getPostProductId(),
                        request.getPaymentId(),
                        newOrder
                );
                List<Transaction> transactions = transactionService.getTransactionsOfOrder(newOrder);
                log.info(">>> Passed get transactions");

                newOrder = orderService.updateOrderTransactions(newOrder, transactions);
                log.info(">>> Passed update transactions for order");

                newOrder = orderService.updateOrderStatus(newOrder, OrderStatus.PAID);
                log.info(">>> Passed update order status");

                String orderShippingCode = ghnService.createOrderShippingResponseToDto(
                        newOrder, transactionRepository.findAllByOrder(newOrder).getLast().getPayment()).get("orderCode");
                log.info(">>> Passed get orderShippingCode: {}", orderShippingCode);
                String totalServiceFee = ghnService.createOrderShippingResponseToDto(
                        newOrder, transactionRepository.findAllByOrder(newOrder).getLast().getPayment()
                ).get("totalFee");

                newOrder = orderService.updateOrderCode(orderShippingCode, newOrder);
                log.info(">>> Passed set Order Code");
                SystemWallet systemWallet = systemWalletService.createEscrowRecordAfterReduceFeeWalletPayment(newOrder, totalServiceFee);
                newOrder = orderService.updateSystemWallet(systemWallet, newOrder);
            }
            Invoice newInvoice = invoiceService.createInvoiceInstance(newOrder, "", 0);
            invoiceService.generateInvoice(newInvoice.getId());
            responseData = orderMapper.toDto(newOrder);
            log.info(">>> Passed created response");

            postProductService.updateSoldStatus(true, postProduct);

            log.info(">>> Build response");
            response = responseMapper.toDto(
                    true,
                    "PLACE ORDERED SUCCESS",
                    responseData,
                    null
            );

            log.info(">>> [END] placeOrder success");
            return ResponseEntity.status(HttpStatus.OK.value()).body(response);
        } catch (Exception e) {
//            newOrder.getPostProduct().setSold(false);
            throw e;
        }
    }

    @Operation(
            summary = "Get wallet transaction history",
            description = """
                        Retrieves a paginated list of wallet transactions for the currently authenticated user.
                        This endpoint supports pagination through 'page' and 'size' query parameters.
                        Each record in the result includes transaction details such as:
                        - Transaction ID
                        - Type (credit/debit)
                        - Amount
                        - Status
                        - Timestamp
                        - ...
                        Use this API to display a user's transaction history in their dashboard or account page.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @GetMapping("/transaction-history")
    public ResponseEntity<?> getWalletTransactionHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            Buyer buyer = buyerService.getCurrentUser();
            Page<WalletTransaction> transactions = walletService.getTransactionHistory(buyer, page, size);

            Page<WalletTransactionResponse> responsePage = transactions.map(walletMapper::toTransactionResponse);

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET ALL WALLET TRANSACTION SUCCESSFULLY.",
                    responsePage, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "GET ALL WALLET TRANSACTION SUCCESSFULLY.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Add a product post to the buyer's wish-list",
            description = """
                    This endpoint allows an authenticated **buyer** to add a product post 
                    (`PostProduct`) to their personal wish-list.
                    
                    - The buyer must be logged in.
                    - The product (`PostProduct`) must exist and be active.
                    - A seller **cannot** add their own product to their own wish-list (for fairness and data integrity).
                    - If the product is already in the buyer's wish-list, the service may prevent duplication or update the record, depending on business logic.
                    
                    **Use case:**  
                    Buyers use this API to save or bookmark products they are interested in purchasing later.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @PostMapping("/wish-list")
    public ResponseEntity<?> addProductToWishList(@RequestBody WishListRequest request) {
        log.info(">>> [Buyer Controller] Add product to wish list: Started.");
        try {
            Buyer buyer = buyerService.getCurrentUser();
            log.info(">>> [Buyer Controller] Buyer info: {}", buyer.getUsername());

            log.info(">>> [Buyer Controller] Post product id: {}", request.getPostId());
            PostProduct postProduct = postProductService.getPostProductById(request.getPostId());
            log.info(">>> [Buyer Controller] Post product: {}", postProduct);

            if (buyer.getSeller() == postProduct.getSeller()) {
                throw new IllegalArgumentException("Seller can not add your product into your wish-listing.");
            }

            WishListing wishListing = wishListMapper.toEntity(request, buyer, postProduct);

            WishListing savedWishList = wishListingService.addWishList(wishListing);

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "ADD PRODUCT TO WISH LISTING SUCCESSFULLY.",
                    wishListMapper.toDto(savedWishList), null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "ADD PRODUCT TO WISH LISTING FAILED.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Remove a product from the buyer's wish list",
            description = """
                    This endpoint allows an authenticated **buyer** to remove a product post 
                    from their personal wish list.
                    
                    - The `wishId` must correspond to an existing wish-list entry.
                    - The buyer must own the wish-list entry; otherwise, access will be denied.
                    - If the wish-list item does not exist or has already been removed, the API will return an appropriate error message.
                    
                    **Use case:**  
                    Buyers use this endpoint when they no longer wish to keep a product in their saved wish-list.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_BUYER', 'ROLE_SELLER')")
    @PostMapping("/remove-wish-list/{wishId}")
    public ResponseEntity<?> removeWishList(@PathVariable(name = "wishId") long id) {
        try {
            wishListingService.removePostProduct(id);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "REMOVE POST PRODUCT FROM WISH LIST SUCCESSFULLY.",
                    null, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "REMOVE POST PRODUCT FROM WISH LIST FAILED.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Retrieve the buyer's wish list",
            description = """
                    This endpoint returns a paginated list of the buyer's wish-list items.
                    
                    - The buyer must be logged in.
                    - Results can be optionally filtered by **priority** (e.g., HIGH, MEDIUM, LOW).
                    - If no priority is specified, all wish-list items are returned.
                    - Supports pagination via `page` and `size` parameters.
                    
                    **Use case:**  
                    Buyers use this endpoint to view and manage the list of product posts they have added to their wish-list.
                    """
    )
    @GetMapping("/wish-list")
    public ResponseEntity<?> getWishList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "priority", required = false) WishListPriority priority
    ) {
        try {
            Buyer buyer = buyerService.getCurrentUser();
            Page<WishListing> wishListings = wishListingService.getWishList(buyer, page, size, priority);
            Page<WishListingResponse> mapped = wishListings.map(wishListMapper::toDto);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("content", mapped.getContent());
            data.put("pageNumber", mapped.getNumber());
            data.put("pageSize", mapped.getSize());
            data.put("totalElements", mapped.getTotalElements());
            data.put("totalPages", mapped.getTotalPages());
            data.put("first", mapped.isFirst());
            data.put("last", mapped.isLast());
            data.put("hasNext", mapped.hasNext());
            data.put("hasPrevious", mapped.hasPrevious());

            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET WISH LIST SUCCESSFULLY.",
                    data,
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "GET WISH LIST FAILED.",
                    null,
                    e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Retrieve paginated list of buyers",
            description = """
                    This endpoint allows an **administrator** to retrieve a paginated list of all registered buyers in the system. 
                    The request supports pagination parameters (`page`, `size`) and returns a structured response containing 
                    buyer information and metadata.
                    
                    **Access Control:** Only users with the role `ROLE_ADMIN` can access this endpoint.
                    
                    **Response:**
                    - On success: returns a paginated list of `BuyerResponse` objects with a success message.
                    - On failure: returns an error response with failure status and message details.
                    """
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> getBuyerList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            Page<Buyer> listBuyer = buyerService.getListBuyers(page, size);
            Page<BuyerResponse> response = listBuyer.map(buyerMapper::toDto);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET LIST BUYERS SUCCESSFULLY.",
                    response, null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    "GET LIST BUYERS FAILED.",
                    null, e.getMessage()
            ));
        }
    }
}
