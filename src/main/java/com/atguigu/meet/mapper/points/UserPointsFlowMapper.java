package com.atguigu.meet.mapper.points;

import com.atguigu.meet.model.entity.points.UserPointsFlow;
import com.atguigu.meet.model.vo.points.PointsFlowVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

/**
 * 用户积分流水 Mapper
 */
public interface UserPointsFlowMapper extends BaseMapper<UserPointsFlow> {

    /**
     * 分页查询某用户的积分明细（按业务类型可选筛选，时间倒序）
     *
     * @param page    分页参数
     * @param userId  用户ID
     * @param bizType 业务类型（1推荐奖 2自购奖 3购物券奖 4积分对冲），传 null 查全部
     */
    IPage<PointsFlowVO> selectFlowPage(Page<PointsFlowVO> page,
                                       @Param("userId") Long userId,
                                       @Param("bizType") Integer bizType);
}
