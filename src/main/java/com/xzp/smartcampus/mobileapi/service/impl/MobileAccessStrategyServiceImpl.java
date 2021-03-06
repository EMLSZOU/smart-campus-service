package com.xzp.smartcampus.mobileapi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzp.smartcampus.access_strategy.model.AccessStrategyModel;
import com.xzp.smartcampus.access_strategy.model.AccessStrategyTimeModel;
import com.xzp.smartcampus.access_strategy.model.AccessStrategyToGroupModel;
import com.xzp.smartcampus.access_strategy.service.IAccessStrategyControlService;
import com.xzp.smartcampus.access_strategy.service.IAccessStrategyService;
import com.xzp.smartcampus.access_strategy.service.IAccessStrategyTimeService;
import com.xzp.smartcampus.access_strategy.service.IAccessStrategyToGroupService;
import com.xzp.smartcampus.common.exception.SipException;
import com.xzp.smartcampus.common.utils.SqlUtil;
import com.xzp.smartcampus.common.vo.PageResult;
import com.xzp.smartcampus.human.model.StaffGroupModel;
import com.xzp.smartcampus.human.model.StudentGroupModel;
import com.xzp.smartcampus.human.service.IStaffGroupService;
import com.xzp.smartcampus.human.service.IStudentGroupService;
import com.xzp.smartcampus.mobileapi.service.IMobileAccessStrategyService;
import com.xzp.smartcampus.mobileapi.vo.AccessStrategyVo;
import com.xzp.smartcampus.mobileapi.vo.UserGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SGS
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class MobileAccessStrategyServiceImpl implements IMobileAccessStrategyService {

    @Resource
    private IAccessStrategyService strategyService;

    @Resource
    private IAccessStrategyTimeService strategyTimeService;

    @Resource
    private IAccessStrategyToGroupService strategyToGroupService;

    @Resource
    private IStudentGroupService studentGroupService;

    @Resource
    private IStaffGroupService userGroupService;

    /**
     * 查询策略列表
     *
     * @param searchParam 搜索条件
     * @param current     当前页
     * @param pageSize    页数量
     * @return PageResult
     */
    @Override
    public PageResult<AccessStrategyVo> getAccessStrategyPage(AccessStrategyVo searchParam, Integer current, Integer pageSize) {
        PageResult<AccessStrategyModel> modelPageResult = strategyService.selectPage(new Page<>(current, pageSize), new QueryWrapper<AccessStrategyModel>()
                .eq(StringUtils.isNotEmpty(searchParam.getStrategyStatus()), "strategy_status", searchParam.getStrategyStatus())
                .like(StringUtils.isNotEmpty(searchParam.getStrategyName()), "strategy_name", searchParam.getStrategyName())
                .orderByDesc("create_time")
        );
        if (CollectionUtils.isEmpty(modelPageResult.getData())) {
            return PageResult.emptyPageResult();
        }
        return new PageResult<>(modelPageResult.getTotal(), modelPageResult.getTotalPage(), this.toAccessStrategyVos(modelPageResult.getData()));
    }

    /**
     * 转换为vo
     *
     * @param strategyModels 数据
     * @return List<AccessStrategyVo>
     */
    private List<AccessStrategyVo> toAccessStrategyVos(List<AccessStrategyModel> strategyModels) {
        if (CollectionUtils.isEmpty(strategyModels)) {
            log.info("strategyModels is null");
            return Collections.emptyList();
        }
        List<String> strategyIds = strategyModels.stream().map(AccessStrategyModel::getId).collect(Collectors.toList());
        Map<String, List<UserGroupVo>> strategyIdToUserGroupVoListMap = this.getStrategyIdToUserGroupVoListMap(strategyIds);
        Map<String, List<AccessStrategyTimeModel>> strategyIdToTimeQuantumListMap = this.getStrategyIdToTimeQuantumListMap(strategyIds);
        return strategyModels.stream().map(item -> {
            AccessStrategyVo strategyVo = new AccessStrategyVo();
            BeanUtils.copyProperties(item, strategyVo);
            strategyVo.setGroupList(strategyIdToUserGroupVoListMap.getOrDefault(item.getId(), Collections.emptyList()));
            strategyVo.setTimeQuantumList(strategyIdToTimeQuantumListMap.getOrDefault(item.getId(), Collections.emptyList()));
            return strategyVo;
        }).collect(Collectors.toList());
    }

    /**
     * 策略id映射时间段
     *
     * @param strategyIds 策略id
     * @return 时间段映射
     */
    private Map<String, List<AccessStrategyTimeModel>> getStrategyIdToTimeQuantumListMap(List<String> strategyIds) {
        if (CollectionUtils.isEmpty(strategyIds)) {
            log.warn("strategyIds is null");
            return Collections.emptyMap();
        }
        List<AccessStrategyTimeModel> strategyTimeModels = strategyTimeService.selectList(new QueryWrapper<AccessStrategyTimeModel>()
                .in("strategy_id", strategyIds)
        );
        if (CollectionUtils.isEmpty(strategyTimeModels)) {
            log.info("not find strategyTimeModels by strategyIds {}", strategyIds);
            return Collections.emptyMap();
        }
        Map<String, List<AccessStrategyTimeModel>> strategyIdToTimeQuantumListMap = new HashMap<>(strategyIds.size());
        strategyTimeModels.forEach(item -> {
            strategyIdToTimeQuantumListMap.computeIfAbsent(item.getStrategyId(), k -> new ArrayList<>()).add(item);
        });
        return strategyIdToTimeQuantumListMap;
    }

    /**
     * 策略id映射分组
     *
     * @param strategyIds 策略id
     * @return 映射关系
     */
    private Map<String, List<UserGroupVo>> getStrategyIdToUserGroupVoListMap(List<String> strategyIds) {
        if (CollectionUtils.isEmpty(strategyIds)) {
            log.warn("strategyIds is null");
            return Collections.emptyMap();
        }
        List<AccessStrategyToGroupModel> strategyToGroupModels = strategyToGroupService.selectList(new QueryWrapper<AccessStrategyToGroupModel>()
                .in("strategy_id", strategyIds)
        );
        if (CollectionUtils.isEmpty(strategyToGroupModels)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> strategyIdGroupIdsMap = new HashMap<>(strategyIds.size());
        List<String> groupIds = new ArrayList<>();
        strategyToGroupModels.forEach(item -> {
            strategyIdGroupIdsMap.computeIfAbsent(item.getStrategyId(), k -> new ArrayList<>()).add(item.getGroupId());
            groupIds.add(item.getGroupId());
        });
        List<UserGroupVo> userGroupVos = new ArrayList<>();
        // 学生组
        List<StudentGroupModel> studentGroupModels = studentGroupService.selectByIds(groupIds);
        if (!CollectionUtils.isEmpty(studentGroupModels)) {
            userGroupVos.addAll(studentGroupModels.stream().map(UserGroupVo::newInstance).collect(Collectors.toList()));
        }
        // 职工组
        List<StaffGroupModel> userGroupModels = userGroupService.selectByIds(groupIds);
        if (!CollectionUtils.isEmpty(userGroupModels)) {
            userGroupVos.addAll(userGroupModels.stream().map(UserGroupVo::newInstance).collect(Collectors.toList()));
        }
        Map<String, UserGroupVo> groupIdToVoMap = userGroupVos.stream().collect(Collectors.toMap(UserGroupVo::getId, k -> k));
        // 设置映射关系
        Map<String, List<UserGroupVo>> strategyIdToUserGroupVoListMap = new HashMap<>(strategyIds.size());
        strategyIdGroupIdsMap.forEach((strategyId, toGroupIds) -> {
            List<UserGroupVo> groupVos = new ArrayList<>();
            toGroupIds.forEach(gId -> {
                if (groupIdToVoMap.containsKey(gId)) {
                    groupVos.add(groupIdToVoMap.get(gId));
                }
            });
            strategyIdToUserGroupVoListMap.put(strategyId, groupVos);
        });

        return strategyIdToUserGroupVoListMap;
    }

    /**
     * 保存策略
     *
     * @param strategyVo 数据
     */
    @Override
    public void saveAccessStrategy(AccessStrategyVo strategyVo) {
        // 新增
        if (StringUtils.isBlank(strategyVo.getId())) {
            this.createAccessStrategy(strategyVo);
            return;
        }
        // 更新
        this.updateAccessStrategy(strategyVo);
    }

    /**
     * 更新策略
     *
     * @param strategyVo 数据
     */
    private void updateAccessStrategy(AccessStrategyVo strategyVo) {
        if (StringUtils.isBlank(strategyVo.getId())) {
            log.warn("id is null");
            throw new SipException("参数错误，更新操作id不能为空");
        }
        AccessStrategyModel strategyModel = strategyService.selectById(strategyVo.getId());
        if (strategyModel == null) {
            log.warn("not find strategyModel by id {}", strategyVo.getId());
            throw new SipException("参数错误，找不到数据 id " + strategyVo.getId());
        }
        strategyModel.setStrategyName(strategyVo.getStrategyName());
        strategyModel.setStrategyStatus(strategyVo.getStrategyStatus());
        strategyModel.setStrategyCode(strategyVo.getStrategyCode());
        strategyModel.setDescription(strategyVo.getDescription());
        // 保存时间段
        if (!CollectionUtils.isEmpty(strategyVo.getTimeQuantumList())) {
            this.saveTimeQuantumList(strategyVo.getId(), strategyVo.getTimeQuantumList());
        }
        strategyService.updateById(strategyModel);
    }

    /**
     * 新增策略
     *
     * @param strategyVo 数据
     */
    private void createAccessStrategy(AccessStrategyVo strategyVo) {
        strategyVo.setId(SqlUtil.getUUId());
        if (!CollectionUtils.isEmpty(strategyVo.getTimeQuantumList())) {
            this.saveTimeQuantumList(strategyVo.getId(), strategyVo.getTimeQuantumList());
        }
        strategyService.insert(strategyVo);
    }

    /**
     * 保存时间段数据
     *
     * @param strategyId      策略id
     * @param timeQuantumList 时间段
     */
    private void saveTimeQuantumList(String strategyId, List<AccessStrategyTimeModel> timeQuantumList) {
        if (StringUtils.isBlank(strategyId) || CollectionUtils.isEmpty(timeQuantumList)) {
            log.warn("strategyId or timeQuantumList is null");
            return;
        }
        // 删除之前的数据
        strategyTimeService.delete(new UpdateWrapper<AccessStrategyTimeModel>()
                .eq("strategy_id", strategyId)
        );
        timeQuantumList.forEach(item -> item.setStrategyId(strategyId));
        // 保存数据
        strategyTimeService.insertBatch(timeQuantumList);
    }

    /**
     * 策略分配
     *
     * @param strategyId 策略id
     * @param groupIds   分组id
     */
    @Override
    public void saveStrategyToGroupIds(String strategyId, List<String> groupIds) {
        if (StringUtils.isBlank(strategyId) || CollectionUtils.isEmpty(groupIds)) {
            log.warn("strategyId or groupIds is null");
            return;
        }
        // 清除之前的关联数据
        strategyToGroupService.delete(new UpdateWrapper<AccessStrategyToGroupModel>()
                .eq("strategy_id", strategyId)
        );
        // 重新建立关联
        List<AccessStrategyToGroupModel> strategyToGroupModels = groupIds.stream().map(groupId -> {
            AccessStrategyToGroupModel strategyToGroupModel = new AccessStrategyToGroupModel();
            strategyToGroupModel.setGroupId(groupId);
            strategyToGroupModel.setStrategyId(strategyId);
            return strategyToGroupModel;
        }).collect(Collectors.toList());
        // 保存数据
        strategyToGroupService.insertBatch(strategyToGroupModels);
    }

    /**
     * 删除策略
     *
     * @param strategyIds 策略id
     */
    @Override
    public void deleteStrategyByIds(List<String> strategyIds) {
        if (CollectionUtils.isEmpty(strategyIds)) {
            log.warn("strategyIds is null");
            return;
        }
        // 删除时间段
        strategyTimeService.delete(new UpdateWrapper<AccessStrategyTimeModel>()
                .in("strategy_id", strategyIds)
        );
        // 删除自身
        strategyService.deleteByIds(strategyIds);
    }

    /**
     * 根据id查询
     *
     * @param strategyId strategyId
     * @return AccessStrategyVo
     */
    @Override
    public AccessStrategyVo getAccessStrategyVoById(String strategyId) {
        if (StringUtils.isBlank(strategyId)) {
            log.warn("strategyId is null");
            throw new SipException("参数错误，strategyId不能为空");
        }
        AccessStrategyModel strategyModel = strategyService.selectById(strategyId);
        if (strategyModel == null) {
            log.warn("strategyModel not find by strategyId {}", strategyId);
            throw new SipException("参数错误，找不到数据 strategyId " + strategyId);
        }
        List<AccessStrategyVo> strategyVos = this.toAccessStrategyVos(Collections.singletonList(strategyModel));
        if (CollectionUtils.isEmpty(strategyVos)) {
            return null;
        }
        return strategyVos.get(0);
    }
}
