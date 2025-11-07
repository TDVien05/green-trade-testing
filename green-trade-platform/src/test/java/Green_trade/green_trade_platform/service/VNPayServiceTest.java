package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.config.VnPayConfig;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.VnPayServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class VNPayServiceTest {

    private BuyerServiceImpl buyerService;
    private VnPayConfig vnPayConfig;
    private VnPayServiceImpl vnPayService;

    @BeforeEach
    void setup() {
        buyerService = mock(BuyerServiceImpl.class);
        vnPayConfig = new VnPayConfig();
        vnPayService = new VnPayServiceImpl(vnPayConfig, buyerService);
    }

    @Test
    void shouldGenerateSignedPaymentUrlWithRequiredParams() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        when(req.getParameter("bankcode")).thenReturn(null);
        when(req.getParameter("ordertype")).thenReturn(null);
        when(req.getParameter("language")).thenReturn(null);

        Buyer buyer = new Buyer();
        buyer.setBuyerId(123L);
        buyer.setUsername("john");
        when(buyerService.getCurrentUser()).thenReturn(buyer);

        try (MockedStatic<VnPayConfig> mocked = mockStatic(VnPayConfig.class)) {
            mocked.when(() -> VnPayConfig.getRandomNumber(8)).thenReturn("TXN12345");
            mocked.when(() -> VnPayConfig.hmacSHA512(any(), any())).thenReturn("SEC123");

            // Gán trực tiếp các field static thay vì mock
            VnPayConfig.vnp_TmnCode = "TESTCODE";
            VnPayConfig.vnp_ReturnUrl = "http://test-return";
            VnPayConfig.vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
            VnPayConfig.vnp_HashSecret = "TESTSECRET";

            Map<String, Object> result = vnPayService.createPaymentUrl(req, 10000L);
            assertNotNull(result);
            String url = (String) result.get("url_payment");
            assertNotNull(url);
            assertTrue(url.startsWith(VnPayConfig.vnp_Url + "?"));

            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            assertTrue(decoded.contains("vnp_Version=2.1.0"));
            assertTrue(decoded.contains("vnp_Command=pay"));
            assertTrue(decoded.contains("vnp_TmnCode=" + VnPayConfig.vnp_TmnCode));
            assertTrue(decoded.contains("vnp_Amount=" + (10000L * 100)));
            assertTrue(decoded.contains("vnp_CurrCode=VND"));
            assertTrue(decoded.contains("vnp_TxnRef=TXN12345"));
            assertTrue(decoded.contains("vnp_ReturnUrl=" + VnPayConfig.vnp_ReturnUrl));
            assertTrue(decoded.contains("vnp_IpAddr=127.0.0.1"));
            assertTrue(decoded.contains("vnp_OrderInfo=123") || decoded.contains("john"));
            assertTrue(decoded.contains("vnp_Locale=vn"));
            assertTrue(decoded.contains("vnp_OrderType=other"));
            assertTrue(decoded.contains("vnp_SecureHash=SEC123"));
        }
    }

    @Test
    void shouldIncludeOptionalParamsWhenPresent() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("10.0.0.2");
        when(req.getParameter("bankcode")).thenReturn("NCB");
        when(req.getParameter("ordertype")).thenReturn("billpayment");
        when(req.getParameter("language")).thenReturn("en");

        Buyer buyer = new Buyer();
        buyer.setBuyerId(1L);
        buyer.setUsername("alice");
        when(buyerService.getCurrentUser()).thenReturn(buyer);

        try (MockedStatic<VnPayConfig> mocked = mockStatic(VnPayConfig.class)) {
            mocked.when(() -> VnPayConfig.getRandomNumber(8)).thenReturn("ABCDEFGH");
            mocked.when(() -> VnPayConfig.hmacSHA512(any(), any())).thenReturn("HASHED");

            Map<String, Object> result = vnPayService.createPaymentUrl(req, 1L);
            String url = (String) result.get("url_payment");
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);

            assertTrue(decoded.contains("vnp_BankCode=NCB"));
            assertTrue(decoded.contains("vnp_OrderType=billpayment"));
            assertTrue(decoded.contains("vnp_Locale=en"));
        }
    }

    @Test
    void shouldVerifyReturnSuccessWhenHashMatchesAndCode00() {
        HttpServletRequest req = mock(HttpServletRequest.class);

        Map<String, String[]> params = Map.of(
                "vnp_TxnRef", new String[]{"REF001"},
                "vnp_ResponseCode", new String[]{"00"},
                "vnp_Amount", new String[]{"100"},
                "vnp_SecureHash", new String[]{"VALIDHASH"}
        );

        when(req.getParameterMap()).thenReturn(params);

        try (MockedStatic<VnPayConfig> mocked = mockStatic(VnPayConfig.class)) {
            mocked.when(() -> VnPayConfig.hmacSHA512(any(), any())).thenReturn("VALIDHASH");

            Map<String, Object> result = vnPayService.processReturn(req);

            assertEquals(true, result.get("success"));
            assertEquals("REF001", result.get("transaction_code"));
            assertEquals("Xác minh thành công", result.get("message"));
            assertEquals("00", result.get("response_code"));
        }
    }

    @Test
    void shouldRejectReturnWhenSecureHashMismatches() {
        HttpServletRequest req = mock(HttpServletRequest.class);

        Map<String, String[]> params = Map.of(
                "vnp_TxnRef", new String[]{"REFXYZ"},
                "vnp_ResponseCode", new String[]{"00"},
                "vnp_Amount", new String[]{"500"},
                "vnp_SecureHash", new String[]{"RECEIVEDHASH"}
        );

        when(req.getParameterMap()).thenReturn(params);

        try (MockedStatic<VnPayConfig> mocked = mockStatic(VnPayConfig.class)) {
            mocked.when(() -> VnPayConfig.hmacSHA512(any(), any())).thenReturn("COMPUTEDHASH");

            Map<String, Object> result = vnPayService.processReturn(req);

            assertEquals(false, result.get("success"));
            assertEquals("REFXYZ", result.get("transaction_code"));
            assertEquals("Mã bảo mật không hợp lệ", result.get("message"));
            assertEquals("RECEIVEDHASH", result.get("vnp_secureHash"));
            assertEquals("COMPUTEDHASH", result.get("sign_value"));
            assertEquals("00", result.get("response_code"));
        }
    }

    @Test
    void shouldHandleFailureResponseCodeWithValidHash() {
        HttpServletRequest req = mock(HttpServletRequest.class);

        Map<String, String[]> params = Map.of(
                "vnp_TxnRef", new String[]{"REF002"},
                "vnp_ResponseCode", new String[]{"24"},
                "vnp_Amount", new String[]{"100"},
                "vnp_SecureHash", new String[]{"VALIDHASH"}
        );

        when(req.getParameterMap()).thenReturn(params);

        try (MockedStatic<VnPayConfig> mocked = mockStatic(VnPayConfig.class)) {
            mocked.when(() -> VnPayConfig.hmacSHA512(any(), any())).thenReturn("VALIDHASH");

            Map<String, Object> result = vnPayService.processReturn(req);

            assertEquals(false, result.get("success"));
            assertEquals("Thanh toán thất bại!", result.get("message"));
            assertEquals("24", result.get("response_code"));
            assertEquals("REF002", result.get(""));
        }
    }

    @Test
    void shouldExcludeEmptyParamsFromHashAndQuery() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("192.168.1.10");
        when(req.getParameter("bankcode")).thenReturn("");
        when(req.getParameter("ordertype")).thenReturn(null);
        when(req.getParameter("language")).thenReturn("");

        Buyer buyer = new Buyer();
        buyer.setBuyerId(5L);
        buyer.setUsername("bob");
        when(buyerService.getCurrentUser()).thenReturn(buyer);

        try (MockedStatic<VnPayConfig> mocked = mockStatic(VnPayConfig.class)) {
            mocked.when(() -> VnPayConfig.getRandomNumber(8)).thenReturn("12345678");
            mocked.when(() -> VnPayConfig.hmacSHA512(any(), any())).thenReturn("SIGNEDHASH");

            Map<String, Object> result = vnPayService.createPaymentUrl(req, 2L);
            String url = (String) result.get("url_payment");
            String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);

            assertFalse(decoded.contains("vnp_BankCode="));
            assertTrue(decoded.contains("vnp_OrderType=other"));
            assertTrue(decoded.contains("vnp_SecureHash=SIGNEDHASH"));
        }
    }
}
