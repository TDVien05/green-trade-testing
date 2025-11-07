package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.Dispute;
import Green_trade.green_trade_platform.model.Evidence;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.ProductImage;
import Green_trade.green_trade_platform.repository.EvidenceRepository;
import Green_trade.green_trade_platform.service.EvidenceService;
import Green_trade.green_trade_platform.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EvidenceServiceImpl implements EvidenceService {
    private final FileUtils fileUtils;
    private final EvidenceRepository evidenceRepository;
    private final CloudinaryService cloudinaryService;

    public EvidenceServiceImpl(FileUtils fileUtils, EvidenceRepository evidenceRepository, CloudinaryService cloudinaryService) {
        this.fileUtils = fileUtils;
        this.evidenceRepository = evidenceRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public List<Evidence> saveEvidence(List<MultipartFile> files, Dispute dispute) throws IOException {
        log.info(">>> files data: {}", files);
        files.forEach((file) -> {
            fileUtils.validateFile(file);
            log.info(">>> Checked File name: {}", file.toString());
        });


        for (int i = 0; i <= files.size() - 1; i++) {
            Map<String, String> uploadResult = cloudinaryService.upload(files.get(i), "Evidences/" + "DisputeId" + ":" + dispute.getId() + "/evidences_image_" + i);
            String imageUrl = uploadResult.get("fileUrl");
            log.info(">>> Passed uploaded picture {}", i);
            Evidence evidenceImage = Evidence.builder()
                    .imageUrl(imageUrl)
                    .orderImage((long) i + 1)
                    .dispute(dispute)
                    .build();
            evidenceRepository.save(evidenceImage);
        }
        log.info(">>> Passed uploaded file");
        return evidenceRepository.findAllByDispute(dispute);
    }
}
