package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.Payment;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.service.implement.GhnServiceImpl;
import Green_trade.green_trade_platform.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class GhnServiceTest {

    private StringUtils stringUtils;
    private GhnServiceImpl service;

    @BeforeEach
    void setup() {
        stringUtils = mock(StringUtils.class);
        service = new GhnServiceImpl(stringUtils);
    }

    @Test
    void shouldMapCreateOrderSuccessResponseToDto() throws Exception {
        // Arrange domain
        Buyer sellerBuyer = Buyer.builder()
                .fullName("Seller Name")
                .phoneNumber("0900000000")
                .street("Seller Street")
                .wardName("Seller Ward")
                .districtName("Seller District")
                .provinceName("Seller Province")
                .build();
        Seller seller = Seller.builder()
                .buyer(sellerBuyer)
                .ghnShopId("SHOP123")
                .build();
        Buyer buyer = Buyer.builder()
                .fullName("Buyer Name")
                .phoneNumber("0911111111")
                .street("Buyer Street")
                .wardName("Buyer Ward")
                .districtName("Buyer District")
                .provinceName("Buyer Province")
                .build();
        PostProduct post = PostProduct.builder()
                .title("Eco Product")
                .price(new BigDecimal("150000"))
                .length("10").width("20").height("30").weight("400")
                .seller(seller)
                .build();
        Order order = Order.builder()
                .price(new BigDecimal("200000"))
                .buyer(buyer)
                .postProduct(post)
                .build();
        Payment payment = Payment.builder().gatewayName("COD").build();

        when(stringUtils.fullAddress(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Seller Street, Seller Ward, Seller District, Seller Province");

        // Mock lookups to avoid HTTP
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn("1").when(spyService).findProvinceCodeByProvinceName(anyString());
        doReturn("2").when(spyService).findDistrictCodeByDistrictName(anyInt(), anyString());
        doReturn("3").when(spyService).findWardCodeByWardName(anyInt(), anyString());

        // Mock RestTemplate construction and response for createOrder
        String responseJson = "{\n" +
                "  \"code\": 200,\n" +
                "  \"message\": \"Success\",\n" +
                "  \"message_display\": \"Created\",\n" +
                "  \"data\": {\n" +
                "    \"order_code\": \"GHN001\",\n" +
                "    \"sort_code\": \"SC01\",\n" +
                "    \"trans_type\": \"truck\",\n" +
                "    \"expected_delivery_time\": \"2025-01-01T10:00:00Z\",\n" +
                "    \"fee\": {\"main_service\": 15000, \"insurance\": 2000},\n" +
                "    \"total_fee\": 17000\n" +
                "  }\n" +
                "}";

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));
        })) {
            // Act
            Map<String, String> dto = spyService.createOrderShippingResponseToDto(order, payment);

            // Assert
            assertEquals("true", dto.get("success"));
            assertEquals("GHN001", dto.get("orderCode"));
            assertEquals("SC01", dto.get("sortCode"));
            assertEquals("truck", dto.get("transType"));
            assertEquals("2025-01-01T10:00:00Z", dto.get("expectedDeliveryTime"));
            assertEquals("15000", dto.get("mainServiceFee"));
            assertEquals("2000", dto.get("insuranceFee"));
            assertEquals("17000", dto.get("totalFee"));
            assertEquals("Created", dto.get("messageDisplay"));
        }
    }

    @Test
    void shouldCreateCancelOrderBodyRequestCorrectly() {
        String orderCode = "ORDER123";

        Map<String, Object> result = service.createCancelOrderShippingServiceBodyRequest(orderCode);

        assertNotNull(result);
        assertTrue(result.containsKey("order_codes"));
        assertEquals(List.of(orderCode), result.get("order_codes"));
    }

    @Test
    void shouldReturnShippingFeeDtoWhenResponseIsValid() throws Exception {
        // Arrange minimal order to call getShippingFeeDto(Order,...)
        Buyer sellerBuyer = Buyer.builder()
                .provinceName("Seller Province")
                .districtName("Seller District")
                .wardName("Seller Ward")
                .build();
        Seller seller = Seller.builder()
                .buyer(sellerBuyer)
                .ghnShopId("SHOP_ID")
                .build();
        Buyer buyer = Buyer.builder()
                .provinceName("Buyer Province")
                .districtName("Buyer District")
                .wardName("Buyer Ward")
                .build();
        PostProduct post = PostProduct.builder()
                .title("Item")
                .length("10").width("20").height("30").weight("400")
                .price(new BigDecimal("50000"))
                .seller(seller)
                .build();
        Order order = Order.builder()
                .buyer(buyer)
                .postProduct(post)
                .build();

        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn("11").when(spyService).findProvinceCodeByProvinceName(anyString());
        doReturn("22").when(spyService).findDistrictCodeByDistrictName(anyInt(), anyString());
        doReturn("33").when(spyService).findWardCodeByWardName(anyInt(), anyString());

        String feeResponse = "{\n" +
                "  \"code\": 200,\n" +
                "  \"message\": \"OK\",\n" +
                "  \"data\": {\n" +
                "    \"total\": 10000,\n" +
                "    \"service_fee\": 7000,\n" +
                "    \"insurance_fee\": 500,\n" +
                "    \"pick_station_fee\": 0,\n" +
                "    \"coupon_value\": 0,\n" +
                "    \"r2s_fee\": 1000,\n" +
                "    \"cod_fee\": 1500,\n" +
                "    \"pick_remote_areas_fee\": 0,\n" +
                "    \"deliver_remote_areas_fee\": 0,\n" +
                "    \"cod_failed_fee\": 0\n" +
                "  }\n" +
                "}";

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(contains("/fee"), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(feeResponse, HttpStatus.OK));
        })) {
            // Act
            Map<String, String> dto = spyService.getShippingFeeDto(order, 0);

            // Assert
            assertEquals("OK", dto.get("message"));
            assertEquals("10000", dto.get("total"));
            assertEquals("7000", dto.get("service_fee"));
            assertEquals("500", dto.get("insurance_fee"));
            assertEquals("0", dto.get("pick_station_fee"));
            assertEquals("0", dto.get("coupon_value"));
            assertEquals("1000", dto.get("r2s_fee"));
            assertEquals("1500", dto.get("cod_fee"));
            assertEquals("0", dto.get("pick_remote_areas_fee"));
            assertEquals("0", dto.get("deliver_remote_areas_fee"));
            assertEquals("0", dto.get("cod_failed_fee"));
        }
    }

    @Test
    void shouldExtractLatestStatusFromLogs() {
        // Arrange JSON with logs where latest log overrides top-level status
        String response = "{\n" +
                "  \"data\": {\n" +
                "    \"status\": \"processing\",\n" +
                "    \"updated_date\": \"2025-01-01T00:00:00Z\",\n" +
                "    \"order_code\": \"OC123\",\n" +
                "    \"shop_id\": 999,\n" +
                "    \"log\": [\n" +
                "      {\"status\": \"picked\", \"updated_date\": \"2025-01-02T00:00:00Z\"},\n" +
                "      {\"status\": \"delivering\", \"updated_date\": \"2025-01-03T00:00:00Z\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        // Act
        Map<String, Object> result = service.extractLatestOrderStatus(response);

        // Assert
        assertEquals("delivering", result.get("status"));
        assertEquals("2025-01-03T00:00:00Z", result.get("updated_date"));
        assertEquals("OC123", result.get("order_code"));
        assertEquals(999L, result.get("shop_id"));
        assertEquals("Success", result.get("message"));
    }

    @Test
    void shouldThrowWhenShippingFeeResponseCodeIsNot200() throws Exception {
        // Arrange order similar to earlier
        Buyer sellerBuyer = Buyer.builder()
                .provinceName("Seller Province")
                .districtName("Seller District")
                .wardName("Seller Ward")
                .build();
        Seller seller = Seller.builder()
                .buyer(sellerBuyer)
                .ghnShopId("SHOP_ID")
                .build();
        Buyer buyer = Buyer.builder()
                .provinceName("Buyer Province")
                .districtName("Buyer District")
                .wardName("Buyer Ward")
                .build();
        PostProduct post = PostProduct.builder()
                .title("Item")
                .length("10").width("20").height("30").weight("400")
                .price(new BigDecimal("50000"))
                .seller(seller)
                .build();
        Order order = Order.builder()
                .buyer(buyer)
                .postProduct(post)
                .build();

        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn("11").when(spyService).findProvinceCodeByProvinceName(anyString());
        doReturn("22").when(spyService).findDistrictCodeByDistrictName(anyInt(), anyString());
        doReturn("33").when(spyService).findWardCodeByWardName(anyInt(), anyString());

        String errorResponse = "{\n" +
                "  \"code\": 400,\n" +
                "  \"message\": \"Bad Request\",\n" +
                "  \"data\": {}\n" +
                "}";

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(contains("/fee"), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));
        })) {
            // Act + Assert
            RuntimeException ex = assertThrows(RuntimeException.class, () -> spyService.getShippingFeeDto(order, 0));
            assertTrue(ex.getMessage().contains("GHN API trả về lỗi"));
        }
    }

    @Test
    void shouldReturnNoDataWhenOrderDetailHasNoData() {
        // Arrange response with invalid/empty data
        String responseEmptyArray = "{ \"data\": [] }";
        Map<String, Object> result1 = service.extractLatestOrderStatus(responseEmptyArray);
        assertEquals("no_data", result1.get("status"));
        assertEquals("Không có dữ liệu đơn hàng", result1.get("message"));

        String responseMissingData = "{ \"message\": \"ok\" }";
        Map<String, Object> result2 = service.extractLatestOrderStatus(responseMissingData);
        assertEquals("no_data", result2.get("status"));
        assertEquals("Không có dữ liệu đơn hàng", result2.get("message"));
    }

    @Test
    void shouldReturnErrorJsonWhenCancelOrderFails() throws Exception {
        // Arrange mock RestTemplate to throw
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenThrow(new RuntimeException("network down"));
        })) {
            Map<String, Object> req = new HashMap<>();
            req.put("order_codes", List.of("OC_FAIL"));

            // Act
            String result = service.cancelOrder(req, "SHOP123");

            // Assert
            assertTrue(result.contains("\"error\":\"network down\""));
        }
    }

    @Test
    void testParseSuccessfulCancelResponseWithDataArray() throws Exception {
        String json = "{\n" +
                "  \"code\": 200,\n" +
                "  \"message\": \"OK\",\n" +
                "  \"data\": [\n" +
                "    {\"order_code\": \"OC1\", \"result\": true, \"message\": \"Cancelled\"},\n" +
                "    {\"order_code\": \"OC2\", \"result\": false, \"message\": \"Already delivered\"}\n" +
                "  ]\n" +
                "}";
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn(json).when(spyService).cancelOrder(anyMap(), anyString());

        Map<String, Object> dto = spyService.createCancelOrderShippingServiceResponseToDto("OC1", "SHOP1");

        assertEquals(200, dto.get("code"));
        assertEquals("OK", dto.get("message"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) dto.get("data");
        assertNotNull(data);
        assertEquals(2, data.size());

        Map<String, Object> first = data.get(0);
        assertEquals("OC1", first.get("order_code"));
        assertEquals(true, first.get("result"));
        assertEquals("Cancelled", first.get("message"));

        Map<String, Object> second = data.get(1);
        assertEquals("OC2", second.get("order_code"));
        assertEquals(false, second.get("result"));
        assertEquals("Already delivered", second.get("message"));
    }

    @Test
    void testHandleEmptyDataArrayReturnsEmptyList() throws Exception {
        String json = "{\n" +
                "  \"code\": 200,\n" +
                "  \"message\": \"OK\",\n" +
                "  \"data\": []\n" +
                "}";
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn(json).when(spyService).cancelOrder(anyMap(), anyString());

        Map<String, Object> dto = spyService.createCancelOrderShippingServiceResponseToDto("OC_EMPTY", "SHOP1");

        assertEquals(200, dto.get("code"));
        assertEquals("OK", dto.get("message"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) dto.get("data");
        assertNotNull(data);
        assertTrue(data.isEmpty());
    }

    @Test
    void testDefaultValuesAppliedForMissingItemFields() throws Exception {
        String json = "{\n" +
                "  \"code\": 200,\n" +
                "  \"message\": \"OK\",\n" +
                "  \"data\": [\n" +
                "    {}\n" +
                "  ]\n" +
                "}";
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn(json).when(spyService).cancelOrder(anyMap(), anyString());

        Map<String, Object> dto = spyService.createCancelOrderShippingServiceResponseToDto("OC_MISSING", "SHOP1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) dto.get("data");
        assertNotNull(data);
        assertEquals(1, data.size());
        Map<String, Object> item = data.get(0);

        assertEquals("", item.get("order_code"));
        assertEquals(false, item.get("result"));
        assertEquals("", item.get("message"));
    }

    @Test
    void testNonArrayDataProducesEmptyList() throws Exception {
        // data as object
        String jsonObject = "{ \"code\":201, \"message\":\"Accepted\", \"data\": {\"some\":\"field\"} }";
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn(jsonObject).when(spyService).cancelOrder(anyMap(), anyString());

        Map<String, Object> dtoObject = spyService.createCancelOrderShippingServiceResponseToDto("OC_OBJ", "SHOP1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataObject = (List<Map<String, Object>>) dtoObject.get("data");
        assertNotNull(dataObject);
        assertTrue(dataObject.isEmpty());

        // data as null
        String jsonNull = "{ \"code\":201, \"message\":\"Accepted\", \"data\": null }";
        doReturn(jsonNull).when(spyService).cancelOrder(anyMap(), anyString());

        Map<String, Object> dtoNull = spyService.createCancelOrderShippingServiceResponseToDto("OC_NULL", "SHOP1");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataNull = (List<Map<String, Object>>) dtoNull.get("data");
        assertNotNull(dataNull);
        assertTrue(dataNull.isEmpty());
    }

    @Test
    void testDefaultTopLevelFieldsWhenMissing() throws Exception {
        String json = "{ \"data\": [ {\"order_code\":\"OCX\",\"result\":true,\"message\":\"ok\"} ] }";
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn(json).when(spyService).cancelOrder(anyMap(), anyString());

        Map<String, Object> dto = spyService.createCancelOrderShippingServiceResponseToDto("OCX", "SHOP1");

        assertEquals(0, dto.get("code"));
        assertEquals("", dto.get("message"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) dto.get("data");
        assertEquals(1, data.size());
        assertEquals("OCX", data.get(0).get("order_code"));
    }

    @Test
    void testMalformedJsonThrowsJsonProcessingException() {
        String invalidJson = "{ this is not valid json ";
        GhnServiceImpl spyService = Mockito.spy(service);
        doReturn(invalidJson).when(spyService).cancelOrder(anyMap(), anyString());

        assertThrows(JsonProcessingException.class, () ->
                spyService.createCancelOrderShippingServiceResponseToDto("BAD", "SHOP1"));
    }

    @Test
    void shouldPostOrderCodeAndReturnResponseBody() {
        // inject TOKEN value via reflection
        try {
            Field tokenField = GhnServiceImpl.class.getDeclaredField("TOKEN");
            tokenField.setAccessible(true);
            tokenField.set(service, "TEST_TOKEN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String expectedBody = "{\"data\":\"ok\"}";
        String orderCode = "OC123";

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenAnswer((InvocationOnMock inv) -> {
                        // verify endpoint
                        String url = inv.getArgument(0, String.class);
                        assertTrue(url.contains("/shipping-order/detail"));
                        // verify request entity contains order_code
                        @SuppressWarnings("unchecked")
                        HttpEntity<Map<String, Object>> entity = inv.getArgument(1, HttpEntity.class);
                        Map<String, Object> body = entity.getBody();
                        assertNotNull(body);
                        assertEquals(orderCode, body.get("order_code"));
                        return new ResponseEntity<>(expectedBody, HttpStatus.OK);
                    });
        })) {
            String result = service.getOrderDetail(orderCode);
            assertEquals(expectedBody, result);
        }
    }

    @Test
    void shouldSetTokenAndContentTypeHeaders() {
        // inject TOKEN value via reflection
        try {
            Field tokenField = GhnServiceImpl.class.getDeclaredField("TOKEN");
            tokenField.setAccessible(true);
            tokenField.set(service, "TEST_TOKEN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String orderCode = "OC999";
        String resp = "{\"status\":\"ok\"}";

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        HttpEntity<Map<String, Object>> entity = inv.getArgument(1, HttpEntity.class);
                        HttpHeaders headers = entity.getHeaders();
                        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
                        assertEquals("TEST_TOKEN", headers.getFirst("Token"));
                        return new ResponseEntity<>(resp, HttpStatus.OK);
                    });
        })) {
            String result = service.getOrderDetail(orderCode);
            assertEquals(resp, result);
        }
    }

    @Test
    void shouldAllowNullOrderCodeInRequestBody() {
        // inject TOKEN value via reflection
        try {
            Field tokenField = GhnServiceImpl.class.getDeclaredField("TOKEN");
            tokenField.setAccessible(true);
            tokenField.set(service, "TEST_TOKEN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String responseBody = "{\"nullOrder\":\"handled\"}";

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        HttpEntity<Map<String, Object>> entity = inv.getArgument(1, HttpEntity.class);
                        Map<String, Object> body = entity.getBody();
                        assertTrue(body.containsKey("order_code"));
                        assertNull(body.get("order_code"));
                        return new ResponseEntity<>(responseBody, HttpStatus.OK);
                    });
        })) {
            String result = service.getOrderDetail(null);
            assertEquals(responseBody, result);
        }
    }

    @Test
    void shouldPropagateExceptionOnApiFailure() {
        // inject TOKEN value via reflection
        try {
            Field tokenField = GhnServiceImpl.class.getDeclaredField("TOKEN");
            tokenField.setAccessible(true);
            tokenField.set(service, "TEST_TOKEN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenThrow(new RuntimeException("api down"));
        })) {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getOrderDetail("FAIL_OC"));
            assertEquals("api down", ex.getMessage());
        }
    }

    @Test
    void shouldReturnNullWhenResponseBodyIsNull() {
        // inject TOKEN value via reflection
        try {
            Field tokenField = GhnServiceImpl.class.getDeclaredField("TOKEN");
            tokenField.setAccessible(true);
            tokenField.set(service, "TEST_TOKEN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        })) {
            String result = service.getOrderDetail("ANY");
            assertNull(result);
        }
    }
}
