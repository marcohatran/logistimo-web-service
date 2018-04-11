package com.logistimo.stockrebalancing.client.config;

import com.logistimo.services.utils.ConfigUtil;
import org.springframework.stereotype.Component;

/**
 * Created by nitisha.khandelwal on 19/03/18.
 */

@Component
public class StockRebalancingClientConfiguration {

  private String url = ConfigUtil.get("stockrebalancing.url", "http://localhost:8700");

  private String path = ConfigUtil.get("stockrebalancing.path", "/v1/stock-rebalancing");

  public String getUrl() {
    return url;
  }

  public String getPath() {
    return url.concat(path);
  }

}
