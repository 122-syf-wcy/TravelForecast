package com.travel;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 游云华章 - 主启动类
 * 
 * @author Travel Team
 * @date 2025-10-10
 */
@SpringBootApplication
public class TravelPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelPredictionApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("智教黔行·时空旅测绘梦蓝图 启动成功!");
        System.out.println("========================================\n");
    }
}
