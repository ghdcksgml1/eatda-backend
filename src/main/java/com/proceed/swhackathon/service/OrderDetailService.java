package com.proceed.swhackathon.service;

import com.proceed.swhackathon.dto.menuOption.MenuOptionDTO;
import com.proceed.swhackathon.dto.orderDetail.OrderDetailDTO;
import com.proceed.swhackathon.dto.orderDetail.OrderDetailInsertDTO;
import com.proceed.swhackathon.dto.userOrderDetail.UserOrderDetailDTO;
import com.proceed.swhackathon.dto.userOrderDetail.UserOrderDetailResponseDTO;
import com.proceed.swhackathon.exception.Message;
import com.proceed.swhackathon.exception.coupon.CouponUseNotFoundException;
import com.proceed.swhackathon.exception.menu.MenuNotFoundException;
import com.proceed.swhackathon.exception.menu.MenuNotMatchingStoreException;
import com.proceed.swhackathon.exception.menuOption.MenuOptionNotFoundException;
import com.proceed.swhackathon.exception.order.OrderNotFoundException;
import com.proceed.swhackathon.exception.order.OrderStatusException;
import com.proceed.swhackathon.exception.payment.PaymentNotFoundException;
import com.proceed.swhackathon.exception.payment.PaymentStatusException;
import com.proceed.swhackathon.exception.user.UserNotFoundException;
import com.proceed.swhackathon.exception.user.UserUnAuthorizedException;
import com.proceed.swhackathon.exception.userOrderDetail.UserOrderDetailNotFoundException;
import com.proceed.swhackathon.model.*;
import com.proceed.swhackathon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderDetailService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MenuRepository menuRepository;
    private final MenuOptionRepository menuOptionRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserOrderDetailRepository userOrderDetailRepository;
    private final OrderDetailOptionRepository orderDetailOptionRepository;
    private final CouponUseRepository couponUseRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public OrderDetailDTO insertOrderDetail(String userId,
                                            Long orderId,
                                            Long menuId,
                                            OrderDetailInsertDTO orderDetailDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException();
        });
        Order order = orderRepository.findOrderByIdWithStore(orderId).orElseThrow(() -> {
            throw new OrderNotFoundException();
        });
        Menu menu = menuRepository.findByMenuIdWithStore(menuId).orElseThrow(() -> {
            throw new MenuNotFoundException();
        });

        if(order.getStore() != menu.getStore())
            throw new MenuNotMatchingStoreException();

                /*
         ??????, OrderDetail??? Order??? ?????? true ?????? ???????????????, ?????? false??? ?????????.
         */
        List<OrderDetail> orderDetails = orderDetailRepository.findAllByMenuCheckIsFalseAndOrder(user, order);
        for(OrderDetail od : orderDetails){
            log.info("findAllByMenuCheckIsFalseAndOrder execute! : ?????? ??????????????? ???????????????.");
            od.setMenuCheck(false);
        }

        /*
         ????????? ????????? ?????? ????????? ????????? ???????????? ???????????? ?????? (??????)
         */
//        Optional<OrderDetail> orderDetail = orderDetailRepository.findByUserAndOrderAndMenuV2(user, order, menu);
        OrderDetail od;
//        if(orderDetail.isEmpty()) {
            od = OrderDetail.builder() // ????????? ?????? ??????
                    .quantity(orderDetailDTO.getQuantity())
                    .menuCheck(true)
                    .build();
            od.setUser(user);
            od.setMenu(menu);
            od.setOrder(order);
