package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.exception.OrderNotFound;
import Green_trade.green_trade_platform.exception.PaymentMethodNotSupportedException;
import Green_trade.green_trade_platform.exception.PostProductNotFound;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.repository.PostProductRepository;
import Green_trade.green_trade_platform.request.ShippingFeeRequest;
import Green_trade.green_trade_platform.response.RestResponse;
import Green_trade.green_trade_platform.service.PostProductService;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.GhnServiceImpl;
import Green_trade.green_trade_platform.service.implement.OrderServiceImpl;
import Green_trade.green_trade_platform.service.implement.PaymentServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shipping")
@Slf4j
public class ShippingServiceController {

    private final GhnServiceImpl ghnService;
    private final ResponseMapper responseMapper;
    private final OrderRepository orderRepository;
    private final PostProductService postProductService;
    private final PostProductRepository postProductRepository;
    private final BuyerServiceImpl buyerService;
    private final PaymentServiceImpl paymentService;
    private final OrderServiceImpl orderService;

    public ShippingServiceController(
            GhnServiceImpl ghnService,
            ResponseMapper responseMapper,
            OrderRepository orderRepository,
            PostProductService postProductService,
            PostProductRepository postProductRepository,
            BuyerServiceImpl buyerService,
            PaymentServiceImpl paymentService,
            OrderServiceImpl orderService
    ) {
        this.ghnService = ghnService;
        this.responseMapper = responseMapper;
        this.orderRepository = orderRepository;
        this.postProductService = postProductService;
        this.postProductRepository = postProductRepository;
        this.buyerService = buyerService;
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @Operation(
            summary = "Fetch list of provinces",
            description = """
                        Retrieves a list of provinces available from the GHN (Giao Hàng Nhanh) shipping service.  
                        This data is often used to populate province dropdowns during address creation or checkout.
                    
                        **Workflow:**
                        1. The system calls the GHN API to retrieve the list of supported provinces.
                        2. The response is mapped into a key-value structure (province code → province name).
                        3. The endpoint returns a JSON object containing all provinces supported for shipping.
                    
                        **Use cases:**
                        - Displaying a list of provinces when users fill out shipping or billing addresses.
                        - Fetching location data dynamically from the GHN logistics API.
                    """
    )
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() throws JsonProcessingException {
        Map<String, String> provincesMap = new HashMap<>();
        provincesMap = ghnService.getProvinceList();
        RestResponse response = responseMapper.toDto(
                true,
                "FETCH PROVINCES SUCCESSFULLY",
                provincesMap,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @Operation(
            summary = "Fetch list of districts by province ID",
            description = """
                        Retrieves a list of districts for a specified province using the GHN (Giao Hàng Nhanh) shipping service.  
                        This endpoint is typically used when the user selects a province, and the frontend needs to load 
                        all districts under that province dynamically.
                    
                        **Workflow:**
                        1. The client provides a `provinceId` as a request parameter.
                        2. The system sends a request to the GHN API to fetch districts belonging to that province.
                        3. The districts are mapped into a key-value format (district code → district name).
                        4. The endpoint returns the resulting district list.
                    
                        **Use cases:**
                        - Displaying available districts when users select a province during checkout or address creation.
                        - Dynamically populating location dropdowns in registration or shipping forms.
                    """
    )
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(
            @RequestParam int provinceId
    ) throws JsonProcessingException {
        Map<String, String> districtsMap = new HashMap<>();
        districtsMap = ghnService.getDistrictListByProvinceId(provinceId);
        RestResponse response = responseMapper.toDto(
                true,
                "FETCH DISTRICTS SUCCESSFULLY",
                districtsMap,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @Operation(
            summary = "Fetch list of wards by district ID",
            description = """
                        Retrieves a list of wards (subdistricts) for a specific district using the GHN (Giao Hàng Nhanh) shipping service.  
                        This endpoint is usually called after a district is selected, allowing the frontend to dynamically display 
                        all available wards under that district.
                    
                        **Workflow:**
                        1. The client provides a `districtId` as a request parameter.
                        2. The system calls the GHN API to fetch wards corresponding to the given district.
                        3. The ward data is formatted as a key-value map (ward code → ward name).
                        4. The formatted list is returned to the client.
                    
                        **Use cases:**
                        - Displaying available wards when users select a district during checkout or address creation.
                        - Completing address selection for shipping, billing, or delivery purposes.
                    """
    )
    @GetMapping("/wards")
    public ResponseEntity<?> getWards(
            @RequestParam int districtId
    ) throws JsonProcessingException {
        Map<String, String> wardsMap = new HashMap<>();
        wardsMap = ghnService.getWardListByDistrictId(districtId);
        RestResponse response = responseMapper.toDto(
                true,
                "FETCH WARDS SUCCESSFULLY",
                wardsMap,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @Operation(
            summary = "Fetch shipping fee for a specific order",
            description = """
                        Retrieves the shipping fee for a given order using the GHN (Giao Hàng Nhanh) shipping service.  
                        The system uses order details (such as delivery address, package weight, and dimensions) to query 
                        GHN's API and return the calculated shipping cost.
                    
                        **Workflow:**
                        1. The client sends an `orderId` as a path variable.
                        2. The system validates that the order exists.
                        3. The GHN API is called with the order’s delivery information.
                        4. The calculated shipping fee and related details (service type, estimated delivery time, etc.) are returned.
                    
                        **Use cases:**
                        - Displaying the estimated or actual shipping fee on the order details page.
                        - Allowing sellers or admins to verify delivery costs before fulfillment.
                        - Showing buyers the delivery cost breakdown in checkout or order tracking screens.
                    
                        **Security Notes:**
                        - Requires a valid JWT token (`ROLE_BUYER`, `ROLE_SELLER`, or `ROLE_ADMIN`).
                        - A user can only access shipping fee information for orders they own.
                    """
    )
    @GetMapping("/shipping-fee/{orderId}")
    public ResponseEntity<?> getShippingFee(
            @PathVariable Long orderId
    ) throws Exception {
        int codValue = 0;
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFound());
        Map<String, String> shippingFeeData = ghnService.getShippingFeeDto(order, codValue);
        RestResponse response = responseMapper.toDto(
                true,
                "FETCH SHIPPING FEE SUCCESSFULLY",
                shippingFeeData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @PostMapping("/shipping-fee")
    public ResponseEntity<RestResponse<Map<String, String>, Object>> getShippingFee(
            @Valid @RequestBody ShippingFeeRequest request
    ) throws Exception {
        int codValue = 0;
        log.info(">>> [ShippingServiceController] in getShippingFee: codValue = {}", codValue);
        PostProduct postProduct = postProductRepository.findById(request.getPostId()).orElseThrow(() -> new PostProductNotFound());

        Buyer currentBuyer = buyerService.getCurrentUser();
        Buyer targetBuyer = currentBuyer;
        targetBuyer.setWardName(request.getWardName());
        targetBuyer.setDistrictName(request.getDistrictName());
        targetBuyer.setProvinceName(request.getProvinceName());

        Seller seller = postProduct.getSeller();

        Payment payment = paymentService.findPaymentMethodById(request.getPaymentId());

        if (payment == null) {
            throw new PaymentMethodNotSupportedException();
        }
        log.info(">>> [ShippingServiceController] in getShippingFee: Payment is supported");


        if (payment.getGatewayName().equalsIgnoreCase("COD")) {
            log.info(">>> [ShippingServiceController] in getShippingFee: COD payment");
            codValue = postProduct.getPrice().intValue();
        }
        log.info(">>> [ShippingServiceController] in getShippingFee: codValue = {}", codValue);

        Map<String, String> shippingFeeData = ghnService.getShippingFeeDto(targetBuyer, seller, postProduct, codValue);

        RestResponse<Map<String, String>, Object> response = responseMapper.toDto(
                true,
                "FETCH SHIPPING FEE SUCCESSFULLY",
                shippingFeeData,
                null
        );

        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }

    @GetMapping("/order/{orderId}/status")
    public ResponseEntity<RestResponse<Map<String, Object>, Object>> getOrderStatus(@PathVariable Long orderId) {
        Order foundOrder = orderService.getOrderById(orderId);

        if (foundOrder == null) {
            throw new OrderNotFound();
        }

        Map<String, Object> responseData = ghnService.getLastestOrderStatus(foundOrder.getOrderCode());

        RestResponse<Map<String, Object>, Object> response = responseMapper.toDto(
                true,
                "FETCH ORDER SHIPPING STATUS SUCCESSFULLY",
                responseData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }
}
