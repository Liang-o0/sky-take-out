package com.sky.controller.user;

import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "用户订单相关接口")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);

        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);

        //处理微信支付问题，个人添加的代码：orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());

        return Result.success(orderPaymentVO);
    }

    /**
     * 查看订单历史
     * @param pageSize
     * @return
     */
    @ApiOperation("查看订单历史")
    @GetMapping("/historyOrders")
    public Result<PageResult> getOrderHistory(int page, int pageSize , Integer status){
       PageResult pageResult = orderService.historyOrders(page, pageSize, status);
       return Result.success(pageResult);
    }

    /**
     * @param id
     * @return
     */
    @ApiOperation("查询详细订单信息")
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> getOrderDetails(@PathVariable("id") Long id){
       OrderVO details = orderService.getOrderDetails(id);
       return Result.success(details);
    }

    /**
     * 根据id取消订单
     * @param id
     * @return
     */
    @ApiOperation("取消订单")
    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable("id") Long id) throws Exception{
        orderService.userCancel(id);
        return Result.success();
    }

    @ApiOperation("再来一单")
    @PostMapping("/repetition/{id}")
    public Result orderOneMore(@PathVariable("id") Long id){
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 客户催单
     * @param id
     */
    @ApiOperation(("客户催单"))
    @GetMapping("/reminder/{id}")
    public Result reminderOrder(@PathVariable("id") Long id){
        orderService.reminder(id);
        return Result.success();
    }
}
