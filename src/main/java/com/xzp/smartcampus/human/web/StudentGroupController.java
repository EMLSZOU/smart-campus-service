package com.xzp.smartcampus.human.web;

import com.xzp.smartcampus.human.model.StudentGroupModel;
import com.xzp.smartcampus.human.service.IStudentGroupService;
import com.xzp.smartcampus.human.vo.StudentGroupTreeVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/human/student-group")
public class StudentGroupController {

    @Resource
    private IStudentGroupService groupService;

    /**
     * 查询分组树
     *
     * @param searchValue 搜索条件
     * @return List<StudentGroupTreeVo>
     */
    @GetMapping("/gets/tree")
    public ResponseEntity<List<StudentGroupTreeVo>> getStudentGroupVoTreeList(StudentGroupModel searchValue) {
        return ResponseEntity.ok(groupService.getStudentGroupVoTreeList(searchValue));
    }

    /**
     * 修改或新增方法
     *
     * @param groupModel 数据
     * @return ResponseEntity<StaffModel>
     */
    @PostMapping("/posts")
    public ResponseEntity<StudentGroupModel> postSchoolModel(@RequestBody StudentGroupModel groupModel) {
        return ResponseEntity.ok(groupService.postGroupModel(groupModel));
    }

    /**
     * 删除数据
     *
     * @param groupIds groupIds
     * @return ResponseEntity<String>
     */
    @GetMapping("/deletes/deletes-by-ids")
    public ResponseEntity<String> deleteGroupByIds(@RequestParam(value = "groupIds", defaultValue = "") String groupIds) {
        groupService.deleteGroupByIds(Arrays.asList(groupIds.split(",")));
        return ResponseEntity.ok("删除成功");
    }

    /**
     * 复制分组 不会复制用户
     *
     * @param sourceIds sourceIds
     * @param targetIds targetIds
     * @return ResponseEntity<StaffGroupModel>
     */
    @PostMapping("/copy")
    public ResponseEntity<String> copyGroupToGroups(@RequestParam(value = "sourceIds", defaultValue = "") String sourceIds,
                                                    @RequestParam(value = "targetIds", defaultValue = "") String targetIds) {
        groupService.copyGroupToGroups(Arrays.asList(sourceIds.split(",")), Arrays.asList(targetIds.split(",")));
        return ResponseEntity.ok("操作成功");
    }

    /**
     * 移动分组
     *
     * @param sourceIds sourceIds
     * @param targetId  targetId
     * @return ResponseEntity<String>
     */
    @PostMapping("/move")
    public ResponseEntity<String> moveGroupToGroups(@RequestParam(value = "sourceIds", defaultValue = "") String sourceIds,
                                                    @RequestParam(value = "targetId", defaultValue = "") String targetId) {
        groupService.moveGroupToGroups(Arrays.asList(sourceIds.split(",")), targetId);
        return ResponseEntity.ok("操作成功");
    }

    /**
     * 移动用户到分组
     *
     * @param userIds  userIds
     * @param targetId targetId
     * @return ResponseEntity<String>
     */
    @PostMapping("/user/move")
    public ResponseEntity<String> moveUserToGroups(@RequestParam(value = "userIds", defaultValue = "") String userIds,
                                                   @RequestParam(value = "targetId", defaultValue = "") String targetId) {
        groupService.moveUserToGroups(Arrays.asList(userIds.split(",")), targetId);
        return ResponseEntity.ok("操作成功");
    }

    /**
     * 根据id获取分组信息
     *
     * @param groupId 分组id
     * @return StaffGroupModel
     */
    @GetMapping("/gets/gets-by-id")
    public ResponseEntity<StudentGroupModel> getStaffGroupModelById(@RequestParam(value = "groupId") String groupId) {
        return ResponseEntity.ok(groupService.selectById(groupId));
    }
}
