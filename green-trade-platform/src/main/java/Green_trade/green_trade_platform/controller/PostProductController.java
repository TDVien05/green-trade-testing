package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.exception.PostProductNotFound;
import Green_trade.green_trade_platform.exception.SubscriptionExpiredException;
import Green_trade.green_trade_platform.mapper.PostProductListMapper;
import Green_trade.green_trade_platform.mapper.PostProductMapper;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.mapper.SellerMapper;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.request.UpdatePostProductRequest;
import Green_trade.green_trade_platform.response.PostProductListResponse;
import Green_trade.green_trade_platform.response.PostProductResponse;
import Green_trade.green_trade_platform.response.RestResponse;
import Green_trade.green_trade_platform.response.SellerResponse;
import Green_trade.green_trade_platform.service.implement.PostProductServiceImpl;
import Green_trade.green_trade_platform.service.implement.SellerServiceImpl;
import Green_trade.green_trade_platform.service.implement.SubscriptionServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/post-product")
@RequiredArgsConstructor
public class PostProductController {
    private final PostProductServiceImpl postProductService;
    private final ResponseMapper responseMapper;
    private final PostProductListMapper postProductListMapper;
    private final PostProductMapper postProductMapper;
    private final SellerMapper sellerMapper;
    private final SellerServiceImpl sellerService;

