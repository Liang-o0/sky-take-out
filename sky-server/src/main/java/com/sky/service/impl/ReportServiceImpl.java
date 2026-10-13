package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkSpaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkSpaceService workspaceService;

    /**
     * 获取营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dateListVO = new ArrayList<>();
        dateListVO.add(begin);

        //将从begin到end的日期都一个一个添加到dateList中
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dateListVO.add(begin);
        }
        //用来存放每条营业额的集合
        ArrayList<Double> turnList = new ArrayList<>();
        //遍历dateList，获取每天的营业额
        for (LocalDate date : dateListVO) {
            //将获取到当天最早时间和当天最迟时间，时间格式也转为LocalDateTime
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);//统计已完成的订单
            //Double turnover = orderMapper.getByMap(beginTime, endTime, Orders.COMPLETED);
            Double turnover = orderMapper.getByMap(map);
            turnover = turnover == null ? 0.0 : turnover;//营业额不为null，则 turnover = turnover，否则 turnover = 0.0
            turnList.add(turnover);
        }

        String dateList = StringUtils.join(dateListVO,",");
        String turnoverList = StringUtils.join(turnList,",");


        return new TurnoverReportVO(dateList, turnoverList);
    }

    /**
     * 用户统计接口
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserReport(LocalDate begin, LocalDate end) {

        List<LocalDate> dateListVO = new ArrayList<>();
        dateListVO.add(begin);

        while (!begin.isEqual(end)) {
                begin = begin.plusDays(1);
                dateListVO.add(begin);
        }
        //统计每天新用户数量的集合
        List<Integer> newUser = new ArrayList<>();
        //统计每天总用户数量的集合
        List<Integer> totalUser = new ArrayList<>();

        for (LocalDate date : dateListVO) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);


            Map<String, Object> map = new HashMap<>();
            map.put("end", endTime);
            //这里Map使用不了。所以改为OrdersPageQueryDTO传递参数
            //OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
            //ordersPageQueryDTO.setEndTime(endTime);
            //totalUser.add(orderMapper.getUserReport(ordersPageQueryDTO));
            totalUser.add(userMapper.countByMap(map));
            map.put("begin", beginTime);
            //ordersPageQueryDTO.setBeginTime(beginTime);
            //newUser.add(orderMapper.getUserReport(ordersPageQueryDTO));
            newUser.add(userMapper.countByMap(map));
        }
        String dateList = StringUtils.join(dateListVO, ",");
        String newUserList = StringUtils.join(newUser, ",");
        String totalUserList = StringUtils.join(totalUser, ",");

        return new UserReportVO(dateList, totalUserList, newUserList);
    }

    /**
     * 获取订单统计接口
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderReport(LocalDate begin, LocalDate end) {
        List<LocalDate> dateListVO = new ArrayList<>();
        dateListVO.add(begin);
        while (!begin.isEqual(end)) {
            begin = begin.plusDays(1);
            dateListVO.add(begin);
        }

        //订单总数的集合
        List<Integer> orderCountListVO = new ArrayList<>();
        //有效订单数的集合
        List<Integer> validOrderCountListVO = new ArrayList<>();

        for (LocalDate date : dateListVO) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();


            //查询每日订单数，
            map.put("begin", beginTime);
            map.put("end", endTime);

            orderCountListVO.add(orderMapper.getOrderReport(map));

            map.put("status", Orders.COMPLETED);
            validOrderCountListVO.add(orderMapper.getOrderReport(map));
        }

        //计算总订单数
        Integer totalOrderCount = orderCountListVO.stream().reduce(Integer::sum).get();
        //计算有效订单数
        Integer validOrderCount = validOrderCountListVO.stream().reduce(Integer::sum).get();
        //计算订单完成率
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        String validOrderCountList = StringUtils.join(validOrderCountListVO, ",");
        String orderCountList = StringUtils.join(orderCountListVO, ",");
        String dateList = StringUtils.join(dateListVO, ",");
        return new OrderReportVO(dateList, orderCountList, validOrderCountList, totalOrderCount, validOrderCount, orderCompletionRate);
    }

    /**
     * 查询销量排名top10接口
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        //获取开始时间和结束时间
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        //获取销量排名top10
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);
        //将goodsSalesDTOList转换为SalesTop10ReportVO需要的格式
        //提取出所有的名称和数量
        List<String> names = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        //将取出来的名称和数量转换为String类型List集合，并且使用逗号隔开
        String nameList = StringUtils.join(names, ",");
        String numberList = StringUtils.join(numbers, ",");

        return new SalesTop10ReportVO(nameList, numberList);
    }


    /**
     * 导出Excel报表接口
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库获取营业数据
            //查询最近30天的营业数据，获取日期
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
            //将时间格式转为LocalDateTime
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);
        //2.通过POI将数据写入到Excel表中
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的文件
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

            //获取表格文件的sheet标签页
            XSSFSheet sheet1 = workbook.getSheet("Sheet1");
            //填充时间
            sheet1.getRow(1).getCell(1).setCellValue("时间：" + begin + "到" + end);

            //获取到第4行，
            XSSFRow row = sheet1.getRow(3);
            //填充营业额
            row.getCell(2).setCellValue(businessData.getTurnover());
            //获取订单完成率
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate().doubleValue());
            //获取新增用户
            row.getCell(6).setCellValue(businessData.getNewUsers());


            //获取到第5行
            row = sheet1.getRow(4);
            //获取有效订单数
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            //获取平均客单价
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            //填充表格中的明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                row = sheet1.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessDataVO.getTurnover());
                row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate().doubleValue());
                row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                row.getCell(6).setCellValue(businessDataVO.getNewUsers());


            }


            //3.通过输出流将Excel表写入到客户端浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            //4.关闭资源
            workbook.close();
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
