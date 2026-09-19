package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.roborder.RobOrderFlowMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderFlowPageQueryDTO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowGroupVO;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RobOrderFlowServiceImpl 单元测试
 * <p>
 * 聚焦按订单组两层分页的 Java 组装契约（fix-rob-order-flow-group-by-order）：
 * 第 2 层 SQL 只保组内正序，组间顺序必须由 Service 按第 1 层 orderId 序列重排；
 * 分页元数据以订单组计，空页不得发起第 2 层查询。
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

    private RobOrderFlowGroupVO group(long orderId) {
        RobOrderFlowGroupVO g = new RobOrderFlowGroupVO();
        g.setOrderId(orderId);
        return g;
    }

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
    void getFlowPage_reordersRowsByGroupSequence_andKeepsWithinGroupAsc() {
        RobOrderFlowPageQueryDTO dto = new RobOrderFlowPageQueryDTO();
        dto.setPageNum(1);
        dto.setPageSize(10);

        // 第 1 层：当页组序 O1 在前、O3 在后（组总数 7）
        Page<RobOrderFlowGroupVO> groupPage = new Page<>(1, 10);
        groupPage.setRecords(List.of(group(1L), group(3L)));
        groupPage.setTotal(7L);
        when(robOrderFlowMapper.selectFlowOrderGroups(any(), any(), any(), any())).thenReturn(groupPage);

        // 第 2 层：SQL 全局时间正序返回——O3 的 5 行（16 点）整体早于 O1 的 3 行（17 点）
        List<RobOrderFlowVO> sqlOrderRows = new ArrayList<>();
        sqlOrderRows.add(row(3L, 1, LocalDateTime.of(2026, 9, 19, 16, 0), "10400.00"));
        sqlOrderRows.add(row(3L, 3, LocalDateTime.of(2026, 9, 19, 16, 10), "-10400.00"));
        sqlOrderRows.add(row(3L, 3, LocalDateTime.of(2026, 9, 19, 16, 10), "10400.00"));
        sqlOrderRows.add(row(3L, 3, LocalDateTime.of(2026, 9, 19, 16, 20), "-10400.00"));
        sqlOrderRows.add(row(3L, 3, LocalDateTime.of(2026, 9, 19, 16, 20), "10400.00"));
        sqlOrderRows.add(row(1L, 1, LocalDateTime.of(2026, 9, 19, 17, 5), "10400.00"));
        sqlOrderRows.add(row(1L, 3, LocalDateTime.of(2026, 9, 19, 17, 6), "-10400.00"));
        sqlOrderRows.add(row(1L, 3, LocalDateTime.of(2026, 9, 19, 17, 6), "10400.00"));
        when(robOrderFlowMapper.selectFlowRowsByOrderIds(anyList(), any(), any(), any())).thenReturn(sqlOrderRows);
        when(robOrderFlowMapper.selectFlowTotalAmount(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        Response resp = robOrderFlowService.getFlowPage(dto);
        assertEquals(200, resp.getCode());

        RobOrderFlowPageResultVO result = (RobOrderFlowPageResultVO) resp.getData();
        assertNotNull(result);
        List<RobOrderFlowVO> list = result.getList();

        // 平铺 8 行：先整组 O1（3 行），再整组 O3（5 行），同组连续且组内保持 SQL 正序
        assertEquals(8, list.size());
        assertEquals(List.of(1L, 1L, 1L, 3L, 3L, 3L, 3L, 3L),
                list.stream().map(RobOrderFlowVO::getOrderId).toList());
        // O1 组内：下单(1) -> 红冲(3,-) -> 正向(3,+)
        assertEquals(List.of(1, 3, 3), list.subList(0, 3).stream().map(RobOrderFlowVO::getEventType).toList());
        assertEquals("10400.00", list.get(0).getSignedTotalAmount().toPlainString());
        assertEquals("-10400.00", list.get(1).getSignedTotalAmount().toPlainString());
        assertEquals("10400.00", list.get(2).getSignedTotalAmount().toPlainString());
        // O3 链式两次转移的五行顺序原样保留
        assertEquals(List.of(1, 3, 3, 3, 3),
                list.subList(3, 8).stream().map(RobOrderFlowVO::getEventType).toList());
        // 中文名已回填
        assertNotNull(list.get(0).getEventTypeName());

        // 分页元数据以订单组计
        assertEquals(7L, result.getTotal());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalAmount()));
    }

    @Test
    void getFlowPage_emptyPage_skipsSecondLayerQuery() {
        RobOrderFlowPageQueryDTO dto = new RobOrderFlowPageQueryDTO();
        dto.setPageNum(99);
        dto.setPageSize(10);

        Page<RobOrderFlowGroupVO> emptyPage = new Page<>(99, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        when(robOrderFlowMapper.selectFlowOrderGroups(any(), any(), any(), any())).thenReturn(emptyPage);
        when(robOrderFlowMapper.selectFlowTotalAmount(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        Response resp = robOrderFlowService.getFlowPage(dto);
        assertEquals(200, resp.getCode());

        RobOrderFlowPageResultVO result = (RobOrderFlowPageResultVO) resp.getData();
        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        // 无订单组时不得发起第 2 层 IN 查询
        verify(robOrderFlowMapper, never()).selectFlowRowsByOrderIds(anyList(), any(), any(), any());
    }
}
