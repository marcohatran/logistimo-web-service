/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */

/**
 *
 */
package com.logistimo.config.service.impl;

import com.google.gson.Gson;

import com.logistimo.AppFactory;
import com.logistimo.config.entity.Config;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.config.models.DomainConfig;
import com.logistimo.config.models.LocationConfig;
import com.logistimo.config.models.TransportersConfig;
import com.logistimo.config.models.TransportersSystemConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.dao.JDOUtils;
import com.logistimo.entity.comparator.LocationComparator;
import com.logistimo.locations.client.LocationClient;
import com.logistimo.locations.model.LocationResponseModel;
import com.logistimo.logger.XLog;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.services.cache.MemcacheService;
import com.logistimo.services.impl.PMF;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

/**
 * @author arun
 */
@Service
public class ConfigurationMgmtServiceImpl implements ConfigurationMgmtService {

  private static final XLog xLogger = XLog.getLog(ConfigurationMgmtServiceImpl.class);

  private LocationClient locationClient;

  @Autowired
  public void setLocationClient(LocationClient locationClient) {
    this.locationClient = locationClient;
  }

  /**
   * Add a new configuration to the data store
   *
   * @param config The configuration object along with its key
   * @throws ServiceException Thrown if the object or its key is invalid
   */
  public void addConfiguration(String key, IConfig config) throws ServiceException {
    addConfiguration(key, null, config);
  }

  /**
   * Add a domain-specific configuration, if domainId is not null without persistence manager
   */
  public void addConfiguration(String key, Long domainId, IConfig config) throws ServiceException {
    addConfiguration(key, domainId, config, null);
  }

  /**
   * Add a domain-specific configuration, if domainId is not null with persistence manager
   */
  public void addConfiguration(String key, Long domainId, IConfig config, PersistenceManager pm) throws ServiceException {
    if (config == null || config.getConfig() == null) {
      throw new ServiceException("Invalid configuration object - the object or its value are null");
    }

    if (key == null || key.isEmpty()) {
      throw new ServiceException(
          "Invalid configuration key - key cannot be null or an empty string");
    }

    // Save the config object to data store
    PersistenceManager localPM = pm;
    if(localPM == null) {
      localPM = PMF.get().getPersistenceManager();
    }
    try {
      // Check if a config with this key already exists
      try {
        String realKey = getRealKey(key, domainId);
        JDOUtils.getObjectById(IConfig.class, realKey, localPM);
        // If it comes here, then this object is found, so throw an exception, given duplication
        throw new ServiceException("An entry with exists for key: " + key);
      } catch (JDOObjectNotFoundException e) {
        // This key does not exist; so it is valid to add a config with this key
      }
      //add loc ids
      updateDomainConfigLocIds(config);
      config.setKey(key);
      config.setConfig(getCleanString(config.getConfig()));
      config.setLastUpdated(new Date());
      localPM.makePersistent(config);
      localPM.detachCopy(config);
    } catch (Exception e) {
      xLogger.fine("Exception while adding configuation object: {0}", e.getMessage());
      throw new ServiceException(e);
    } finally {
      if(pm == null) {
        localPM.close();
      }
    }
  }

  /**
   * Get the configuration object, given a key
   *
   * @param key The key for the config. object
   * @return The config. object corresponding to the given key
   * @throws ObjectNotFoundException If the config. object for the given key was not found
   * @throws ServiceException        Any invalid parameter or data retrieval exceptions
   */
  public IConfig getConfiguration(String key) throws ServiceException {
    return getConfiguration(key, null);
  }

  /**
   * Get domain-specific configurtion, if domain is specified
   */
  public IConfig getConfiguration(String key, Long domainId)
      throws ServiceException {
    if (key == null || key.isEmpty()) {
      throw new ServiceException("Invalid key: " + key);
    }
    IConfig config = null;
    PersistenceManager pm = getPM();
    try {
      String realKey = getRealKey(key, domainId);
      config = JDOUtils.getObjectById(IConfig.class, realKey, pm);
      config = pm.detachCopy(config);
    } catch (JDOObjectNotFoundException e) {
      xLogger.warn("Config object not found for key: {0}", key);
      throw new ObjectNotFoundException(e);
    } finally {
      pm.close();
    }

    return config;
  }

  protected PersistenceManager getPM() {
    return PMF.get().getPersistenceManager();
  }

  /**
   * Update a given configuration object.
   *
   * @param config The configuration object to be updated.
   * @throws ServiceException Thrown if an invalid object or key was passed.
   */
  public void updateConfiguration(IConfig config) throws ServiceException {
    if (config == null || config.getConfig() == null) {
      throw new ServiceException("Invalid configuration object - the object or its value are null");
    }

    if (config.getKey() == null || config.getKey().isEmpty()) {
      throw new ServiceException(
          "Invalid configuration key - key cannot be null or an empty string");
    }

    // Save the config object to data store
    PersistenceManager pm = PMF.get().getPersistenceManager();
    Transaction tx = pm.currentTransaction();
    try {
      tx.begin();
      IConfig c = JDOUtils.getObjectById(IConfig.class, config.getKey(), pm);
      c.setPrevConfig(c.getConfig()); // backup the current configuration before updating
      c.setConfig(
          getCleanString(config.getConfig())); // update the current configuration with the new one
      c.setUserId(config.getUserId());
      c.setDomainId(config.getDomainId());
      c.setLastUpdated(new Date());
      int locindex = compareLocationChange(c);
      //update loc ids
      if (locindex != 0) {
        updateDomainConfigLocIds(c);
      }
      //pm.makePersistent( c );
      tx.commit();
      // whenever there is a change in the location configuration, re-initialize it
      if (c.getKey().equals(IConfig.LOCATIONS)) {
        LocationConfig.initialize();
      }
    } catch (Exception e) {
      xLogger
          .fine("Exception while updating configuration object with key {0}: {1}", config.getKey(),
              e.getMessage(), e);
      throw new ServiceException(e.getMessage());
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      pm.close();
    }
  }


