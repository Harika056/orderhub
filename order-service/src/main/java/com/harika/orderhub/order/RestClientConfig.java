package com.harika.orderhub.order;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(500);
        factory.setReadTimeout(1000);
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    RestClient inventoryRestClient(RestClient.Builder loadBalancedBuilder) {
        return loadBalancedBuilder.baseUrl("http://inventory-service").build();
    }
}
