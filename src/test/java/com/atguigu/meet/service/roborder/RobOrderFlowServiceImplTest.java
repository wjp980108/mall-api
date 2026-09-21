package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.roborder.RobOrderFlowMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderFlowPageQueryDTO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowPageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.atguigu.meet.service.roborder.impl.RobOrderFlowServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RobOrderFlowServiceImpl 单元测试
 * <p>
 * 聚焦单层事件物理行分页契约（fix-rob-order-flow-page-by-time）：
 * Service 直出 Mapper 行分页结果——全局事件时间倒序，同一转移事件的正向行(+)
 * MUST 在红冲行(-)之前；不按订单聚合，分页元数据以物理行计。
 */
@ExtendWith(MockitoExtension.class)
class RobOrderFlowServiceImplTest {

    @Mock
    private RobOrderFlowMapper robOrderFlowMapper;
    @Mock
    private RobOrderMapper robOrderMapper;
    @Mock
    private RobOrderOperateLogMapper operateLogMapper;

    @InjectMocks
    private RobOrderFlowServiceImpl robOrderFlowService;

    private RobOrderFlowVO row(long orderId, int eventType, LocalDateTime time, String amount) {
        RobOrderFlowVO vo = new RobOrderFlowVO();
        vo.setOrderId(orderId);
        vo.setEventType(eventType);
        vo.setEventTime(time);
        vo.setSignedTotalAmount(new BigDecimal(amount));
        return vo;
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFlowPage_passesThroughRowPage_sameTimePositiveBeforeReversal() {
        RobOrderFlowPageQueryDTO dto = new RobOrderFlowPageQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        // Mapper 行分页结果（ORDER BY create_time DESC, id DESC, split.n DESC 的直出顺序）
        List<RobOrderFlowVO> records = List.of(
                // 18:00 订单3 下单
                row(3L, 1, LocalDateTime.of(2026, 9, 19, 18, 0), "100.00"),
                // 17:10 订单1 第二次转移正向(C) +
                row(1L, 3, LocalDateTime.of(2026, 9, 19, 17, 10), "100.00"),
                // 17:10 订单1 第二次转移红冲(B) -
                row(1L, 3, LocalDateTime.of(2026, 9, 19, 17, 10), "-100.00"),
                // 17:05 订单1 下单（同订单行不连续，排在其转移行下方）
                row(1L, 1, LocalDateTime.of(2026, 9, 19, 17, 5), "100.00"));
        Page<RobOrderFlowVO> rowPage = new Page<>(1, 10);
        rowPage.setRecords(records);
        rowPage.setTotal(9L);
        when(robOrderFlowMapper.selectFlowPage(any(), any(), any(), any(), any())).thenReturn(rowPage);
        when(robOrderFlowMapper.selectFlowTotalAmount(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        Response resp = robOrderFlowService.getFlowPage(dto);
        assertEquals(200, resp.getCode());

        RobOrderFlowPageResultVO result = (RobOrderFlowPageResultVO) resp.getData();
        assertNotNull(result);
        List<RobOrderFlowVO> list = result.getList();

        // 4 行直出，顺序不被重排；不按订单聚合（orderIds = 3,1,1,1）
        assertEquals(4, list.size());
        assertEquals(List.of(3L, 1L, 1L, 1L),
                list.stream().map(RobOrderFlowVO::getOrderId).toList());
        // 同刻正向行(+100) 在红冲行(-100) 之前
        assertEquals("100.00", list.get(1).getSignedTotalAmount().toPlainString());
        assertEquals("-100.00", list.get(2).getSignedTotalAmount().toPlainString());
        // 中文名已回填
        assertNotNull(list.get(0).getEventTypeName());
        assertNotNull(list.get(1).getEventTypeName());

        // 分页元数据以事件物理行计
        assertEquals(9L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalAmount()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFlowPage_emptyPage_returnsEmptyList() {
        RobOrderFlowPageQueryDTO dto = new RobOrderFlowPageQueryDTO();
        dto.setPageNum(99);
        dto.setPageSize(10);

        Page<RobOrderFlowVO> emptyPage = new Page<>(99, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        when(robOrderFlowMapper.selectFlowPage(any(), any(), any(), any(), any())).thenReturn(emptyPage);
        when(robOrderFlowMapper.selectFlowTotalAmount(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);

        Response resp = robOrderFlowService.getFlowPage(dto);
        assertEquals(200, resp.getCode());

        RobOrderFlowPageResultVO result = (RobOrderFlowPageResultVO) resp.getData();
        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        // 单层分页：只查一次分页、一次合计
        verify(robOrderFlowMapper).selectFlowPage(any(), any(), any(), any(), any());
    }
}
