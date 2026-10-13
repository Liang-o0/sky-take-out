package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
     Orders getById(Long id);

    /**
     * 插入1条订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据分页请求参数查询历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> getByPageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据状态查询订单数量（待接单、待派送，派送中）
      * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据状态和时间查询支付超时的订单
     * @param status
     * @param time
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStautsAndTime(Integer status, LocalDateTime time);


    /**
     * 根据条件营业额
     * @param
     * @return
     */
    //Double getByMap(LocalDateTime beginTime, LocalDateTime endTime, Integer status);
    Double getByMap(Map<String,Object> map);

    /**
     * 根据条件查询用户统计
     * @param map
     */
    //Integer getUserReport(OrdersPageQueryDTO ordersPageQueryDTO);
    Integer getUserReport(Map<String,Object> map);

    /**
     * 根据条件查询订单统计
     * @param map
     * @return
     */
    Integer getOrderReport(Map<String,Object> map);

    /**
     * 查询销量排名top10接口
     * @param beginTime
     * @param endTime
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
