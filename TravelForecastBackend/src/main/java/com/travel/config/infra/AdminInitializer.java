package com.travel.config.infra;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.user.User;
import com.travel.mapper.user.UserMapper;
import com.travel.utils.security.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动初始化：创建默认管理员账号 admin/admin123（仅在不存在时创建）
 */
@Slf4j
@Component
public class AdminInitializer implements ApplicationRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
            qw.eq(User::getUsername, "admin");
            User exist = userMapper.selectOne(qw);
            if (exist == null) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(PasswordUtil.encode("admin123"));
                admin.setEmail("syf120508@foxmail.com");
                admin.setNickname("系统管理员");
                admin.setRole("admin");
                admin.setStatus("active");
                userMapper.insert(admin);
                log.info("已创建默认管理员账号：admin / admin123，请及时修改密码！");
            }
        } catch (Exception e) {
            log.warn("创建默认管理员账号失败：{}", e.getMessage());
        }
    }
}


