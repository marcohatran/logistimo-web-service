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

import com.logistimo.constants.CharacterConstants;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import lombok.Singular;

@Data
public class PreparedStatementModel {

  private String query;

  @Singular
  private List<ParamModel> params = new LinkedList<>();

  public void param(ParamModel model) {
    this.params.add(model);
  }

  public void param(Integer value) {
    this.params.add(new ParamModel(ParamModel.PARAM_TYPE.INT, value));
  }

  public void param(Long value) {
    this.params.add(new ParamModel(ParamModel.PARAM_TYPE.LONG, value));
  }

  public void param(String value) {
    this.params.add(new ParamModel(ParamModel.PARAM_TYPE.STRING, value));
  }

  public void inParams(String csv,
                       StringBuilder queryBuilder) {
    inParams(csv, queryBuilder,true);
  }

  private void inParams(String csv,
                        StringBuilder queryBuilder, boolean removeEnclosingQuotes) {
    queryBuilder.append(CharacterConstants.O_BRACKET);
    for (String tag : csv.split(CharacterConstants.COMMA)) {
      queryBuilder.append(CharacterConstants.QUESTION)
          .append(CharacterConstants.COMMA);
      param(removeEnclosingQuotes?stripQuotes(tag):tag);
    }
    queryBuilder.setLength(queryBuilder.length() - 1);
    queryBuilder.append(CharacterConstants.C_BRACKET);
  }

  String stripQuotes(String text){
    if(!text.startsWith(CharacterConstants.S_QUOTE)){
      return text;
    }
    if(text.lastIndexOf(CharacterConstants.S_QUOTE) != text.length()-1){
      return text;
    }
    return text.substring(1,text.length()-1);
  }
}
