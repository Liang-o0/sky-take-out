package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
@Api("订单管理接口")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 根据条件查询订单,订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @ApiOperation("根据条件查询订单")
    @GetMapping("/conditionSearch")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
            PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
            return Result.success(pageResult);
    }

    /**
     * 获取订单各个状态的数量信息
     * @return
     */
    @ApiOperation("获取订单各个状态的数量信息")
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics(){
            OrderStatisticsVO result = orderService.statistics();
            return Result.success(result);
    }

    /**
     * 获取订单详情
     * @return
     */
    @ApiOperation("获取订单详情")
    @GetMapping("/details/{id}")
    public Result<OrderVO> getOrderDetails(@PathVariable("id") Long id){

        OrderVO orderDetails = orderService.getOrderDetails(id);
        return Result.success(orderDetails);
    }

    /**
     * 进行接单操作
     * @param ordersConfirmDTO
     * @return
     */
    @ApiOperation("进行接单操作")
    @PutMapping("/confirm")
    public Result confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO){

        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 拒单操作
     * @param ordersRejectionDTO
     * @return
     */
    @ApiOperation("进行拒单操作")
    @PutMapping("/rejection")
    public Result rejectionOrder(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception{
            orderService.rejection(ordersRejectionDTO);
            return Result.success();
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     * @return
     */
    @ApiOperation("取消订单")
    @PutMapping("/cancel")
    public Result cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO){
        orderService.adminCancel(ordersCancelDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @ApiOperation("派送订单")
    @PutMapping("/delivery/{id}")
    public Result deliveryOrder(@PathVariable("id") Long id){
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @ApiOperation("完成订单")
    @PutMapping("/complete/{id}")
    public Result completeOrder(@PathVariable("id") Long id) throws Exception{
        orderService.complete(id);
        return Result.success();
    }

}
