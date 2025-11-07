package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.request.NeedVerifyPostRequest;
import Green_trade.green_trade_platform.request.PostProductDecisionRequest;
import Green_trade.green_trade_platform.request.UploadPostProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostProductService {
    PostProduct createNewPostProduct(
            UploadPostProductRequest request,
            List<MultipartFile> files
    ) throws Exception;

    Page<PostProduct> getAllProductPaging(int page, int size, String sortedBy, boolean isAsc);

    Page<PostProduct> getAllPostProductForVerifiedReview(int size, int page) throws Exception;

    PostProduct getPostProductById(Long postProductId) throws Exception;

    PostProduct checkPostProductVerification(PostProductDecisionRequest request) throws Exception;
}
