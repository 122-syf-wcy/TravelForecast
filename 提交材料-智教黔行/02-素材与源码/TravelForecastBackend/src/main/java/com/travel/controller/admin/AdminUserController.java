package com.travel.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.Result;
import com.travel.entity.user.User;
import com.travel.mapper.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员端-用户管理
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取用户详情
     * GET /admin/users/{userId}
     */
    @GetMapping("/{userId}")
    public Result<User> getUserById(@PathVariable Long userId) {
        log.info("管理员查询用户详情: userId={}", userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 删除用户（软删除）
     * DELETE /admin/users/{userId}
     */
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        log.info("管理员删除用户: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不允许删除管理员账号
        if ("admin".equals(user.getRole())) {
            return Result.error("不允许删除管理员账号");
        }

        // 软删除（MyBatis-Plus会自动处理deleted_at字段）
        int deleted = userMapper.deleteById(userId);

        if (deleted > 0) {
            log.info("用户删除成功: userId={}, username={}", userId, user.getUsername());
            return Result.success(null, "删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 获取用户列表（分页）
     * GET /admin/users?page=1&size=10&username=xxx&phone=xxx&status=xxx
     */
    @GetMapping
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("管理员查询用户列表: page={}, size={}, username={}, phone={}, status={}",
                page, size, username, phone, status);

        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", "user"); // 只查询普通用户

        if (username != null && !username.isEmpty()) {
            queryWrapper.like("username", username);
        }
        if (phone != null && !phone.isEmpty()) {
            queryWrapper.like("phone", phone);
        }
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            queryWrapper.between("created_at", startDate, endDate);
        }

        queryWrapper.orderByDesc("created_at");

        // 分页查询
        Page<User> userPage = new Page<>(page, size);
        userPage = userMapper.selectPage(userPage, queryWrapper);

        // 移除密码字段
        List<User> users = userPage.getRecords();
        users.forEach(user -> user.setPassword(null));

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("records", users);
        result.put("total", userPage.getTotal());
        result.put("current", userPage.getCurrent());
        result.put("size", userPage.getSize());
        result.put("pages", userPage.getPages());

        return Result.success(result);
    }

    /**
     * 获取用户统计数据
     * GET /admin/users/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getUserStatistics() {
        log.info("管理员查询用户统计数据");

        Map<String, Object> statistics = new HashMap<>();

        // 总用户数（只计算普通用户）
        QueryWrapper<User> totalQuery = new QueryWrapper<>();
        totalQuery.eq("role", "user");
        long totalUsers = userMapper.selectCount(totalQuery);

        // 活跃用户数（最近30天登录过的用户）
        QueryWrapper<User> activeQuery = new QueryWrapper<>();
        activeQuery.eq("role", "user");
        activeQuery.eq("status", "active");
        activeQuery.ge("login_at", LocalDateTime.now().minusDays(30));
        long activeUsers = userMapper.selectCount(activeQuery);

        // 本月新增用户
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        QueryWrapper<User> monthQuery = new QueryWrapper<>();
        monthQuery.eq("role", "user");
        monthQuery.ge("created_at", monthStart);
        long monthlyNewUsers = userMapper.selectCount(monthQuery);

        // 付费用户数（这里暂时用status=active的用户代替，实际应该根据业务逻辑判断）
        QueryWrapper<User> paidQuery = new QueryWrapper<>();
        paidQuery.eq("role", "user");
        paidQuery.eq("status", "active");
        long paidUsers = userMapper.selectCount(paidQuery);

        statistics.put("totalUsers", totalUsers);
        statistics.put("activeUsers", activeUsers);
        statistics.put("monthlyNewUsers", monthlyNewUsers);
        statistics.put("paidUsers", paidUsers);

        return Result.success(statistics);
    }

    /**
     * 获取用户增长趋势数据
     * GET /admin/users/growth
     */
    @GetMapping("/growth")
    public Result<Map<String, Object>> getUserGrowth() {
        log.info("管理员查询用户增长趋势");

        Map<String, Object> result = new HashMap<>();
        List<String> months = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();
        List<Long> activeUsersList = new ArrayList<>();

        // 最近7个月的数据
        for (int i = 6; i >= 0; i--) {
            LocalDateTime monthStart = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0)
                    .withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);

            String monthLabel = monthStart.format(DateTimeFormatter.ofPattern("M月"));
            months.add(monthLabel);

            // 该月新增用户
            QueryWrapper<User> newQuery = new QueryWrapper<>();
            newQuery.eq("role", "user");
            newQuery.ge("created_at", monthStart);
            newQuery.lt("created_at", monthEnd);
            long newCount = userMapper.selectCount(newQuery);
            newUsers.add(newCount);

            // 该月活跃用户（在该月登录过的用户）
            QueryWrapper<User> activeQuery = new QueryWrapper<>();
            activeQuery.eq("role", "user");
            activeQuery.ge("login_at", monthStart);
            activeQuery.lt("login_at", monthEnd);
            long activeCount = userMapper.selectCount(activeQuery);
            activeUsersList.add(activeCount);
        }

        result.put("months", months);
        result.put("newUsers", newUsers);
        result.put("activeUsers", activeUsersList);

        return Result.success(result);
    }

    /**
     * 获取用户分布数据（按地区）
     * GET /admin/users/distribution
     */
    @GetMapping("/distribution")
    public Result<List<Map<String, Object>>> getUserDistribution() {
        log.info("管理员查询用户地区分布");

        // 这里简化处理，实际应该从用户表的地区字段统计
        // 由于当前User实体没有地区字段，这里返回示例数据
        List<Map<String, Object>> distribution = new ArrayList<>();

        Map<String, Object> southwest = new HashMap<>();
        southwest.put("name", "西南地区");
        southwest.put("value", 1250);
        distribution.add(southwest);

        Map<String, Object> east = new HashMap<>();
        east.put("name", "华东地区");
        east.put("value", 850);
        distribution.add(east);

        Map<String, Object> north = new HashMap<>();
        north.put("name", "华北地区");
        north.put("value", 600);
        distribution.add(north);

        Map<String, Object> south = new HashMap<>();
        south.put("name", "华南地区");
        south.put("value", 350);
        distribution.add(south);

        Map<String, Object> other = new HashMap<>();
        other.put("name", "其他地区");
        other.put("value", 100);
        distribution.add(other);

        return Result.success(distribution);
    }

    /**
     * 更新用户状态
     * PUT /admin/users/{userId}/status
     */
    @PutMapping("/{userId}/status")
    public Result<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        log.info("管理员更新用户状态: userId={}, status={}", userId, status);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不允许修改管理员状态
        if ("admin".equals(user.getRole())) {
            return Result.error("不允许修改管理员状态");
        }

        user.setStatus(status);
        int updated = userMapper.updateById(user);

        if (updated > 0) {
            return Result.success(null, "状态更新成功");
        } else {
            return Result.error("状态更新失败");
        }
    }

    /**
     * 更新用户信息
     * PUT /admin/users/{userId}
     */
    @PutMapping("/{userId}")
    public Result<User> updateUser(
            @PathVariable Long userId,
            @RequestBody User userUpdate) {

        log.info("管理员更新用户信息: userId={}", userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不允许修改管理员信息
        if ("admin".equals(user.getRole())) {
            return Result.error("不允许修改管理员信息");
        }

        // 更新允许修改的字段
        if (userUpdate.getUsername() != null) {
            user.setUsername(userUpdate.getUsername());
        }
        if (userUpdate.getEmail() != null) {
            user.setEmail(userUpdate.getEmail());
        }
        if (userUpdate.getPhone() != null) {
            user.setPhone(userUpdate.getPhone());
        }
        if (userUpdate.getNickname() != null) {
            user.setNickname(userUpdate.getNickname());
        }
        if (userUpdate.getStatus() != null) {
            user.setStatus(userUpdate.getStatus());
        }

        int updated = userMapper.updateById(user);

        if (updated > 0) {
            user.setPassword(null);
            return Result.success(user, "更新成功");
        } else {
            return Result.error("更新失败");
        }
    }
}
