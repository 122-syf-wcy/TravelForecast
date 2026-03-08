package com.travel.config.infra;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyuncs.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
        log.info("初始化OSS客户端: endpoint={}, region={}", endpoint, region);
        
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V1);
        
        if (accessKeyId != null && !accessKeyId.isEmpty() && 
            accessKeySecret != null && !accessKeySecret.isEmpty()) {
            log.info("使用配置文件中的AccessKey: accessKeyId={}", accessKeyId.substring(0, 8) + "***");
            return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
        }
        
        String envKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String envKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        if (envKeyId != null && !envKeyId.isEmpty() &&
            envKeySecret != null && !envKeySecret.isEmpty()) {
            log.info("使用环境变量中的AccessKey");
            try {
                EnvironmentVariableCredentialsProvider provider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
                return new OSSClientBuilder().build(endpoint, provider, conf);
            } catch (Exception e) {
                log.warn("通过环境变量初始化OSS失败: {}", e.getMessage());
            }
        }
        
        log.warn("OSS AccessKey 未配置，OSS 功能将不可用（本地开发可忽略）");
        return new OSSClientBuilder().build(endpoint, "placeholder", "placeholder", conf);
    }
}


