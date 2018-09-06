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

package com.logistimo.sql;

import com.sun.rowset.CachedRowSetImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

public class PreparedStatementExecutor {


  private static ResultSet executeQuery(PreparedStatementModel model, PreparedStatement statement)
      throws SQLException {
    int index = 1;
    for (ParamModel param : model.getParams()) {
      switch (param.getParamType()) {
        case INT:
          statement.setInt(index, (Integer) param.getValue());
          break;
        case LONG:
          statement.setLong(index, (Long) param.getValue());
          break;
        case STRING:
        default:
          statement.setString(index, (String) param.getValue());
      }
      index++;
    }
    return statement.executeQuery();
  }

  public static CachedRowSet executeRowSet(Connection connection, PreparedStatementModel model)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(model.getQuery())) {
      CachedRowSetImpl rowSet = new CachedRowSetImpl();
      rowSet.populate(executeQuery(model, statement));
      return rowSet;
    }
  }
}
