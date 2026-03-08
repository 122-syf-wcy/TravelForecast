package com.travel.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OssConfig {
    @Value("${oss.endpoint:https://oss-cn-beijing.aliyuncs.com}")
    private String endpoint;

    @Value("${oss.region:cn-beijing}")
    private String region;

    @Value("${oss.access-key-id:}")
    private String accessKeyId;

    @Value("${oss.access-key-secret:}")
    private String accessKeySecret;

    @Bean
    public OSS ossClient() {
        if (accessKeyId == null || accessKeyId.isEmpty() ||
            accessKeySecret == null || accessKeySecret.isEmpty()) {
            log.warn("OSS AccessKey 未配置，OSS 功能将不可用（本地开发可忽略）");
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            conf.setSignatureVersion(SignVersion.V1);
            return new OSSClientBuilder().build(endpoint, "placeholder", "placeholder", conf);
        }
        log.info("初始化OSS客户端: endpoint={}, region={}", endpoint, region);
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V1);
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
    }
}
