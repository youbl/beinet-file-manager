package cn.beinet.deployment.admin.users.controller;

import cn.beinet.business.login.service.UserManagementService;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.users.dto.UsersDto;
import cn.beinet.deployment.admin.users.service.UsersService;
import cn.beinet.sdk.login.dto.UserDto;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author youbl.blog.csdn.net
 * @since 2024-11-19 12:28:00
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "users", description = "用户增删改查接口类")
public class UsersController {
    private final UsersService service;
    private final UserManagementService userManagementService;

    @PostMapping("/users/all")
    @Operation(summary = "用户列表", description = "用户列表查询接口")
    public ResponseData<List<UsersDto>> findAll(@RequestBody UsersDto dto) {
        return ResponseData.ok(service.search(dto));
    }

    @GetMapping("/users")
    @Operation(summary = "用户查询", description = "根据id，查询单个用户")
    public ResponseData<UsersDto> findById(@NonNull Long id) {
        return ResponseData.ok(service.findById(id));
    }


    @DeleteMapping("/users")
    // @EventLog(subType = SubType.Users_DEL)
    @Operation(summary = "删除用户", description = "根据id，删除单个用户")
    public ResponseData<Boolean> delById(@NonNull Long id) {
        return ResponseData.ok(service.removeById(id));
    }

    @PutMapping("/users")
    // @EventLog(subType = SubType.Users_UPDATE)
    @Operation(summary = "编辑用户", description = "根据id，更新单个用户数据")
    public ResponseData<Long> updateById(@RequestBody @NonNull UsersDto dto) {
        Assert.notNull(dto.getId(), "update must set primary key.");
        Long newId = service.saveUsers(dto);
        return ResponseData.ok(newId);
    }

    @PostMapping("/users")
    // 要上报新增的主键，因此这里不能用注解@EventLog
    @Operation(summary = "新增用户", description = "新增单个用户数据")
    public ResponseData<Long> insert(@RequestBody @NonNull UsersDto dto) {
        // clear primary key before insert
        dto.setId(null);

        Long newId = service.saveUsers(dto);
        dto.setId(newId);
        //LogUtils.reportLog(SubType.Users_ADD, dto); // 事件上报代码
        return ResponseData.ok(newId);
    }

    // ========== 用户管理功能扩展 ==========

    @GetMapping("/users/management/page")
    @Operation(summary = "分页查询用户", description = "分页查询用户列表，支持搜索和筛选")
    public ResponseData<IPage<UserDto>> queryUsersPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer loginType) {
        
        IPage<UserDto> result = userManagementService.queryUsers(page, size, keyword, status, loginType);
        return ResponseData.ok(result);
    }

    @GetMapping("/users/management/{userId}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    public ResponseData<UserDto> getUserDetail(@PathVariable Long userId) {
        UserDto user = userManagementService.getUserById(userId);
        return ResponseData.ok(user);
    }

    @PutMapping("/users/management/{userId}/status")
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    public ResponseData<Boolean> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam Integer status,
            HttpServletRequest request) {
        
        if (status != 0 && status != 1) {
            return ResponseData.fail(400, "状态值无效，只能是0（禁用）或1（启用）");
        }
        
        boolean success = userManagementService.updateUserStatus(userId, status, request);
        return ResponseData.ok(success);
    }

    @DeleteMapping("/users/management/{userId}")
    @Operation(summary = "删除用户", description = "软删除用户（管理员操作）")
    public ResponseData<Boolean> deleteUserByAdmin(@PathVariable Long userId, HttpServletRequest request) {
        boolean success = userManagementService.deleteUser(userId, request);
        return ResponseData.ok(success);
    }

    @GetMapping("/users/management/statistics")
    @Operation(summary = "获取用户统计", description = "获取用户总数、活跃用户数等统计信息")
    public ResponseData<UserManagementService.UserStatistics> getUserStatistics() {
        UserManagementService.UserStatistics statistics = userManagementService.getUserStatistics();
        return ResponseData.ok(statistics);
    }

}