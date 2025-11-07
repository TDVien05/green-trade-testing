package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.response.PostProductListResponse;
import Green_trade.green_trade_platform.response.PostProductResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PostProductListMapper {
    private final PostProductMapper postProductMapper;

    public PostProductListMapper(PostProductMapper postProductMapper) {
        this.postProductMapper = postProductMapper;
    }

    public PostProductListResponse toDto(List<PostProduct> postProducts, Map<String, Object> meta) {
        List<PostProductResponse> postProductListResponses = postProducts.stream()
                .map(
                        postProduct -> postProductMapper.toDto(postProduct)
                )
                .toList();
        return PostProductListResponse.builder()
                .postList(postProductListResponses)
                .meta(meta)
                .build();
    }
}
