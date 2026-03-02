package com.travel.config.web;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * RestTemplate优化配置
 * 使用Apache HttpClient连接池提升性能
 */
@Configuration
public class RestTemplateConfig {

        /**
         * 优化的RestTemplate Bean
         * - 连接池管理
         * - 超时控制
         * - 连接复用
         */
        @Bean
        public RestTemplate restTemplate() {
                // 配置连接池
                PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                connectionManager.setMaxTotal(200); // 最大连接数
                connectionManager.setDefaultMaxPerRoute(50); // 每个路由的最大连接数

                // 配置连接超时和读取超时
                ConnectionConfig connectionConfig = ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.of(10, TimeUnit.SECONDS)) // 连接超时10秒 (增加以适应网络波动)
                                .setSocketTimeout(Timeout.of(120, TimeUnit.SECONDS)) // 读取超时120秒 (增加以适应模型训练长耗时)
                                .build();
                connectionManager.setDefaultConnectionConfig(connectionConfig);

                // 配置请求超时
                RequestConfig requestConfig = RequestConfig.custom()
                                .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.SECONDS)) // 从连接池获取连接超时10秒
                                .build();

                // 创建HttpClient
                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(connectionManager)
                                .setDefaultRequestConfig(requestConfig)
                                .evictExpiredConnections() // 自动清理过期连接
                                .evictIdleConnections(Timeout.of(30, TimeUnit.SECONDS)) // 清理空闲连接
                                .build();

                // 创建RestTemplate
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

                return new RestTemplate(factory);
        }
}
