package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class GhnServiceImpl {

    private final StringUtils stringUtils;
    @Value("${ghn.token}")
    private String TOKEN;

    public GhnServiceImpl(StringUtils stringUtils) {
        this.stringUtils = stringUtils;
    }

    public String createOrder(Map<String, Object> requestBody, String shopId) {
        RestTemplate restTemplate = new RestTemplate();

        log.info(">>> ghnToken: {}", TOKEN);
        log.info(">>> requestBody in createOrderShippingAPI: {}", requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", TOKEN);
        headers.set("ShopId", shopId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create", entity, String.class);

        return response.getBody();
    }

    public String cancelOrder(Map<String, Object> request, String shopId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", TOKEN);
        headers.set("ShopId", shopId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/switch-status/cancel", entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    public String getOrderDetail(String orderCode) {
        RestTemplate restTemplate = new RestTemplate();

        log.info(">>> ghnToken: {}", TOKEN);
        log.info(">>> orderCode in getOrderDetail: {}", orderCode);

        // Body request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("order_code", orderCode);

        // Header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", TOKEN);

        // Entity (body + header)
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Gọi API GHN
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail",
                entity,
                String.class
        );

        log.info(">>> response from GHN: {}", response.getBody());
        return response.getBody();
    }

    public Map<String, Object> extractLatestOrderStatus(String ghnResponse) {
        Map<String, Object> result = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(ghnResponse);

            JsonNode dataNode = root.path("data");
            JsonNode orderData;

            if (dataNode.isArray() && dataNode.size() > 0) {
                orderData = dataNode.get(0);
            } else if (dataNode.isObject()) {
                orderData = dataNode;
            } else {
                log.warn(">>> GHN response không có data hợp lệ");
                result.put("status", "no_data");
                result.put("message", "Không có dữ liệu đơn hàng");
                return result;
            }

            JsonNode logs = orderData.path("log");
            String status = orderData.path("status").asText("");
            String updatedDate = orderData.path("updated_date").asText("");
            String orderCode = orderData.path("order_code").asText("");
            long shopId = orderData.path("shop_id").asLong(0);

            if (logs.isArray() && logs.size() > 0) {
                JsonNode latestLog = logs.get(logs.size() - 1);
                // Nếu log có status mới nhất thì ưu tiên lấy từ đó
                status = latestLog.path("status").asText(status);
                updatedDate = latestLog.path("updated_date").asText(updatedDate);
            }

            result.put("status", status.isEmpty() ? "unknown" : status);
            result.put("updated_date", updatedDate);
            result.put("order_code", orderCode);
            result.put("shop_id", shopId);
            result.put("message", "Success");

            log.info(">>> Trạng thái mới nhất: {} (updated_date: {} | order_code: {} | shop_id: {})",
                    status, updatedDate, orderCode, shopId);

            return result;

        } catch (Exception e) {
            log.error(">>> Lỗi khi phân tích GHN response: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", "Lỗi khi xử lý dữ liệu GHN: " + e.getMessage());
            return result;
        }
    }

    public Map<String, Object> getLastestOrderStatus(String orderCode) {
        String resultInString = getOrderDetail(orderCode);
        return extractLatestOrderStatus(resultInString);
    }

    public Map<String, Object> createCancelOrderShippingServiceBodyRequest(String orderCode) {
        return Map.of(
                "order_codes", List.of(orderCode)
        );
    }

    public Map<String, Object> createCancelOrderShippingServiceResponseToDto(String orderCode, String shopId) throws JsonProcessingException {
        Map<String, Object> bodyRequest = createCancelOrderShippingServiceBodyRequest(orderCode);
        String resultInString = cancelOrder(bodyRequest, shopId);

        Map<String, Object> result = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonData = objectMapper.readTree(resultInString);

        result.put("code", jsonData.path("code").asInt(0));
        result.put("message", jsonData.path("message").asText(""));

        JsonNode jsonNode = jsonData.path("data");

        if (jsonNode.isArray()) {
            List<Map<String, Object>> dataList = new ArrayList<>();

            for (JsonNode node : jsonNode) {
                Map<String, Object> item = new HashMap<>();
                item.put("order_code", node.path("order_code").asText(""));
                item.put("result", node.path("result").asBoolean(false));
                item.put("message", node.path("message").asText(""));
                dataList.add(item);
            }

            result.put("data", dataList);
        } else {
            result.put("data", Collections.emptyList());
        }

        return result;
    }

    public String getShippingFee(Map<String, Object> requestBody, String shopId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", TOKEN);
        headers.set("ShopId", shopId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee", entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    public String registerShop(Map<String, Object> requestBody) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", TOKEN);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shop/register", entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String getProvinces() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", "4433d6f4-ae5f-11f0-b040-4e257d8388b4");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/province",
                    HttpMethod.GET,
                    entity,
                    String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String getWards(int districtId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", "4433d6f4-ae5f-11f0-b040-4e257d8388b4");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/ward?district_id=" + districtId;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET, // GET là chuẩn nhất ở đây
                    entity,
                    String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public String getDistricts(int provinceId) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", "4433d6f4-ae5f-11f0-b040-4e257d8388b4");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("province_id", provinceId);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/district",
                    HttpMethod.POST, // Dù curl ghi GET, GHN yêu cầu POST khi có body
                    entity,
                    String.class);

            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public Map<String, String> getProvinceList() throws JsonProcessingException {
        Map<String, String> result = new HashMap<>();
        String provincesInString = getProvinces();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(provincesInString);
        JsonNode data = root.path("data");

        for (JsonNode province : data) {
            int id = province.path("ProvinceID").asInt();
            String name = province.path("ProvinceName").asText();
            result.put(id + "", name);
        }
        return result;
    }

    public Map<String, String> getDistrictListByProvinceId(int provinceId) throws JsonProcessingException {
        Map<String, String> result = new HashMap<>();
        String districtsInString = getDistricts(provinceId);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(districtsInString);
        JsonNode data = root.path("data");

        for (JsonNode province : data) {
            int id = province.path("DistrictID").asInt();
            String name = province.path("DistrictName").asText();
            result.put(id + "", name);
        }
        return result;
    }

    public Map<String, String> getWardListByDistrictId(int districtId) throws JsonProcessingException {
        Map<String, String> result = new HashMap<>();
        String districtsInString = getWards(districtId);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(districtsInString);
        JsonNode data = root.path("data");

        for (JsonNode province : data) {
            int id = province.path("WardCode").asInt();
            String name = province.path("WardName").asText();
            result.put(id + "", name);
        }
        return result;
    }

    public String findProvinceCodeByProvinceName(String provinceName) throws JsonProcessingException {
        Map<String, String> provinceList = getProvinceList();

        // Duyệt qua danh sách để tìm tỉnh có tên khớp
        for (Map.Entry<String, String> entry : provinceList.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(provinceName.trim())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String findDistrictCodeByDistrictName(int provinceId, String districtName) throws JsonProcessingException {
        Map<String, String> districtList = getDistrictListByProvinceId(provinceId);

        for (Map.Entry<String, String> entry : districtList.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(districtName.trim())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String findWardCodeByWardName(int districtId, String wardName) throws JsonProcessingException {
        Map<String, String> wardList = getWardListByDistrictId(districtId);

        for (Map.Entry<String, String> entry : wardList.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(wardName.trim())) {
                return entry.getKey(); // WardCode là String, không cần parse sang Long
            }
        }
        return null;
    }

    public Map<String, String> getShippingFeeDto(Order order, int codValue) throws JsonProcessingException {
        Map<String, Object> bodyData = getShippingFeeServiceBodyRequest(order, codValue);

        String resultString = getShippingFee(bodyData, order.getPostProduct().getSeller().getGhnShopId());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(resultString);

        if (root == null || !root.has("code")) {
            throw new RuntimeException("Phản hồi không hợp lệ từ GHN: " + resultString);
        }

        int code = root.path("code").asInt();
        if (code != 200) {
            String message = root.path("message").asText("Unknown error");
            throw new RuntimeException("GHN API trả về lỗi: " + message);
        }

        JsonNode data = root.path("data");

        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", root.path("message").asText());
        result.put("total", data.path("total").asText());
        result.put("service_fee", data.path("service_fee").asText());
        result.put("insurance_fee", data.path("insurance_fee").asText());
        result.put("pick_station_fee", data.path("pick_station_fee").asText());
        result.put("coupon_value", data.path("coupon_value").asText());
        result.put("r2s_fee", data.path("r2s_fee").asText());
        result.put("cod_fee", data.path("cod_fee").asText());
        result.put("pick_remote_areas_fee", data.path("pick_remote_areas_fee").asText());
        result.put("deliver_remote_areas_fee", data.path("deliver_remote_areas_fee").asText());
        result.put("cod_failed_fee", data.path("cod_failed_fee").asText());

        log.info("GHN shipping fee response mapped: {}", result);

        return result;
    }

    public Map<String, String> getShippingFeeDto(Buyer buyer, Seller seller, PostProduct postProduct, int codValue)
            throws JsonProcessingException {
        Map<String, Object> bodyData = getShippingFeeServiceBodyRequest(buyer, seller, postProduct, codValue);

        String resultString = getShippingFee(bodyData, seller.getGhnShopId());
        log.info(">>> resultInString: {}", resultString);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(resultString);

        if (root == null || !root.has("code")) {
            throw new RuntimeException("Phản hồi không hợp lệ từ GHN: " + resultString);
        }

        int code = root.path("code").asInt();
        if (code != 200) {
            String message = root.path("message").asText("Unknown error");
            throw new RuntimeException("GHN API trả về lỗi: " + message);
        }

        JsonNode data = root.path("data");

        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", root.path("message").asText());
        result.put("total", data.path("total").asText());
        result.put("service_fee", data.path("service_fee").asText());
        result.put("insurance_fee", data.path("insurance_fee").asText());
        result.put("pick_station_fee", data.path("pick_station_fee").asText());
        result.put("coupon_value", data.path("coupon_value").asText());
        result.put("r2s_fee", data.path("r2s_fee").asText());
        result.put("cod_fee", data.path("cod_fee").asText());
        result.put("pick_remote_areas_fee", data.path("pick_remote_areas_fee").asText());
        result.put("deliver_remote_areas_fee", data.path("deliver_remote_areas_fee").asText());
        result.put("cod_failed_fee", data.path("cod_failed_fee").asText());

        log.info("GHN shipping fee response mapped: {}", result);

        return result;
    }

    public Map<String, Object> getShippingFeeServiceBodyRequest(Order order, int codValue)
            throws JsonProcessingException {
        String sellerProvinceId = findProvinceCodeByProvinceName(
                order.getPostProduct().getSeller().getBuyer().getProvinceName());
        String sellerDistrictId = findDistrictCodeByDistrictName(Integer.parseInt(sellerProvinceId),
                order.getPostProduct().getSeller().getBuyer().getDistrictName());
        String sellerWardId = findWardCodeByWardName(Integer.parseInt(sellerDistrictId),
                order.getPostProduct().getSeller().getBuyer().getWardName());

        String buyerProvinceId = findProvinceCodeByProvinceName(order.getBuyer().getProvinceName());
        String buyerDistrictId = findDistrictCodeByDistrictName(Integer.parseInt(buyerProvinceId),
                order.getBuyer().getDistrictName());
        String buyerWardId = findWardCodeByWardName(Integer.parseInt(buyerDistrictId), order.getBuyer().getWardName());

        Map<String, Object> result = new HashMap<>();
        result.put("service_type_id", 5);
        result.put("from_district_id", Integer.parseInt(sellerDistrictId));
        result.put("from_ward_code", sellerWardId);
        result.put("to_district_id", Integer.parseInt(buyerDistrictId));
        result.put("to_ward_code", buyerWardId);
        result.put("length", order.getPostProduct().getLength());
        result.put("width", order.getPostProduct().getWidth());
        result.put("height", order.getPostProduct().getHeight());
        result.put("weight", order.getPostProduct().getWeight());
        result.put("cod_value", codValue);
        result.put("insurance_value", 0);
        result.put("coupon", null);
        Map<String, Object> item = Map.of(
                "name", order.getPostProduct().getTitle(),
                "quantity", 1,
                "length", order.getPostProduct().getLength(),
                "width", order.getPostProduct().getWidth(),
                "height", order.getPostProduct().getHeight(),
                "weight", order.getPostProduct().getWeight());
        result.put("items", List.of(item));
        return result;
    }

    public Map<String, Object> getShippingFeeServiceBodyRequest(Buyer buyer, Seller seller, PostProduct postProduct,
                                                                int codValue) throws JsonProcessingException {
        String sellerProvinceId = findProvinceCodeByProvinceName(seller.getBuyer().getProvinceName());
        log.info(">>> [GhnServiceImpl] seller province id: {}", sellerProvinceId);
        String sellerDistrictId = findDistrictCodeByDistrictName(Integer.parseInt(sellerProvinceId),
                seller.getBuyer().getDistrictName());
        log.info(">>> [GhnServiceImpl] seller district id: {}", sellerProvinceId);
        String sellerWardId = findWardCodeByWardName(Integer.parseInt(sellerDistrictId),
                seller.getBuyer().getWardName());
        log.info(">>> [GhnServiceImpl] seller ward id: {}", sellerWardId);

        String buyerProvinceId = findProvinceCodeByProvinceName(buyer.getProvinceName());
        log.info(">>> [GhnServiceImpl] buyer province id: {}", sellerProvinceId);
        String buyerDistrictId = findDistrictCodeByDistrictName(Integer.parseInt(buyerProvinceId),
                buyer.getDistrictName());
        log.info(">>> [GhnServiceImpl] buyer district id: {}", buyerDistrictId);
        String buyerWardId = findWardCodeByWardName(Integer.parseInt(buyerDistrictId), buyer.getWardName());
        log.info(">>> [GhnServiceImpl] buyer ward id: {}", buyerWardId);

        Map<String, Object> result = new HashMap<>();
        result.put("service_type_id", 5);
        result.put("from_district_id", Integer.parseInt(sellerDistrictId));
        result.put("from_ward_code", sellerWardId);
        result.put("to_district_id", Integer.parseInt(buyerDistrictId));
        result.put("to_ward_code", buyerWardId);
        result.put("length", Integer.parseInt(postProduct.getLength()));
        result.put("width", Integer.parseInt(postProduct.getWidth()));
        result.put("height", Integer.parseInt(postProduct.getHeight()));
        result.put("weight", Integer.parseInt(postProduct.getWeight()));
        result.put("cod_value", codValue);
        result.put("insurance_value", 0);
        result.put("coupon", null);
        Map<String, Object> item = Map.of(
                "name", postProduct.getTitle(),
                "quantity", 1,
                "length", Integer.parseInt(postProduct.getLength()),
                "width", Integer.parseInt(postProduct.getWidth()),
                "height", Integer.parseInt(postProduct.getHeight()),
                "weight", Integer.parseInt(postProduct.getWeight()));
        result.put("items", List.of(item));
        return result;
    }

    public Map<String, Object> createOrderShippingServiceBodyRequest(Order order, Payment paymentMethod)
            throws JsonProcessingException {
        Map<String, Object> data = new HashMap<>();
        Seller seller = order.getPostProduct().getSeller();
        Buyer buyer = order.getBuyer();
        PostProduct postProduct = order.getPostProduct();
        String sellerProvinceId = findProvinceCodeByProvinceName(seller.getBuyer().getProvinceName());
        String sellerDistrictId = findDistrictCodeByDistrictName(Integer.parseInt(sellerProvinceId),
                seller.getBuyer().getDistrictName());
        String sellerWardId = findWardCodeByWardName(Integer.parseInt(sellerDistrictId),
                seller.getBuyer().getWardName());
        String buyerProvinceId = findProvinceCodeByProvinceName(buyer.getProvinceName());
        String buyerDistrictId = findDistrictCodeByDistrictName(Integer.parseInt(buyerProvinceId),
                buyer.getDistrictName());
        String buyerWardId = findWardCodeByWardName(Integer.parseInt(buyerDistrictId), buyer.getWardName());
        int codValue = 0;
        if ("COD".equalsIgnoreCase(paymentMethod.getGatewayName())) {
            codValue = order.getPrice().intValue();
        }

        data.put("payment_type_id", 2);
        data.put("note", "Not have"); // lưu ý vì chưa có tham số truyền vào
        data.put("required_note", "KHONGCHOXEMHANG");
        data.put("return_phone", seller.getBuyer().getPhoneNumber());
        data.put("return_address", seller.getBuyer().getStreet());
        data.put("return_district_id", Integer.parseInt(sellerDistrictId));
        data.put("return_ward_code", sellerWardId);
        data.put("client_order_code", "");
        data.put("from_name", seller.getBuyer().getFullName());
        data.put("from_phone", seller.getBuyer().getPhoneNumber());
        data.put("from_address", stringUtils.fullAddress(seller.getBuyer().getStreet(), seller.getBuyer().getWardName(), seller.getBuyer().getDistrictName(), seller.getBuyer().getProvinceName()));
        data.put("from_ward_name", seller.getBuyer().getWardName());
        data.put("from_district_name", seller.getBuyer().getDistrictName());
        data.put("from_province_name", seller.getBuyer().getProvinceName());
        data.put("to_name", buyer.getFullName());
        data.put("to_phone", buyer.getPhoneNumber());
        data.put("to_address", buyer.getStreet());
        data.put("to_ward_name", buyer.getWardName());
        data.put("to_district_name", buyer.getDistrictName());
        data.put("to_province_name", buyer.getProvinceName());
        data.put("cod_amount", codValue);
        data.put("content", order.getPostProduct().getTitle());
        data.put("length", Integer.parseInt(postProduct.getLength()));
        data.put("width", Integer.parseInt(postProduct.getWidth()));
        data.put("height", Integer.parseInt(postProduct.getHeight()));
        data.put("weight", Integer.parseInt(postProduct.getWeight()));
        data.put("cod_failed_amount", 2000);
        data.put("pick_station_id", 1444);
        data.put("deliver_station_id", null);
        data.put("insurance_value", 0);
        data.put("service_type_id", 5);
        data.put("coupon", null);
        data.put("pickup_time", 1692840132);
        data.put("pick_shift", List.of(2));

        // Items
        Map<String, Object> item = new HashMap<>();
        item.put("name", order.getPostProduct().getTitle());
        item.put("code", "");
        item.put("quantity", 1);
        item.put("price", postProduct.getPrice().intValue());
        item.put("length", Integer.parseInt(postProduct.getLength()));
        item.put("width", Integer.parseInt(postProduct.getWidth()));
        item.put("height", Integer.parseInt(postProduct.getHeight()));
        item.put("weight", Integer.parseInt(postProduct.getWeight()));

        Map<String, Object> category = new HashMap<>();
        category.put("level1", order.getPostProduct().getTitle());
        item.put("category", category);

        data.put("items", List.of(item));

        return data;
    }

    public Map<String, String> createOrderShippingResponseToDto(Order order, Payment paymentMethod)
            throws JsonProcessingException {

        Map<String, Object> bodyData = createOrderShippingServiceBodyRequest(order, paymentMethod);
        log.info(">>> [GHN Service] GHN body data: {}", bodyData);
        log.info(">>> [GHN Service] GHN shop id: {}", order.getPostProduct().getSeller().getGhnShopId());

        String resultInString = createOrder(bodyData, order.getPostProduct().getSeller().getGhnShopId());
        log.info(">>> [GHN Service] Result in String: {}", resultInString);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(resultInString);

        Map<String, String> response = new HashMap<>();

        int code = root.path("code").asInt();
        String message = root.path("message").asText();
        String messageDisplay = root.path("message_display").asText();

        if (code == 200) {
            JsonNode data = root.path("data");

            String orderCode = data.path("order_code").asText("");
            String sortCode = data.path("sort_code").asText("");
            String transType = data.path("trans_type").asText("");
            String expectedDeliveryTime = data.path("expected_delivery_time").asText("");

            JsonNode fee = data.path("fee");
            String mainService = fee.path("main_service").asText("0");
            String insurance = fee.path("insurance").asText("0");
            String totalFee = data.path("total_fee").asText("0");

            response.put("success", "true");
            response.put("orderCode", orderCode);
            response.put("sortCode", sortCode);
            response.put("transType", transType);
            response.put("expectedDeliveryTime", expectedDeliveryTime);
            response.put("mainServiceFee", mainService);
            response.put("insuranceFee", insurance);
            response.put("totalFee", totalFee);
            response.put("messageDisplay", messageDisplay);

        } else {
            response.put("success", "false");
            response.put("message", message);
            response.put("messageDisplay", messageDisplay);
        }

        log.info(">>> TEst: {}", response);

        return response;
    }

    public String getLastestOrderStatusOnlyStatus(String orderCode) {
        Map<String, Object> result = getLastestOrderStatus(orderCode);

        if (result == null || result.isEmpty()) {
            return "unknown";
        }

        Object statusObj = result.get("status");
        String status = statusObj != null ? statusObj.toString() : "unknown";

        return status;
    }

}
