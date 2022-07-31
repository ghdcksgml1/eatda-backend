package com.proceed.swhackathon.controller;

import com.proceed.swhackathon.dto.OrderDTO;
import com.proceed.swhackathon.dto.OrderInsertDTO;
import com.proceed.swhackathon.dto.OrderStatusDTO;
import com.proceed.swhackathon.dto.ResponseDTO;
import com.proceed.swhackathon.model.OrderStatus;
import com.proceed.swhackathon.model.User;
import com.proceed.swhackathon.repository.UserRepository;
import com.proceed.swhackathon.service.OrderService;
import com.proceed.swhackathon.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @ApiOperation(value = "가게 주문 생성", notes = "가게의 Order주문을 오픈한다.")
    @PostMapping("/insert/{storeId}")
    public ResponseDTO<?> insert(@RequestBody OrderInsertDTO orderDTO,
                                 @PathVariable Long storeId){
        return new ResponseDTO<>(HttpStatus.OK.value(), orderService.insert(orderDTO, storeId));
    }

    @ApiOperation(value = "가게 주문 상태 변경", notes = "가게 주문상태를 변경한다.(사장만 가능)")
    @PostMapping("{orderId}/updateStatus")
    public ResponseDTO<?> updateStatus(@AuthenticationPrincipal String userId,
                                       @RequestBody OrderStatusDTO orderStatus,
                                       @PathVariable Long orderId){
        return new ResponseDTO<>(HttpStatus.OK.value(),
                orderService.updateStatus(userId, orderId, orderStatus.getOrderStatus()));
    }
}