  /**
   * Copy configuration from one domain to another
   */
  public void copyConfiguration(Long srcDomainId, Long destDomainId) throws ServiceException {
    xLogger.fine("Entered copyConfiguration");
    // Get destination configuration
    IConfig srcConfig = null;
    // Check if source configuration exists
    try {
      srcConfig = getConfiguration(createConfigKey(srcDomainId));
    } catch (ObjectNotFoundException e) {
      xLogger.warn("No configuration found to copy (to dest. domain {0}) in source domain {1}",
          destDomainId, srcDomainId);
      return; // nothing to do, really
    }
    // Create or update destination domain configuration with source domain configuration
    IConfig destConfig = null;
    try {
      destConfig = getConfiguration(createConfigKey(destDomainId)).copyFrom(srcConfig);
      destConfig.setDomainId(destDomainId);
      destConfig.setLastUpdated(new Date());
      updateConfiguration(destConfig);
    } catch (ObjectNotFoundException e) { // destination configuration not found; add one
      destConfig = JDOUtils.createInstance(IConfig.class).init(srcConfig);
      destConfig.setDomainId(destDomainId);
      destConfig.setLastUpdated(new Date());
      addConfiguration(createConfigKey(destDomainId), destConfig);
    }
    MemcacheService cache = AppFactory.get().getMemcacheService();
    if (cache != null) {
      try {
        destConfig = getConfiguration(createConfigKey(destDomainId));
        cache.put(DomainConfig.getCacheKey(destDomainId), new DomainConfig(destConfig.getConfig()));
      } catch (Exception e) {
        xLogger.severe("Failed to update config cache for domain  {0}", destDomainId, e);
      }
    }
    xLogger.fine("Exiting copyConfiguration");
  }

  public static String createConfigKey(Long domainId) {
    return IConfig.CONFIG_PREFIX + domainId;
  }

  // Private method to remove new line characters
  private String getCleanString(String inputString) {
    xLogger.fine("Entering getCleanString");
    String str = null;
    if (inputString != null) {
      str = inputString.replaceAll("\r\n|\n", "");
    }
    xLogger.fine("Exiting getCleanString");
    return str;
  }

  // Get a domain-specific or generic configuration key, depending on whether domainId was specified
  private String getRealKey(String key, Long domainId) {
    String realKey = key;
    if (domainId != null) {
      realKey += "." + domainId.toString();
    }
    return realKey;
  }

  private void updateDomainConfigLocIds(IConfig dc) throws ConfigurationException {
    DomainConfig config = new DomainConfig(dc.getConfig());
    config.setUser(dc.getUserId());
    LocationResponseModel response = locationClient.getLocationIds(config);
    config.setCountryId(response.getCountryId());
    config.setStateId(response.getStateId());
    config.setDistrictId(response.getDistrictId());
    dc.setConfig(config.toJSONSring());
  }

  private int compareLocationChange(IConfig d) throws ConfigurationException {
    DomainConfig dc1 = new DomainConfig(d.getPrevConfig());
    DomainConfig dc2 = new DomainConfig(d.getConfig());
    return new LocationComparator().compare(dc1, dc2);
  }

  public void addDefaultDomainConfig(Long domainId, String country, String state, String district,
                                     String timezone, String userId, PersistenceManager pm)
      throws ConfigurationException, ServiceException {
    try {
      DomainConfig domainConfig = new DomainConfig();
      List<String> list = new ArrayList<>(2);
      list.add(userId);
      list.add(String.valueOf(System.currentTimeMillis()));
      domainConfig.addDomainData("General", list);
      domainConfig.setCountry(country);
      domainConfig.setState(state);
      domainConfig.setDistrict(district);
      domainConfig.setTimezone(timezone);

      IConfig config = new Config();
      config.setDomainId(domainId);
      config.setConfig(domainConfig.toJSONSring());
      config.setKey(IConfig.CONFIG_PREFIX + domainId);
      config.setUserId(userId);
      config.setLastUpdated(new Date());
      addConfiguration(IConfig.CONFIG_PREFIX + domainId, config);
      xLogger.info("Domain: {0} default config added successfully.",domainId);
    } catch (Exception e) {
      xLogger.warn("Error while saving default configuration for domain: {0}", domainId, e);
      throw e;
    }
  }

  public TransportersSystemConfig getTransporterConfig(Long dId, boolean enabled) throws
      ServiceException {
    IConfig config = getConfiguration(IConfig.TRANSPORTER_CONFIG);
    TransportersSystemConfig tConfig =
        new Gson().fromJson(config.getConfig(), TransportersSystemConfig.class);
    if (enabled && CollectionUtils.isNotEmpty(tConfig.getTransporters())) {
      keepOnlyEnabledTransporters(dId, tConfig);
    }
    return tConfig;
  }

  private void keepOnlyEnabledTransporters(Long domainId, TransportersSystemConfig tConfig) {
    TransportersConfig config = DomainConfig.getInstance(domainId)
        .getTransportersConfig();
    if (CollectionUtils.isNotEmpty(config.getEnabledTransporters())) {
      Set<String> enabledTSPIds = config.getEnabledTransporters().stream()
          .map(TransportersConfig.TSPConfig::getTspId)
          .collect(Collectors.toSet());
      tConfig.setTransporters(tConfig.getTransporters().stream()
          .filter(s -> enabledTSPIds.contains(s.getId()))
          .collect(Collectors.toList()));
    } else {
      tConfig.setTransporters(new ArrayList<>());
    }
  }
}
