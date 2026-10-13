package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkSpaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorkSpaceServiceImpl implements WorkSpaceService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;


    /**
     * 查询今日运营数据
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime beginTime, LocalDateTime endTime) {


        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        //查询今日总订单数
        Integer totalOrderCount = orderMapper.getOrderReport(map);

        map.put("status", Orders.COMPLETED);
        //查询营业额
        Double turnover = orderMapper.getByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        //查询有效订单数
        Integer validOrderCount = orderMapper.getOrderReport(map);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //查询新增用户数
        Integer newUsers = userMapper.countByMap(map);
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 查询套餐总览
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        //查询套餐中已经启售、停售的数量
        Map<String, Object> map = new HashMap<>();
        map.put("status",1);

        //查询已启售的套餐数量
        Integer sold = setmealMapper.countByMap(map);
        map.put("status",0);

        //查询停售的套餐数量
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {

        Map<String, Object> map = new HashMap<>();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);

        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询订单管理数据
     * @return
     */
    public OrderOverViewVO getOverViewOrder() {
        Map<String, Object> map = new HashMap<>();

        //查询全部订单
        Integer allOrders = orderMapper.getOrderReport(map);

        //查询已取消的订单数量
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.getOrderReport(map);

        //查询已完成的订单数量
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.getOrderReport(map);

        //查询带派送的订单数量
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.getOrderReport(map);

        //查询带派送的订单数量
        map.put("status", Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.getOrderReport(map);

        return OrderOverViewVO.builder()
                .allOrders(allOrders)
                .cancelledOrders(cancelledOrders)
                .completedOrders(completedOrders)
                .deliveredOrders(deliveredOrders)
                .waitingOrders(waitingOrders)
                .build();

    }
}