//        }else{
//            od = orderDetail.get();
//            od.setMenuCheck(true);
//            od.setQuantity(od.getQuantity() + orderDetailDTO.getQuantity());
//        }

        List<OrderDetailOption> orderDetailOptions = new ArrayList<>();
        for(MenuOptionDTO mod : orderDetailDTO.getMenuOptions()){
            // menuOption??? id??? 0???????????? ???????????? ?????? ?????????.
            if(mod.getMenuOption_id() == 0) continue;

            MenuOption menuOption = menuOptionRepository.findById(mod.getMenuOption_id()).orElseThrow(() -> {
                throw new MenuOptionNotFoundException();
            });

            OrderDetailOption orderDetailOption = OrderDetailOption.builder()
                    .menuOption(menuOption)
                    .orderDetail(od)
                    .build();
            orderDetailOptions.add(orderDetailOptionRepository.save(orderDetailOption));
        }
        od.setOrderDetailOptions(orderDetailOptions); // orderDetailOption ????????????
        od.calTotalPrice(); // price ?????? ??????

        OrderDetail save = orderDetailRepository.save(od);

        return OrderDetailDTO.entityToDTO(save);
    }

    public List<OrderDetailDTO> selectCart(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException();
        });

        return orderDetailRepository.findByUser(user)
                .stream()
                .map(OrderDetailDTO::entityToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public String updateMenuCheck(String userId, Long orderDetailId){

        OrderDetail ods = orderDetailRepository.findById(orderDetailId).orElseThrow(() -> {
            throw new UserOrderDetailNotFoundException();
        });

        if(!ods.getUser().getId().equals(userId)) // ?????? ???????????? ?????? ??????
            throw new UserUnAuthorizedException();

        if(ods == null) return "????????? ?????? ???????????????.";
        else
            return ods.triggerCheck() ? "????????? ??????????????????." : "????????? ????????????????????????.";
    }

    @Transactional
    public String updateQuantity(String userId, Long orderDetailId, int quantity){
        OrderDetail ods = orderDetailRepository.findById(orderDetailId).orElseThrow(() -> {
            throw new UserOrderDetailNotFoundException();
        });

        if(!ods.getUser().getId().equals(userId)) // ?????? ???????????? ?????? ??????
            throw new UserUnAuthorizedException();

        ods.setQuantity(quantity);
        ods.calTotalPrice();

        return "????????? ??????????????????.";
    }

    @Transactional
    public Long addOrder(String userId, Long orderId, String paymentId) {
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("{}", Message.USER_NOT_FOUND);
            throw new UserNotFoundException();
        });
        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
            log.warn("{}", Message.ORDER_NOT_FOUND);
            throw new OrderNotFoundException();
        });

        // ?????? ?????? ??????
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> {
            log.warn("{}", "????????? ?????????????????????.");
            throw new PaymentNotFoundException();
        });
        if(!payment.getStatus().equals("DONE")){
            log.warn("{}", Message.PAYMENT_STATUS);
            throw new PaymentStatusException();
        }

        if(order.getOrderStatus() != OrderStatus.WAITING){
            log.warn("{}", Message.ORDER_STATUS);
            throw new OrderStatusException();
        }
        //

        log.info("1. ????????? OrderDetail??? ?????? ??????");
        List<OrderDetail> byUserAndOrder = orderDetailRepository.findByUserAndOrder(user, order);
        log.info("2. OrderDetail??? ???????????? ????????? ???????????? ??????");
        if(byUserAndOrder.isEmpty()){
            log.warn("{}", Message.USER_ORDER_DETAIL_NOT_FOUND);
            throw new UserOrderDetailNotFoundException();
        }
        log.info("3. uod Building");
        // ?????? ??????????????? ???????????? ?????? ?????? (?????? ????????? ????????? ????????? ????????? ??? ????????? ?????? ??????)
