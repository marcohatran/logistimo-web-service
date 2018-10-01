package com.logistimo.stockrebalancing.client.internal;

import com.logistimo.stockrebalancing.client.IStockRebalancingClient;
import com.logistimo.stockrebalancing.client.config.StockRebalancingClientConfiguration;
import com.logistimo.stockrebalancing.client.internal.actions.TriggerStockRebalancingCommand;
import com.logistimo.stockrebalancing.client.internal.models.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by nitisha.khandelwal on 19/03/18.
 */

@Service
public class StockRebalancingClient implements IStockRebalancingClient {

  @Autowired
  private RestTemplate stockRebalancingRestTemplate;

  @Autowired
  private StockRebalancingClientConfiguration configuration;

  @Override
  public void triggerStockRebalancing(String domainId, Configuration request) {
    TriggerStockRebalancingCommand command = new TriggerStockRebalancingCommand(
        stockRebalancingRestTemplate,
        configuration.getPath(), request, domainId);
    command.execute();
  }
}
