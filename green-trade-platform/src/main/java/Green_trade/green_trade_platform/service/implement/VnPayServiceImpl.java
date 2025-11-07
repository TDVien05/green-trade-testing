package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.config.VnPayConfig;
import Green_trade.green_trade_platform.model.Buyer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VnPayServiceImpl {
    @Value(("${vnp_TmnCode}"))
    private String vnpTmnCode;
    @Value("${vnp_HashSecret}")
    private String vnpHashSecret;
    @Value(("${vnp_Url}"))
    private String vnpUrl;
    @Value(("${vnpay.return-url}"))
    private String vnpReturnUrl;

    private final BuyerServiceImpl buyerService;
    private final VnPayConfig vnPayConfig;
//    private final VnPayUtils vnPayUtils;
//    private final WalletRepository walletRepository;

    public VnPayServiceImpl(VnPayConfig config, BuyerServiceImpl buyerService) {
        this.vnPayConfig = config;
        this.buyerService = buyerService;
    }

    public Map<String, Object> createPaymentUrl(HttpServletRequest req, long amount) throws Exception {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TmnCode = VnPayConfig.vnp_TmnCode;
        String vnp_TxnRef = VnPayConfig.getRandomNumber(8);
        String vnp_IpAddr = req.getRemoteAddr();
        Buyer buyer = buyerService.getCurrentUser();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");

        String bankCode = req.getParameter("bankcode");
        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", buyer.getBuyerId().toString() + " : " + buyer.getUsername() + " nạp tiền vào ví.");
        vnp_Params.put("vnp_OrderType", Optional.ofNullable(req.getParameter("ordertype")).orElse("other"));
        vnp_Params.put("vnp_Locale", Optional.ofNullable(req.getParameter("language")).orElse("vn"));
        vnp_Params.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

// Lấy thời gian bắt đầu theo múi giờ Việt Nam
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String startTime = sdf.format(cld.getTime());

// Cộng thêm 15 phút để tính expire
        cld.add(Calendar.MINUTE, 15);
        String expire = sdf.format(cld.getTime());

        vnp_Params.put("vnp_CreateDate", startTime);
        vnp_Params.put("vnp_ExpireDate", expire);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringJoiner hashData = new StringJoiner("&");
        StringJoiner query = new StringJoiner("&");

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.add(fieldName + "=" + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                query.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()) + "=" +
                        URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
            }
        }

        String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, hashData.toString());
        query.add("vnp_SecureHash=" + vnp_SecureHash);

        String paymentUrl = VnPayConfig.vnp_Url + "?" + query.toString();

        Map<String, Object> result = new HashMap<>();
        result.put("url_payment", paymentUrl);
        return result;
    }


    public Map<String, Object> processReturn(HttpServletRequest request) {
        Map<String, String> inputData = new HashMap<>();

        // Lấy toàn bộ tham số vnp_ gửi về
        request.getParameterMap().forEach((key, value) -> {
            if (key.startsWith("vnp_")) {
                inputData.put(key, value[0]);
            }
        });

        String vnp_SecureHash = inputData.get("vnp_SecureHash");
        inputData.remove("vnp_SecureHash");
        inputData.remove("vnp_SecureHashType");

        // Sắp xếp theo thứ tự key tăng dần
        List<String> fieldNames = new ArrayList<>(inputData.keySet());
        Collections.sort(fieldNames);

        // Ghép chuỗi dữ liệu để hash
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String key = fieldNames.get(i);
            String value = inputData.get(key);
            try {
                if (i > 0) {
                    hashData.append('&');
                }
                hashData.append(URLEncoder.encode(key, "US-ASCII"))
                        .append('=')
                        .append(URLEncoder.encode(value, "US-ASCII"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        // Hash chuỗi dữ liệu
        String secureHash = VnPayConfig.hmacSHA512(VnPayConfig.vnp_HashSecret, hashData.toString());

        String transactionCode = inputData.get("vnp_TxnRef");
        String responseCode = inputData.get("vnp_ResponseCode");

        Map<String, Object> result = new HashMap<>();

        if (secureHash.equals(vnp_SecureHash)) {
            if ("00".equals(responseCode)) {
                result.put("success", true);
                result.put("transaction_code", transactionCode);
                result.put("message", "Xác minh thành công");
                result.put("response_code", responseCode);
            } else {
                result.put("success", false);
                result.put("", transactionCode);
                result.put("message", "Thanh toán thất bại!");
                result.put("response_code", responseCode);
            }
        } else {
            result.put("success", false);
            result.put("transaction_code", transactionCode);
            result.put("message", "Mã bảo mật không hợp lệ");
            result.put("vnp_secureHash", vnp_SecureHash);
            result.put("sign_value", secureHash);
            result.put("response_code", responseCode);
        }

        return result;
    }
}


