package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理付款时超时的订单
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟触发一次
    public void processOutTimeOrder(){
        log.info("处理付款超时的订单：{}", LocalDateTime.now());

        //下单时间小于当前时间减去15分钟
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        List<Orders> orders = orderMapper.getByStautsAndTime(Orders.PENDING_PAYMENT,time);

        if (orders != null && orders.size() > 0){
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单付款超时，自动取消");

                orderMapper.update(order);
            }

        }
    }

    /**
     * 处理一直在派送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨1点触发一次
    public void processDeliveryOrder(){
        log.info("处理一直在派送中状态的订单：{}", LocalDateTime.now());

        //凌晨1点减去1个小时 = 24点 也就是处理前一天所有处于派送中的订单
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orders = orderMapper.getByStautsAndTime(Orders.DELIVERY_IN_PROGRESS,time);

        if (orders != null && orders.size() > 0){
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
