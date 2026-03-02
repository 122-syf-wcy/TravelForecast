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
    public OSS ossClient() throws ClientException {
        log.info("初始化OSS客户端: endpoint={}, region={}", endpoint, region);
        
        // 使用V1签名，更兼容
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V1);
        
        // 优先使用配置文件中的AccessKey，如果没有则使用环境变量
        if (accessKeyId != null && !accessKeyId.isEmpty() && 
            accessKeySecret != null && !accessKeySecret.isEmpty()) {
            log.info("使用配置文件中的AccessKey: accessKeyId={}", accessKeyId.substring(0, 8) + "***");
            // 使用简单的构造方式
            return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
        } else {
            log.info("使用环境变量中的AccessKey");
            EnvironmentVariableCredentialsProvider provider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            return new OSSClientBuilder().build(endpoint, provider, conf);
        }
    }
}


