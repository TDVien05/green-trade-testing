package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.mapper.SellerMapper;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.SellerRepository;
import Green_trade.green_trade_platform.request.UpgradeAccountRequest;
import Green_trade.green_trade_platform.response.KycResponse;
import Green_trade.green_trade_platform.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class KycService {

    private final BuyerRepository buyerRepository;
    private final CloudinaryService cloudinaryService;
    private final SellerRepository sellerRepository;
    private final SellerMapper sellerMapper;
    private final FileUtils fileUtils;
    private final BuyerServiceImpl buyerService;


    @Value("${api-key}")
    private String fptApiKey;
    @Value(("${api-key-face}"))
    private String faceApiKey;
    @Value(("${api-key-secret}"))
    private String faceApiSecret;


    public KycResponse verify(
            MultipartFile identityFrontImageUrl,
            MultipartFile businessLicenseUrl,
            MultipartFile selfieImageUrl,
            MultipartFile identityBackImageUrl,
            MultipartFile storePolicyUrl,
            UpgradeAccountRequest request
    ) throws IOException {
        Buyer buyer = buyerService.getCurrentUser();

        log.info(">>> [KYC service] username: {}", buyer.getUsername());

        // Check buyer profile
        if (buyer.getFullName() == null) {
            throw new ProfileException("Hoàn tất hồ sơ người dùng trước khi nâng cáp tài khoản");
        }

        // Upload file into Cloudinary
        Map<String, String> uploadResult = cloudinaryService.upload(
                identityFrontImageUrl, "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/identity_front_image"
        );
        String frontImageUrl = uploadResult.get("fileUrl");

        uploadResult = cloudinaryService.upload(
                businessLicenseUrl, "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/business_license_image"
        );
        String license = uploadResult.get("fileUrl");

        uploadResult = cloudinaryService.upload(
                identityBackImageUrl, "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/identity_back_image"
        );
        String backImageUrl = uploadResult.get("fileUrl");

        uploadResult = cloudinaryService.upload(
                selfieImageUrl, "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/selfie_image"
        );
        String selfieUrl = uploadResult.get("fileUrl");

        uploadResult = cloudinaryService.upload(
                storePolicyUrl, "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/policy_image"
        );
        String policyUrl = uploadResult.get("fileUrl");

        // Check face
        boolean isMatchFace = callFaceCompareApi(frontImageUrl, selfieUrl);
        if (!isMatchFace) {
            return new KycResponse(false, "Face not matched", "REJECTED", "Face verification failed");
        }

        Seller seller = sellerMapper.toEntity(request, buyer, frontImageUrl, license, backImageUrl, selfieUrl, policyUrl);
        sellerRepository.save(seller);

        return new KycResponse(true, "KYC verified successfully", "VERIFIED", null);

    }

    private Map<String, String> callOcrApi(String imageUrl) throws IOException {
        log.info(">>> Calling OCR API...");
        URL imageDownloadUrl = new URL(imageUrl);
        File tempFile = File.createTempFile("ocr", ".jpg");
        try (InputStream in = imageDownloadUrl.openStream();
             OutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL("https://api.fpt.ai/vision/idr/vnm");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api_key", fptApiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"image\"; filename=\"" + tempFile.getName() + "\"\r\n").getBytes());
            os.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());
            Files.copy(tempFile.toPath(), os);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        InputStream responseStream = conn.getResponseCode() == 200
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("FPT OCR Response: {}", response);

        if (!response.trim().startsWith("{")) {
            throw new IOException("Invalid response: " + response);
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(response, Map.class);

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.get("data");
        if (dataList == null || dataList.isEmpty()) {
            throw new IOException("FPT OCR returned empty data: " + response);
        }

        Map<String, Object> data = dataList.get(0);
        String name = (String) data.get("name");
        String idNumber = (String) data.get("id");

        return Map.of("name", name, "id_number", idNumber);
    }

    public Map<String, String> callOcrApi(MultipartFile file) throws IOException {
        log.info(">>> [KYC service] Calling FPT AI OCR API with multipart file...");

        // Create a temporary file from the uploaded multipart file
        File tempFile = File.createTempFile("ocr_", "_" + file.getOriginalFilename());
        file.transferTo(tempFile);

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL("https://api.fpt.ai/vision/idr/vnm");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api_key", fptApiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"image\"; filename=\""
                    + tempFile.getName() + "\"\r\n").getBytes());
            os.write(("Content-Type: " + file.getContentType() + "\r\n\r\n").getBytes());
            Files.copy(tempFile.toPath(), os);
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        // Read the API response
        InputStream responseStream = conn.getResponseCode() == 200
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info(">>> [KYC service] FPT OCR Response: {}", response);

        if (!response.trim().startsWith("{")) {
            throw new IOException("Invalid response from OCR: " + response);
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(response, Map.class);

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) result.get("data");
        if (dataList == null || dataList.isEmpty()) {
            throw new IOException("FPT OCR returned empty data: " + response);
        }

        Map<String, Object> data = dataList.getFirst();

        // Extract key fields safely
        String idNumber = safeGet(data, "id");
        String name = safeGet(data, "name");
        String nationality = safeGet(data, "nationality");
        String home = safeGet(data, "home"); // sometimes called "place_of_origin" or "address" depending on OCR version

        // Delete temp file
        tempFile.delete();

        Map<String, String> extracted = new HashMap<>();
        extracted.put("id", idNumber);
        extracted.put("name", name);
        extracted.put("nationality", nationality);
        extracted.put("home", home);

        log.info(">>> [KYC service] Extracted data: {}", extracted);
        return extracted;
    }

    private String safeGet(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString().trim() : "";
    }

    public KycResponse update(
            String storeName,
            MultipartFile businessLicense,
            MultipartFile storePolicy
    ) throws IOException {
        Buyer buyer = buyerService.getCurrentUser();
        log.info(">>> [UPDATE KYC] Buyer ID: {}, username: {}", buyer.getBuyerId(), buyer.getUsername());

        // Lấy seller đã tồn tại (đã KYC trước đó)
        Seller seller = sellerRepository.findByBuyer(buyer)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy hồ sơ KYC để cập nhật."));

        if (businessLicense != null && !businessLicense.isEmpty()) {
            Map<String, String> upload = cloudinaryService.upload(
                    businessLicense,
                    "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/business_license_image"
            );
            seller.setBusinessLicenseUrl(upload.get("fileUrl"));
        }

        if (storePolicy != null && !storePolicy.isEmpty()) {
            Map<String, String> upload = cloudinaryService.upload(
                    storePolicy,
                    "sellers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/store_policy_image"
            );
            seller.setStorePolicyUrl(upload.get("fileUrl"));
        }

        // Cập nhật tên cửa hàng nếu có
        if (storeName != null && !storeName.isBlank()) {
            seller.setStoreName(storeName);
        }

        // Lưu lại vào DB
        sellerRepository.save(seller);
        log.info(">>> [UPDATE KYC] Seller profile updated successfully for buyerId {}", buyer.getBuyerId());

        return new KycResponse(true, "Cập nhật KYC thành công", "UPDATED", null);
    }


    public boolean callFaceCompareApi(String idImageUrl, String selfieUrl) throws IOException {
        log.info(">>> Calling Face API...");
        URL url = new URL("https://api-us.faceplusplus.com/facepp/v3/compare");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String data = "api_key=" + faceApiKey +
                "&api_secret=" + faceApiSecret +
                "&image_url1=" + idImageUrl +
                "&image_url2=" + selfieUrl;

        // Gửi dữ liệu POST
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
        }

        // Đọc response
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        // Parse JSON bằng Jackson
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(response.toString(), Map.class);

        // Lấy confidence
        double confidence = ((Number) result.get("confidence")).doubleValue();
        return confidence > 80; // Ngưỡng match 80%
    }

    private boolean equalsIgnoreAccentAndCase(String s1, String s2) {
        if (s1 == null || s2 == null) return false;

        // Change both of string to the same form
        s1 = Normalizer.normalize(s1, Normalizer.Form.NFD);
        s2 = Normalizer.normalize(s2, Normalizer.Form.NFD);

        s1 = s1.replaceAll("\\p{M}", "");
        s2 = s2.replaceAll("\\p{M}", "");

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        s1 = s1.replaceAll("\\s+", "");
        s2 = s2.replaceAll("\\s+", "");

        return s1.equals(s2);
    }


}
