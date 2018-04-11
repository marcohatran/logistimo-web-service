package com.logistimo.stockrebalancing.client.internal.actions;

import static com.logistimo.stockrebalancing.client.config.Constants.STOCK_REBALANCING_CLIENT;
import static com.logistimo.stockrebalancing.client.config.Constants.TIMEOUT_IN_MILLISECONDS;

import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.stockrebalancing.client.internal.models.Configuration;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import java.net.URI;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by nitisha.khandelwal on 19/03/18.
 */

public class TriggerStockRebalancingCommand extends HystrixCommand<Void> {

  private final String url;
  private final Configuration request;
  private final String domainId;
  private final RestTemplate restTemplate;

  public TriggerStockRebalancingCommand(RestTemplate restTemplate, String url,
      Configuration request, String domainId) {
    super(HystrixCommandGroupKey.Factory.asKey(STOCK_REBALANCING_CLIENT), TIMEOUT_IN_MILLISECONDS);
    this.restTemplate = restTemplate;
    this.url = url;
    this.domainId = domainId;
    this.request = request;
  }

  @Override
  protected Void run() throws Exception {
    try {
      URI link = JerseyUriBuilder.fromUri(url + "/" + domainId).build();
      restTemplate.postForEntity(link, request, Void.class);
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
    return null;
  }
}
