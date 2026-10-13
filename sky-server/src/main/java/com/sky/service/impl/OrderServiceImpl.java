package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.list;

//事务管理注解，要么所有操作都成功，要么所有操作都失败，保证数据的一致性
@Transactional
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //查询是否有业务逻辑异常，例如：地址簿为空，购物车为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null ){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart cartMapper = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        cartMapper.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.list(cartMapper);
        if (shoppingCarts == null || shoppingCarts.size() == 0){
            //抛出业务异常
            throw new RuntimeException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2.向订单表中插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setUserId(userId);
        orders.setAddress(addressBook.getDetail());
        orders.setPayStatus(Orders.PENDING_PAYMENT);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());
            //设置订单编号，使用当前系统时间戳作为订单编号
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
            //设置收货人
        orders.setConsignee(addressBook.getConsignee());
            //返回主键值ID，为下面插入订单明细表提供id
        orderMapper.insert(orders);

        //3.向订单明细中插入n条数据
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        //4.向购物车表中删除当前用户的所有购物车数据
        shoppingCartMapper.clean(userId);
        //5.返回订单确认页面需要的VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );


        //处理微信支付问题，个人单独添加的代码:JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过websocket向客户端浏览器发送消息 type orderId content
        HashMap<String, Object> map = new HashMap<>();
        map.put("type",1);//1为来单提醒 2为客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号:" + outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);

    }

    /**
     * 用户端取消订单
     * @param id
     */
    public void userCancel(Long id) throws Exception{
        //根据id查询出订单信息
        Orders orders1 = orderMapper.getById(id);

        //查看订单是否为空
        if (orders1.getStatus() == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (orders1.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //声明一个新的对象用来更新订单
        Orders orders = new Orders();
        orders.setId(orders1.getId());

        //如果处于待接单状态，则需要退款
        if (orders1.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额

            //设置支付状态为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户想要取消订单");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 查询用户历史订单
     * @return
     */
    public PageResult historyOrders(int page, int pageSize, Integer status) {
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        Long userId = BaseContext.getCurrentId() ;
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);

        //设置分页参数，进行分页条件查询
        PageHelper.startPage(page,pageSize);
        Page<Orders> ordersPage = orderMapper.getByPageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if (ordersPage != null && ordersPage.getTotal() > 0){
            for (Orders orders : ordersPage){
                Long orders_id = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders_id);

                //封装List<OrderVO>集合
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }

        }


        return new PageResult(ordersPage.getTotal(), list);
    }


    /**
     * 查询订单详情
     * @param id
     * @return
     */
    public OrderVO getOrderDetails(Long id) {
        //获取订单信息，为返回值OrderVO中的属性赋值
        Orders orders = orderMapper.getById(id);
        //获取orderDetails,为OrderVO中属性赋值
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        //封装orderVO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 再来一单
     * @param orderId
     */
    public void repetition(Long orderId) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();

        //根据当前id查询订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

        //将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCarts = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            //将原订单中的菜品信息复制到购物车对象中
            BeanUtils.copyProperties(x,shoppingCart,"id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        //将购物车对象批量到购物车表中
        shoppingCartMapper.insertBatch(shoppingCarts);
    }


    /**
     *根据条件查询订单，订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //使用PageHelper分页插件进行分页查询
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.getByPageQuery(ordersPageQueryDTO);

        //部分订单需要菜品详细信息，将Orders转换为OrderVO，拿到菜品，封装并返回
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(),orderVOList);
    }
    //详细实现上面方法中getOrderVOList(page) ，将Orders转换为OrderVO，
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        //自定义OrderVO，返回结果
        List<OrderVO> orderVOList = new ArrayList<>();

        //获取到当前页所有订单
        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)){
            //通过for循环，一个一个将Orders转换为OrderVO
            for (Orders orders : ordersList){
                OrderVO orderVO = new OrderVO();
                //将orders中的属性复制到orderVO中
                BeanUtils.copyProperties(orders,orderVO);
                //设置在OrdeVO中orderDishes的名称，根据oders中的id,拿到OrderDetail中菜品名称
                String orderDishes = getorderDishesStr(orders);

                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }
    //具体实现上面方法中的getorderDishesStr(orders)，设置OrderVO中的orderDishes属性
    private String getorderDishesStr(Orders orders) {
            List<OrderDetail> orderDetail = orderDetailMapper.getByOrderId(orders.getId());

            //将orderDetail中的菜品信息，为orderDishes名称设置
            List<String> orderDetails = orderDetail.stream().map(x -> {
                String orderDish = x.getName() + "*" + x.getNumber() + ";";
                return orderDish;
            }).collect(Collectors.toList());
             // 将该订单对应的所有菜品信息拼接在一起
            //String.join(分隔符, List<String>) = 把 List 里的多个字符串按照指定的分隔符拼接成一个字符串
            return String.join("", orderDetails);
    }


    /**
     * 获取订单各个状态的数量信息
     * @return
     */
    public OrderStatisticsVO statistics() {
        //调用orderMapper中方法countStatus,获取改订单各个状态的数量信息
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);//待接单数量
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);//待派送数量
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);//派送中数量

        //封装OrderStatisticsVO对象返回结果
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 进行接单操作
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);

    }

    /**
     * 进行拒单操作
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //根据id获取订单
        Orders ordersA = orderMapper.getById(ordersRejectionDTO.getId());
        //订单只有存在且状态为2（待接单）才可以拒单
        if (ordersA == null || !ordersA.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付装填
        Integer payStatus = ordersA.getPayStatus();
        if (payStatus != null && payStatus.equals(Orders.PAID)){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            String refund = String.valueOf(ordersA.getAmount());//因为微信退款功能实现问题，个人补充代码
            log.info("申请退款：{}", refund);

        }

        //封装Orders,更新表中信息，修改订单状态为已拒绝
        Orders orders = Orders.builder()
                .status(Orders.CANCELLED)
                .id(ordersA.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        //根据id获取订单
        Orders ordersA = orderMapper.getById(ordersCancelDTO.getId());

        //订单只有存在且状态为3（已接单）才可以取消订单
        //if (ordersA.getStatus() == null || !ordersA.getStatus().equals(Orders.CONFIRMED)){
          //  throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        //}

        //如果是已经支付状态
        Integer payStatus = ordersA.getPayStatus();
        if (payStatus != null && payStatus.equals(Orders.PAID)){
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            String refund = String.valueOf(ordersA.getAmount());//因为微信退款功能实现问题，个人补充代码
            log.info("申请退款：{}", refund);

        }

        //封装Orders,更新表中信息，修改订单状态为已取消
        Orders orders = Orders.builder()
                .id(ordersA.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        Orders ordersA = orderMapper.getById(id);

        //订单只有存在且状态为3（已接单）才可以派送订单
        if (ordersA == null || !ordersA.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //将订单状态改为派送中
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }


    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) throws Exception{
        Orders ordersA = orderMapper.getById(id);
        //订单只有存在且状态为4（派送中）才可以完成订单
        if (ordersA == null || !ordersA.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //封装Orders对象，将订单状态改为完成
        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id) {
        Orders ordersA = orderMapper.getById(id);
        //判断订单是否为空
        if (ordersA == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //通过websocket向客户端浏览器发送消息 type orderId content
        HashMap<Object, Object> map = new HashMap<>();
        map.put("type",2);//1为来单提醒  2为客户催单
        map.put("orderId",id);
        map.put("content","订单号：" + ordersA.getNumber());

        //将map转换为json字符串
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }
}
