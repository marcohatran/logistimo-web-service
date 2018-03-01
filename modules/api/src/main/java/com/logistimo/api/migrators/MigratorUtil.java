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

import com.logistimo.constants.CharacterConstants;
import com.logistimo.logger.XLog;
import com.logistimo.services.utils.ConfigUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mohan Raja
 */
public class MigratorUtil {

  public static final String CONFIG_QUERY =
      "SELECT `KEY`, cast(CONF as CHAR) as conf FROM CONFIG WHERE `KEY`";

  private static final XLog xLog = XLog.getLog(MigratorUtil.class);
  private static Connection connection;

  /**
   * Get the connection; initialise and get, if not initialised already
   *
   * @return
   * @throws SQLException
   * @throws ClassNotFoundException
   */
  public static Connection getConnection() throws SQLException, ClassNotFoundException {
    if(connection == null) {
      initConnection();
    }
    return connection;
  }

  /**
   * Initialise the connection
   *
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  private static void initConnection() throws ClassNotFoundException, SQLException {
    Class.forName("org.mariadb.jdbc.Driver");
    connection =
        DriverManager.getConnection(ConfigUtil.get("db.url"), ConfigUtil.get("db.user"),
            ConfigUtil.get("db.password"));
  }

  /**
   * Read all the domain config like 'config.%' but not 'config.kiosk.%'
   *
   * @param keys - optional. If not provided all config will be read; else only the specified ones
   * @return
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public static Map<String, String> readConfig(List<String> keys)
      throws ClassNotFoundException, SQLException {
    Map<String, String> conf = new HashMap<>();
    String sql;
    if (keys == null) {
      // for updating all configs
      sql = CONFIG_QUERY + " LIKE 'config.%' AND `KEY` NOT LIKE 'config.kiosk.%'";
    } else {
      boolean isFirst = true;
      StringBuilder str = new StringBuilder();
      for (String key : keys) {
        if (key.startsWith("config.") && !key.startsWith("config.kiosk.")) {
          if (!isFirst) {
            str.append(CharacterConstants.COMMA);
          }
          isFirst = false;
          str.append(CharacterConstants.SINGLE_QUOTES).append(key)
              .append(CharacterConstants.SINGLE_QUOTES);
        } else {
          xLog.info("Migration config: Invalid key found ", key);
        }
      }
      if (str.length() == 0) {
        return null;
      }
      // for specific domain ids.
      sql = CONFIG_QUERY + " IN (" + str.toString() + ")";
    }
    if(connection == null) {
      initConnection();
    }
    PreparedStatement ps = connection.prepareStatement(sql);
    ResultSet resultset = ps.executeQuery();
    while (resultset.next()) {
      conf.put(resultset.getString("key"), resultset.getString("conf"));
    }
    return conf;
  }
}
