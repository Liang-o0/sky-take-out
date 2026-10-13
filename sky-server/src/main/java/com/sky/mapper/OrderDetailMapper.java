package com.sky.mapper;

import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.OrderDetail;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 向细节表中插入n条数据
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);

    /**
     * 根据订单id查询订单详情
     * @param ordersId
     * @return
     */
    @Select("select * from order_detail where order_id = #{ordersId}")
    List<OrderDetail> getByOrderId(Long ordersId);

//    /**
//     * 根据订单号批量取消订单
//     * @param ordersRejectionDTO
//     */
//    @Delete("delete from order_detail where order_id = #{id})")
//    void cancelBatch(OrdersRejectionDTO ordersRejectionDTO);
}
