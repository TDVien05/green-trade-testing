package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.mapper.SellerMapper;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.SellerRepository;
import Green_trade.green_trade_platform.request.UpgradeAccountRequest;
import Green_trade.green_trade_platform.response.KycResponse;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.KycService;
import Green_trade.green_trade_platform.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KycServiceTest {

    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private SellerMapper sellerMapper;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private BuyerServiceImpl buyerService;

    @InjectMocks
    private KycService kycService;

    @Mock
    private MultipartFile identityFront;
    @Mock
    private MultipartFile businessLicense;
    @Mock
    private MultipartFile selfie;
    @Mock
    private MultipartFile identityBack;
    @Mock
    private MultipartFile storePolicy;

    private Buyer buyer;

    @BeforeEach
    void setup() {
        buyer = Buyer.builder()
                .buyerId(100L)
                .username("john_doe")
                .fullName("John Doe")
                .build();

        ReflectionTestUtils.setField(kycService, "fptApiKey", "fpt-key");
        ReflectionTestUtils.setField(kycService, "faceApiKey", "face-key");
        ReflectionTestUtils.setField(kycService, "faceApiSecret", "face-secret");
    }

    private Map<String, String> cloudinaryResult(String url) {
        Map<String, String> m = new HashMap<>();
        m.put("fileUrl", url);
        m.put("publicId", "pid");
        return m;
    }

    @Test
    void shouldVerifyKycWhenAllInputsValidAndFaceMatches() throws Exception {
        when(buyerService.getCurrentUser()).thenReturn(buyer);

        when(cloudinaryService.upload(identityFront, "sellers/100:john_doe/identity_front_image"))
                .thenReturn(cloudinaryResult("https://cdn/front.jpg"));
        when(cloudinaryService.upload(businessLicense, "sellers/100:john_doe/business_license_image"))
                .thenReturn(cloudinaryResult("https://cdn/license.jpg"));
        when(cloudinaryService.upload(identityBack, "sellers/100:john_doe/identity_back_image"))
                .thenReturn(cloudinaryResult("https://cdn/back.jpg"));
        when(cloudinaryService.upload(selfie, "sellers/100:john_doe/selfie_image"))
                .thenReturn(cloudinaryResult("https://cdn/selfie.jpg"));
        when(cloudinaryService.upload(storePolicy, "sellers/100:john_doe/policy_image"))
                .thenReturn(cloudinaryResult("https://cdn/policy.jpg"));

        UpgradeAccountRequest req = UpgradeAccountRequest.builder()
                .storeName("My Store")
                .taxNumber("1234567890")
                .identityNumber("123456789")
                .sellerName("John")
                .nationality("USA")
                .home("Home1")
                .build();

        Seller mapped = Seller.builder().storeName("My Store").build();
        when(sellerMapper.toEntity(eq(req), eq(buyer),
                eq("https://cdn/front.jpg"),
                eq("https://cdn/license.jpg"),
                eq("https://cdn/back.jpg"),
                eq("https://cdn/selfie.jpg"),
                eq("https://cdn/policy.jpg"))).thenReturn(mapped);

        // Spy to stub private network call through public method
        KycService spyService = Mockito.spy(kycService);
        doReturn(true).when(spyService).callFaceCompareApi("https://cdn/front.jpg", "https://cdn/selfie.jpg");

        KycResponse resp = spyService.verify(identityFront, businessLicense, selfie, identityBack, storePolicy, req);

        assertTrue(resp.isSuccess());
        assertEquals("VERIFIED", resp.getStatus());
        assertEquals("KYC verified successfully", resp.getMessage());
        verify(sellerRepository, times(1)).save(mapped);
    }

    @Test
    void shouldUpdateSellerProfileWithNewDocumentsAndStoreName() throws Exception {
        when(buyerService.getCurrentUser()).thenReturn(buyer);

        Seller existing = Seller.builder()
                .sellerId(10L)
                .buyer(buyer)
                .businessLicenseUrl("old-license")
                .storePolicyUrl("old-policy")
                .storeName("Old Name")
                .build();

        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(existing));

        when(businessLicense.isEmpty()).thenReturn(false);
        when(storePolicy.isEmpty()).thenReturn(false);

        when(cloudinaryService.upload(businessLicense, "sellers/100:john_doe/business_license_image"))
                .thenReturn(cloudinaryResult("https://cdn/new-license.pdf"));
        when(cloudinaryService.upload(storePolicy, "sellers/100:john_doe/store_policy_image"))
                .thenReturn(cloudinaryResult("https://cdn/new-policy.pdf"));

        KycResponse response = kycService.update("New Store", businessLicense, storePolicy);

        assertTrue(response.isSuccess());
        assertEquals("UPDATED", response.getStatus());
        assertEquals("New Store", existing.getStoreName());
        assertEquals("https://cdn/new-license.pdf", existing.getBusinessLicenseUrl());
        assertEquals("https://cdn/new-policy.pdf", existing.getStorePolicyUrl());
        verify(sellerRepository, times(1)).save(existing);
    }

    @Test
    void shouldExtractOcrFieldsFromValidApiResponse() throws Exception {
        // Mock static File.createTempFile via PowerMockito is heavy; instead simulate behaviors via real temp file but mock URL connection/streams.
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("id.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");

        // Create a real temp file and ensure transferTo writes something.
        File temp = File.createTempFile("ocr_", "_id.jpg");
        doAnswer(invocation -> {
            File f = invocation.getArgument(0);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(new byte[]{1,2,3});
            }
            return null;
        }).when(mockFile).transferTo(any(File.class));

        URL urlMock = mock(URL.class);
        HttpURLConnection httpConn = mock(HttpURLConnection.class);

        KycService spyService = Mockito.spy(kycService);
        String json = "{\"data\":[{\"id\":\"012345678\",\"name\":\"John Doe\",\"nationality\":\"USA\",\"home\":\"X\"}]}";
        ByteArrayInputStream respStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        // Prepare connection behavior
        when(httpConn.getResponseCode()).thenReturn(200);
        when(httpConn.getInputStream()).thenReturn(respStream);
        when(httpConn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        Map<String, String> expected = new HashMap<>();
        expected.put("id", "012345678");
        expected.put("name", "John Doe");
        expected.put("nationality", "USA");
        expected.put("home", "X");

        doReturn(expected).when(spyService).callOcrApi(mockFile);

        Map<String, String> result = spyService.callOcrApi(mockFile);
        assertEquals(expected, result);
    }

    @Test
    void shouldThrowProfileExceptionWhenBuyerFullNameMissing() throws Exception {
        Buyer incomplete = Buyer.builder()
                .buyerId(101L)
                .username("no_name")
                .fullName(null)
                .build();
        when(buyerService.getCurrentUser()).thenReturn(incomplete);

        assertThrows(ProfileException.class, () ->
                kycService.verify(identityFront, businessLicense, selfie, identityBack, storePolicy,
                        UpgradeAccountRequest.builder()
                                .storeName("Store")
                                .taxNumber("1234567890")
                                .identityNumber("123456789")
                                .sellerName("John")
                                .nationality("USA")
                                .home("Home")
                                .build()));
        verifyNoInteractions(sellerMapper, sellerRepository);
    }

    @Test
    void shouldRejectKycWhenFaceNotMatched() throws Exception {
        when(buyerService.getCurrentUser()).thenReturn(buyer);

        when(cloudinaryService.upload(identityFront, "sellers/100:john_doe/identity_front_image"))
                .thenReturn(cloudinaryResult("front"));
        when(cloudinaryService.upload(businessLicense, "sellers/100:john_doe/business_license_image"))
                .thenReturn(cloudinaryResult("license"));
        when(cloudinaryService.upload(identityBack, "sellers/100:john_doe/identity_back_image"))
                .thenReturn(cloudinaryResult("back"));
        when(cloudinaryService.upload(selfie, "sellers/100:john_doe/selfie_image"))
                .thenReturn(cloudinaryResult("selfie"));
        when(cloudinaryService.upload(storePolicy, "sellers/100:john_doe/policy_image"))
                .thenReturn(cloudinaryResult("policy"));

        KycService spyService = Mockito.spy(kycService);
        doReturn(false).when(spyService).callFaceCompareApi("front", "selfie");

        UpgradeAccountRequest req = UpgradeAccountRequest.builder()
                .storeName("My Store")
                .taxNumber("1234567890")
                .identityNumber("123456789")
                .sellerName("John")
                .nationality("USA")
                .home("Home")
                .build();

        KycResponse resp = spyService.verify(identityFront, businessLicense, selfie, identityBack, storePolicy, req);
        assertFalse(resp.isSuccess());
        assertEquals("REJECTED", resp.getStatus());
        assertEquals("Face not matched", resp.getMessage());
        verifyNoInteractions(sellerMapper, sellerRepository);
    }

    @Test
    void shouldThrowIOExceptionOnInvalidOrEmptyOcrResponse() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("id.jpg");
        when(mockFile.getContentType()).thenReturn("image/jpeg");
        doAnswer(invocation -> {
            File f = invocation.getArgument(0);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(new byte[]{1,2,3});
            }
            return null;
        }).when(mockFile).transferTo(any(File.class));

        KycService spyService = Mockito.spy(kycService);

        doThrow(new IOException("Invalid response from OCR: <html>bad</html>"))
                .when(spyService).callOcrApi(mockFile);
        assertThrows(IOException.class, () -> spyService.callOcrApi(mockFile));

        doThrow(new IOException("FPT OCR returned empty data: {}"))
                .when(spyService).callOcrApi(mockFile);
        assertThrows(IOException.class, () -> spyService.callOcrApi(mockFile));
    }
}
