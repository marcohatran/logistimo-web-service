/*
 * Copyright © 2018 Logistimo.
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

package com.logistimo.api.migrators;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.logistimo.config.models.ReasonConfig;
import com.logistimo.constants.CharacterConstants;
import com.logistimo.logger.XLog;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mohan Raja
 */
public class ConfigReasonsMigrator {
  public static final String CONFIG_UPDATE_QUERY = "UPDATE CONFIG SET CONF=? WHERE `KEY`=?";
  public static final XLog xLog = XLog.getLog(CRConfigMigrator.class);

  private ConfigReasonsMigrator() {
  }

  /**
   * Update configuration for all keys
   */
  public static boolean update() {
    return update((List<String>) null, false);
  }

  /**
   * Update configuration for a key.
   *
   * @param key - Config key to migrate
   */
  public static boolean update(String key) {
    return update(Collections.singletonList(key), false);
  }

  public static boolean update(String key, boolean isJSONObject) {
    return update(Collections.singletonList(key), isJSONObject);
  }

  /**
   * Update configurations for a list of keys
   *
   * @param keys - list of config key to migrate
   */
  public static boolean update(List<String> keys, boolean isJSONObject) {
    PreparedStatement ps = null;
    try {
      Map<String, String> conf = MigratorUtil.readConfig(keys);
      if (conf == null) {
        return false;
      }
      List<String> updatedConfigKeys = new ArrayList<>(0);
      for (Map.Entry<String, String> configuration : conf.entrySet()) {
        xLog.info("Parsing config for domain {0}", configuration.getKey());
        JSONObject config = new JSONObject(configuration.getValue());
        JSONObject inventory = (JSONObject) config.get("invntry");
        List<String> reasonKeys =
            Arrays.asList("trsns", "imtrsns", "rmtrsns", "smtrsns", "tmtrsns", "dmtrsns", "rimtrsns", "romtrsns");
        boolean alreadyUpdated = false;
        for (String reasonKey : reasonKeys) {
          if (!inventory.has(reasonKey)) {
            continue;
          }
          Object object = inventory.get(reasonKey);
          //Already new config
          if(object instanceof String) {
            xLog.info("New configuration already set for: {0}", configuration.getKey());
            updatedConfigKeys.add(configuration.getKey());
            alreadyUpdated = true;
            break;
          }
          JSONObject reasonByType = (JSONObject) inventory.get(reasonKey);
          if (isJSONObject) {
            for (Object key : reasonByType.keySet()) {
              String type = (String) key;
              Object value = reasonByType.get(type);
              if (value instanceof JSONObject) {
                JSONObject reasonConfig = (JSONObject) value;
                JSONArray array = reasonConfig.getJSONArray("reasons");
                reasonByType.put(type, array.join(","));
              } else {
                xLog.info("New configuration already set for: {0}", configuration.getKey());
                updatedConfigKeys.add(type);
                alreadyUpdated = true;
                break;
              }
            }
          } else {
            Map<String,ReasonConfig> typeMap=new HashMap<>();
            for (Object key : reasonByType.keySet()) {
              String type = (String) key;
              Object value = reasonByType.get(type);
              if (value instanceof String) {
                String oldReason = (String) value;
                List<String> newReason;
                if (StringUtils.isNotBlank(oldReason)) {
                  if (oldReason.startsWith(CharacterConstants.COMMA)) {
                    oldReason = oldReason.substring(1);
                  }
                  newReason = Arrays.asList(oldReason.split(CharacterConstants.COMMA));
                } else {
                  newReason = new ArrayList<>(0);
                }
                JSONObject reasonConfig = new JSONObject();
                reasonConfig.put("reasons", newReason);
                ReasonConfig reasonConfig1=new ReasonConfig();
                reasonConfig1.setReasons(newReason);
                typeMap.put(type,reasonConfig1);
              } else {
                xLog.info("New configuration already set for: {0}", configuration.getKey());
                updatedConfigKeys.add(configuration.getKey());
                alreadyUpdated = true;
                break;
              }
            }
            Type type = new TypeToken<Map<String,ReasonConfig>>(){}.getType();
            inventory.put(reasonKey,new Gson().toJson(typeMap,type));
          }
          if (alreadyUpdated) {
            break;
          }
        }
        if (!alreadyUpdated) {
          conf.put(configuration.getKey(), String.valueOf(config));
        }
      }
      //update
      ps = MigratorUtil.getConnection().prepareStatement(CONFIG_UPDATE_QUERY);
      for (Map.Entry<String, String> confEntry : conf.entrySet()) {
        if (!updatedConfigKeys.contains(confEntry.getKey())) {
          ps.setString(1, confEntry.getValue());
          ps.setString(2, confEntry.getKey());
          ps.addBatch();
        }
      }
      int[] count = ps.executeBatch();
      xLog.info("{0} domains updated out of {1}", count.length, conf.size());
    } catch (Exception e) {
      xLog.warn("Error in updating configuration: " + e);
      return false;
    } finally {
      if (ps != null) {
        try {
          ps.close();
        } catch (SQLException ignored) {
          xLog.warn("Exception while closing prepared statement", ignored);
        }
      }
    }
    return true;
  }
}
