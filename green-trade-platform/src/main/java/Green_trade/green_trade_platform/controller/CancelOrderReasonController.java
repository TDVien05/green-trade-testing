package Green_trade.green_trade_platform.controller;

import Green_trade.green_trade_platform.mapper.CancelOrderReasonMapper;
import Green_trade.green_trade_platform.mapper.ResponseMapper;
import Green_trade.green_trade_platform.model.CancelOrderReason;
import Green_trade.green_trade_platform.response.CancelOrderReasonResponse;
import Green_trade.green_trade_platform.response.RestResponse;
import Green_trade.green_trade_platform.service.implement.CancelOrderReasonServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cancel-order-reason")
public class CancelOrderReasonController {

    private final CancelOrderReasonServiceImpl cancelOrderReasonService;
    private final CancelOrderReasonMapper cancelOrderReasonMapper;
    private final ResponseMapper responseMapper;

    public CancelOrderReasonController(CancelOrderReasonServiceImpl cancelOrderReasonService, CancelOrderReasonMapper cancelOrderReasonMapper, ResponseMapper responseMapper) {
        this.cancelOrderReasonService = cancelOrderReasonService;
        this.cancelOrderReasonMapper = cancelOrderReasonMapper;
        this.responseMapper = responseMapper;
    }

    @Operation(
            summary = "Lấy danh sách lý do hủy đơn hàng",
            description = "Trả về toàn bộ danh sách lý do hủy đơn hàng hiện có trong hệ thống. "
                    + "Dữ liệu trả về bao gồm `id` và `cancelOrderReasonName` của từng lý do.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lấy danh sách lý do hủy đơn hàng thành công",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = CancelOrderReasonResponse.class)
                                    )
                            )
                    )
            }
    )
    @GetMapping("")
    public ResponseEntity<RestResponse<?, ?>> getAllCancelOrderReasons() {
        List<CancelOrderReason> result = cancelOrderReasonService.getAllCancelOrderReasons();
        List<CancelOrderReasonResponse> responseData = new ArrayList<>();

        result.forEach(cancelOrderReason -> {
            responseData.add(cancelOrderReasonMapper.toDto(cancelOrderReason));
        });
        return ResponseEntity.status(HttpStatus.OK.value()).body(responseMapper.toDto(
                true,
                "FETCH CANCEL ORDER REASONS SUCCESSFULLY",
                responseData,
                null
        ));
    }
}
