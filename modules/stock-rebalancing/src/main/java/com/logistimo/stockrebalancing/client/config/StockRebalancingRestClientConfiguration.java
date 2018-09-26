package com.logistimo.stockrebalancing.client.config;

import com.logistimo.rest.client.RestConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Created by nitisha.khandelwal on 23/03/18.
 */

@Configuration
public class StockRebalancingRestClientConfiguration {

  @Bean
  public RestTemplate stockRebalancingRestTemplate() {
    return RestConfig.restTemplate();
  }
}