    @Operation(
            summary = "Get all available product posts with pagination and sorting",
            description = """
                    This endpoint retrieves a paginated list of all product posts that are currently available for purchase
                    (i.e., not sold yet). It is typically used on the product listing page of the buyer interface.
                    
                    **Usage:**
                    - When a buyer navigates to the product listing page, the frontend should send the `page` and `size` parameters to the backend.
                    - The backend returns a paginated response containing product details (title, brand, model, price, etc.).
                    - By default, results are **sorted by creation date in descending order**, so the newest products appear first.
                    
                    **Query Parameters:**
                    - `page` *(integer, optional)* — Index of the page to retrieve (0-based). Default value is **0**.
                    - `size` *(integer, optional)* — Number of products per page. Default value is **10**.
                    - `sort` *(string, optional)* — Field to sort by (default: `"createdAt"`). Can be combined with direction (e.g., `"price,asc"` or `"price,desc"`).
                    
                    **Filters (optional):**
                    - Future enhancements may include filters by category, price range, brand, or location.
                    - Example: `/api/posts?page=1&size=12&category=Electronics&minPrice=100&maxPrice=500`
                    
                    **Example Request:**
                    GET /api/posts?page=0&size=10
                    
                    
                    **Authentication:** Not required for browsing public product listings.
                    """
    )
    @GetMapping("")
    public ResponseEntity<RestResponse<PostProductListResponse, Object>> getAllProduct(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort_by", defaultValue = "id") String sortedBy,
            @RequestParam(name = "is_asc", defaultValue = "true") boolean isAsc
    ) {
        try {
            Page<PostProduct> postProductPage = postProductService.getAllProductPaging(page, size, sortedBy, isAsc);
            Map<String, Object> meta = Map.of(
                    "currentPage", postProductPage.getNumber(),
                    "totalElements", postProductPage.getTotalElements(),
                    "totalPage", postProductPage.getTotalPages()
            );

            PostProductListResponse responseData = postProductListMapper.toDto(postProductPage.getContent(), meta);

            RestResponse<PostProductListResponse, Object> response = responseMapper.toDto(
                    true,
                    "Get post product successfully.",
                    responseData,
                    null
            );
            return ResponseEntity.status(HttpStatus.OK.value()).body(response);
        } catch (Exception e) {
            PostProductListResponse responseData = postProductListMapper.toDto(new ArrayList<PostProduct>(), Map.of());
            RestResponse<PostProductListResponse, Object> response = responseMapper.toDto(
                    false,
                    "Get post product failed.",
                    responseData,
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(response);
        }
    }

    @Operation(
            summary = "Get seller information by post product ID",
            description = """
                        Retrieves detailed information about the seller associated with a specific post product.  
                        The client provides a `postId`, and the system returns the corresponding seller’s profile and business details.
                    
                        **Workflow:**
                        1. The system locates the post product using the provided `postId`.
                        2. If found, it retrieves the seller linked to that post.
                        3. The endpoint returns seller details such as store name, contact info, and verification status.
                        4. If the post product is not found, a `404` error is thrown.
                    
                        **Use cases:**
                        - Displaying seller information on a product detail page.
                        - Showing seller ratings, verification status, or contact details alongside their listings.
                        - Allowing buyers to view who owns a particular product post.
                    
                        **Security Notes:**
                        - This endpoint may be publicly accessible or protected depending on platform policy.
                        - Sensitive seller data (like private contact info) should only be returned to authorized users.
                    """
    )
    @GetMapping("/{postId}/seller")
    public ResponseEntity<RestResponse<SellerResponse, Object>> getSellerByPostId(@PathVariable(name = "postId") Long id) {
        PostProduct postProduct = postProductService.findPostProductById(id);
        if (postProduct == null) {
            throw new PostProductNotFound();
        }
        Seller seller = postProduct.getSeller();
        SellerResponse responseData = sellerMapper.toDto(seller);
        RestResponse<SellerResponse, Object> response = responseMapper.toDto(
                true,
                "FETCH SELLER BY POST SUCCESSFULLY",
                responseData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }


    @Operation(
            summary = "Hide or unhide a post product by ID",
            description = """
                        Hides (deactivates) a specific post product from the platform by setting its `active` status to `false`.  
                        The product remains stored in the database but will no longer be visible to buyers or appear in public listings.
                    
                        **Workflow:**
                        1. The client sends a request with the `postId` of the product to hide.
                        2. The system verifies ownership or admin privileges.
                        3. The product’s `active` flag is updated to `false`.
                        4. A confirmation response is returned with the updated product details.
                    
                        **Use cases:**
                        - **Seller:** Temporarily hides a product that is out of stock or under maintenance.
                        - **Admin:** Moderates or disables posts violating policies.
                        - **Buyer (optional):** Typically not allowed; only for viewing hidden-state results if permitted.
                    
                        **Security Notes:**
                        - Requires authentication via JWT.
                        - Accessible to roles: `ROLE_SELLER`, `ROLE_ADMIN`.
                        - The request is **idempotent** — hiding an already hidden product returns the same result.
                    """
    )
    @PreAuthorize("hasRole('ROLE_SELLER')")
    @PostMapping("/hide/{postId}")
    public ResponseEntity<?> hidePostProduct(@PathVariable(name = "postId") Long id,
                                             @RequestParam(name = "is_hide") boolean isHide) {
        String hide = isHide ? "HIDE" : "FALSE";
        try {
            PostProduct temp = postProductService.hidePostProduct(id, isHide);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    hide + " PRODUCT SUCCESSFULLY.",
                    postProductMapper.toDto(temp), null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    false,
                    hide + " PRODUCT SUCCESSFULLY.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Get post product information based on a wish-list ID",
            description = """
                    This endpoint allows an authenticated **buyer** or **seller** to retrieve the detailed information 
                    of a product post (`PostProduct`) that is associated with a specific **wish-list item**.
                    
                    - The `wishId` parameter must correspond to an existing wish-list record.
                    - The system will automatically fetch the `PostProduct` linked to that wish-list entry.
                    - This endpoint is accessible to both buyers and sellers.
                    
                    **Use case:**  
                    Buyers can use this API to quickly view the details of an item they have added to their wish-list,  
                    and sellers can use it to verify which of their posts are currently in wish lists of buyers.
                    """
    )
    @PreAuthorize("hasAnyRole('ROLE_SELLER', 'ROLE_BUYER')")
    @GetMapping("/{wishId}")
    public ResponseEntity<?> getPostInfoByWishId(@PathVariable(name = "wishId") long id) {
        try {
            PostProduct postProduct = postProductService.findPostByWishId(id);
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET POST PRODUCT INFORMATION SUCCESSFULLY.",
                    postProductMapper.toDto(postProduct), null
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(responseMapper.toDto(
                    true,
                    "GET POST PRODUCT INFORMATION FAILED.",
                    null, e.getMessage()
            ));
        }
    }

    @Operation(
            summary = "Update an existing post product",
            description = """
                    Allows a seller to update details of an existing product post.
                    Only users with the role **ROLE_SELLER** can perform this operation.
                    The post cannot be modified if the product has already been sold.
                    """,
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Post product updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RestResponse.class)
                            )
                    )
            }
    )
    @PreAuthorize("hasRole('ROLE_SELLER')")
    @PutMapping("/{postId}")
    public ResponseEntity<RestResponse<PostProductResponse, Object>> updatePostProduct(
            @PathVariable Long postId,
            @Valid @ModelAttribute UpdatePostProductRequest request,
            @RequestPart("pictures") List<MultipartFile> files
    ) throws Exception {
        try {
            PostProduct foundPostProduct = postProductService.findPostProductById(postId);
            if (foundPostProduct == null) {
                throw new PostProductNotFound();
            }

            if (foundPostProduct.isSold()) {
                throw new Exception("Sold product's post cannot be changed the content");
            }

            PostProduct updatedPostProduct = postProductService.updatePostProduct(foundPostProduct, request, files);

            PostProductResponse responseData = postProductMapper.toDto(updatedPostProduct);

            RestResponse<PostProductResponse, Object> response = responseMapper.toDto(
                    true,
                    "UPDATED POST PRODUCT SUCCESSFULLY",
                    responseData,
                    null
            );

            return ResponseEntity.status(HttpStatus.OK.value()).body(response);
        } catch (Exception e) {
            log.info(">>> [PostProductController] error at updatePostProduct: {}", e.getMessage());
            throw e;
        }
    }

}
