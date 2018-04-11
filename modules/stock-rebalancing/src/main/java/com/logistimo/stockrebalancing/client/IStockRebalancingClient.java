package com.logistimo.stockrebalancing.client;

import com.logistimo.stockrebalancing.client.internal.models.Configuration;

/**
 * Created by nitisha.khandelwal on 19/03/18.
 */

public interface IStockRebalancingClient {

  void triggerStockRebalancing(String domainId, Configuration configuration);
}