//        UserOrderDetail result = userOrderDetailRepository.findByOrderAndUserWithOrder(order, user).orElse(null);
//        log.info("4");
//        if(result != null){
//            result.cancel();
//            userOrderDetailRepository.delete(result);
//        }

        UserOrderDetail uod = UserOrderDetail.builder()
                .order(order)
                .user(user)
                .build();
        log.info("4. save??????");
        UserOrderDetail save = userOrderDetailRepository.save(uod);

        payment.setUserOrderDetail_id(save.getId()); // payment??? userOrderDetail_id ????????????.

        return save.getId();
    }

    @Transactional
    public UserOrderDetailDTO detachUOD(String userId, Long orderId, Long uodId, Long couponUseId){
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("{}", Message.USER_NOT_FOUND);
            throw new UserNotFoundException();
        });
        Order order = orderRepository.findOrderByIdWithStore(orderId).orElseThrow(() -> {
            log.warn("{}", Message.ORDER_NOT_FOUND );
            throw new OrderNotFoundException();
        });
        UserOrderDetail uod = userOrderDetailRepository.findById(uodId).orElseThrow(() -> {
            log.warn("{}", Message.USER_ORDER_DETAIL_NOT_FOUND );
            throw new UserOrderDetailNotFoundException();
        });

        // CouponUse??? UserOrderDetail ??????
        if(couponUseId != 0) {
            CouponUse couponUse = couponUseRepository.findById(couponUseId).orElseThrow(() -> {
                log.warn("{}", Message.COUPONUSE_NOT_FOUND);
                throw new CouponUseNotFoundException();
            });
            couponUse.couponUsing(uod); // ?????? ??????
        }

        // ??????????????? true??? ??????????????? ?????????.
        List<OrderDetail> ods = orderDetailRepository.findByUserAndOrder(user, order);
        for (OrderDetail od : ods) {
            od.setUserOrderDetail(uod);
        }

        uod.setOrderDetails(ods);
        uod.calTotalPrice(); // ??? ??????

        // ?????? ?????? ?????? ??????
        order.getStore().setRecentlyOrder(order);

        // ?????? ????????? ???????????? ???????????? ?????? menuCheck??? ???????????????.
        for (OrderDetail od : ods){
            od.setMenuCheck(false);
        }

        return UserOrderDetailDTO.entityToDTO(uod);
    }

    public List<UserOrderDetailDTO> selectUOD(String userId, Long orderId){
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException();
        });
        Order order = orderRepository.findById(orderId).orElseThrow(() -> {
            throw new OrderNotFoundException();
        });

        List<UserOrderDetailDTO> uodDTO = new ArrayList<>();

        List<UserOrderDetail> uods = userOrderDetailRepository.findAllByOrderAndUser(order, user).orElseThrow(() -> {
            throw new UserOrderDetailNotFoundException();
        });

        for(UserOrderDetail uod : uods) {
            List<OrderDetail> ods = orderDetailRepository.selectUOD(uod, user);
//        ods.removeIf(od -> !od.isMenuCheck());
            uod.setOrderDetails(ods);
            Payment payment = paymentRepository.findByUserOrderDetail_id(uod.getId());
            if(payment != null){
                CouponUse couponUse = couponUseRepository.findById(payment.getCouponUse_id()).orElseThrow(() -> {
                    log.warn("{}", Message.COUPONUSE_NOT_FOUND);
                    throw new CouponUseNotFoundException();
                });
                uodDTO.add(UserOrderDetailDTO.entityToDTO(uod, couponUse.getCoupon()));
            }else{
                uodDTO.add(UserOrderDetailDTO.entityToDTO(uod));
            }
        }

        return uodDTO;
    }

    public List<UserOrderDetailResponseDTO> selectUODAll(String userId){
        User user = userRepository.findById(userId).orElseThrow(() -> {
            throw new UserNotFoundException();
        });

        log.info("selectUODAll is Running...");

        return userOrderDetailRepository.findAllByUserWithOrderOrderByIdDesc(user)
                .stream().map(UserOrderDetailResponseDTO::entityToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void garbageCollectOrderDetail(){
        List<OrderDetail> ods = orderDetailRepository.deleteAllByMenuCheckIsFalseAndUserOrderDetailIsNull();
        for(OrderDetail od : ods){
            log.info("delete OrderDetail ID : {}", od.getId());
        }
    }
}
