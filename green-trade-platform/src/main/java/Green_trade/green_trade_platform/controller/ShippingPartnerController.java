package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.mapper.ShippingPartnerMapper;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.ShippingPartner;
import Green_trade.green_trade_platform.response.RestResponse;
import Green_trade.green_trade_platform.response.ShippingPartnerResponse;
import Green_trade.green_trade_platform.service.implement.ShippingPartnerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping-partner")
public class ShippingPartnerController {

    private final ShippingPartnerServiceImpl shippingPartnerService;
    private final ShippingPartnerMapper shippingPartnerMapper;
    private final ResponseMapper responseMapper;

    public ShippingPartnerController(ShippingPartnerServiceImpl shippingPartnerService, ShippingPartnerMapper shippingPartnerMapper, ResponseMapper responseMapper) {
        this.shippingPartnerService = shippingPartnerService;
        this.shippingPartnerMapper = shippingPartnerMapper;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Fetch available shipping partners",
            description = """
                        Retrieves a list of all active shipping partners integrated with the platform.  
                        Each partner entry contains details such as partner name, service type, code, 
                        and additional configuration details used for order delivery.
                    
                        **Workflow:**
                        1. The system queries all registered or active shipping partners from the database.
                        2. Each partner record is mapped to a standardized response format.
                        3. The endpoint returns the complete list of shipping partners available for order delivery.
                    
                        **Use cases:**
                        - Allowing buyers to select a preferred shipping carrier during checkout.
                        - Enabling sellers or admins to view supported logistics partners.
                        - Integrating third-party shipping APIs like GHN, GHTK, or Viettel Post.
                    
                        **Security Notes:**
                        - This endpoint can be public or protected depending on configuration.
                        - If authentication is enforced, only authorized roles (e.g., `ROLE_SELLER`, `ROLE_ADMIN`) can access it.
                    """
    )
    @GetMapping("/partners")
    public ResponseEntity<RestResponse<List<ShippingPartnerResponse>, Object>> getShippingPartners() {
        List<ShippingPartnerResponse> responseData = new ArrayList<>();
        List<ShippingPartner> shippingPartners = shippingPartnerService.getShippingPartners();
        shippingPartners.forEach(
                shippingPartner -> responseData.add(shippingPartnerMapper.toDto(shippingPartner))
        );
        RestResponse<List<ShippingPartnerResponse>, Object> response = responseMapper.toDto(
                true,
                "FETCH SHIPPING PARTNER SUCCESSFULLY",
                responseData,
                null
        );
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }
}
