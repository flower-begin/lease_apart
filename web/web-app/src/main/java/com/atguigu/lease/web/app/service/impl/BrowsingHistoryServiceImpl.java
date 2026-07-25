package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.model.entity.BrowsingHistory;
import com.atguigu.lease.web.app.mapper.BrowsingHistoryMapper;
import com.atguigu.lease.web.app.mapper.GraphInfoMapper;
import com.atguigu.lease.web.app.service.BrowsingHistoryService;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.atguigu.lease.web.app.vo.history.HistoryItemVo;
import com.atguigu.lease.model.enums.ItemType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author liubo
 * @description 针对表【browsing_history(浏览历史)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
@Slf4j
public class BrowsingHistoryServiceImpl extends ServiceImpl<BrowsingHistoryMapper, BrowsingHistory>
        implements BrowsingHistoryService {

    @Autowired
    private BrowsingHistoryMapper browsingHistoryMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Override
    public Page<HistoryItemVo> pageHistory(Page<HistoryItemVo> page, Long userId) {
        Page<HistoryItemVo> result = browsingHistoryMapper.pageHistory(page, userId);
        // Populate room images for each history item
        if (result.getRecords() != null) {
            for (HistoryItemVo item : result.getRecords()) {
                List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.ROOM, item.getRoomId());
                item.setRoomGraphVoList(graphVoList);
            }
        }
        return result;
    }

}
