package Green_trade.green_trade_platform.service.implement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {
    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload file lên Cloudinary, trả về secure_url (String).
     *
     * @param file   MultipartFile từ request
     * @param folder folder trên Cloudinary (ví dụ: "sellers/123")
     * @return secure_url
     */
    public Map<String, String> upload(MultipartFile file, String folder) throws IOException {
        String publicId = UUID.randomUUID().toString();
        Map<?, ?> res = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "public_id", publicId,
                        "resource_type", "auto"
                )
        );

        String fileUrl = res.get("secure_url") != null
                ? res.get("secure_url").toString()
                : res.get("url") != null
                ? res.get("url").toString()
                : null;

        return fileUrl != null ? Map.of(
                "fileUrl", fileUrl,
                "publicId", publicId
        ) : null;
    }

    public boolean delete(String publicId, String folder) {
        try {
            String fullPublicId = folder != null && !folder.isEmpty()
                    ? folder + "/" + publicId
                    : publicId;

            Map<?, ?> res = cloudinary.uploader().destroy(
                    fullPublicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            Object result = res.get("result");
            return "ok".equals(result); // Cloudinary trả về {"result": "ok"} nếu xoá thành công
        } catch (Exception e) {
            log.error("Delete image failed for public_id={} in folder={}, error={}", publicId, folder, e.getMessage());
            return false;
        }
    }

    public Map<String, String> uploadFile(File file, String folder) {
        try {
            Map<?, ?> res = cloudinary.uploader().upload(
                    file,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto",  // ✅ đổi từ "raw" sang "auto" để Cloudinary tự nhận PDF
                            "type", "upload",          // giữ nguyên: upload trực tiếp
                            "access_mode", "public",   // cho phép truy cập công khai
                            "use_filename", true,      // dùng tên file gốc
                            "unique_filename", false   // không thêm hậu tố ngẫu nhiên
                    )
            );

            log.info("📤 Cloudinary upload result: {}", res);

            String fileUrl = res.get("secure_url") != null
                    ? res.get("secure_url").toString()
                    : res.get("url") != null
                    ? res.get("url").toString()
                    : null;

            return fileUrl != null ? Map.of(
                    "fileUrl", fileUrl,
                    "publicId", res.get("public_id").toString()
            ) : null;

        } catch (Exception e) {
            log.error("❌ Upload file to Cloudinary failed: {}", e.getMessage(), e);
            return null;
        }
    }


}
