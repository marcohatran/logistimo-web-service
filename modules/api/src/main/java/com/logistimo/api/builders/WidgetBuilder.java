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

package com.logistimo.api.builders;

import com.logistimo.api.models.WidgetConfigModel;
import com.logistimo.api.models.WidgetModel;
import com.logistimo.dao.JDOUtils;
import com.logistimo.dashboards.entity.IWidget;
import com.logistimo.dashboards.service.IDashboardService;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.users.service.UsersService;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Mohan Raja
 */
@Component
public class WidgetBuilder {

  private IDashboardService dashboardService;
  private UsersService usersService;
  private FChartBuilder fChartBuilder;

  @Autowired
  public void setDashboardService(IDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @Autowired
  public void setUsersService(UsersService usersService) {
    this.usersService = usersService;
  }

  @Autowired
  public void setfChartBuilder(FChartBuilder fChartBuilder) {
    this.fChartBuilder = fChartBuilder;
  }

  public IWidget buildWidget(WidgetModel model, long domainId, String userName) {
    IWidget wid = JDOUtils.createInstance(IWidget.class);
    wid.setdId(domainId);
    wid.setCreatedOn(new Date());
    wid.setCreatedBy(userName);
    wid.setName(model.nm);
    wid.setDesc(model.desc);
    return wid;
  }

  public List<WidgetModel> buildWidgetModelList(List<IWidget> dbList) {
    List<WidgetModel> models = new ArrayList<>(dbList.size());
    models.addAll(dbList.stream().map(iWidget -> buildWidgetModel(iWidget, false))
        .collect(Collectors.toList()));
    return models;
  }

  public WidgetModel buildWidgetModel(IWidget wid, boolean deep) {
    WidgetModel model = new WidgetModel();
    model.wId = wid.getwId();
    model.dId = wid.getdId();
    model.nm = wid.getName();
    if (deep) {
      try {
        model.cByNm = usersService.getUserAccount(wid.getCreatedBy()).getFullName();
        model.cBy = wid.getCreatedBy();
        model.cOn = wid.getCreatedOn();
        model.desc = wid.getDesc();
      } catch (ObjectNotFoundException ignored) {
      }
    }
    return model;
  }

  public IWidget updateWidgetConfig(WidgetConfigModel model)
      throws ServiceException {
    IWidget wid = dashboardService.getWidget(model.wId);
    wid.setTitle(model.tit);
    wid.setSubtitle(model.stit);
    wid.setType(model.ty);
    wid.setFreq(model.fq);
    wid.setNop(model.nop);
    wid.setAggr(model.ag);
    wid.setAggrTy(model.agTy);
    wid.setyLabel(model.ya);
    wid.setExpEnabled(model.ee);
    wid.setShowLeg(model.sl);
    return wid;
  }

  public WidgetConfigModel getWidgetConfig(IWidget wid, boolean isData) throws ServiceException {
    WidgetConfigModel model = new WidgetConfigModel();
    model.wId = wid.getwId();
    model.nm = wid.getName();
    model.tit = wid.getTitle();
    model.stit = wid.getSubtitle();
    model.ty = wid.getType();
    model.fq = wid.getFreq();
    model.nop = wid.getNop();
    model.ag = wid.getAggr();
    model.agTy = wid.getAggrTy();
    model.ya = wid.getyLabel();
    model.ee = wid.getExpEnabled();
    model.sl = wid.getShowLeg();
    if (isData) {
      model.opt = constructChartOptions(wid);
      model.data = fChartBuilder.getAggrData(wid.getdId(), model.fq, model.nop, model.ag);
    }
    return model;
  }

  private String constructChartOptions(IWidget wid) {
    StringBuilder options = new StringBuilder();
    options.append("{");
    options.append("\"theme\": \"fint\"");
    if (wid.getExpEnabled() != null && wid.getExpEnabled()) {
      options.append(",\"exportEnabled\": 1");
    }
    if (wid.getShowLeg() == null || !wid.getShowLeg()) {
      options.append(",\"showLegend\": 0");
    }
    if (StringUtils.isNotEmpty(wid.getyLabel())) {
      options.append(",\"yAxisName\":\"").append(wid.getyLabel()).append("\"");
    } else if (StringUtils.isNotEmpty(wid.getTitle())) {
      options.append(",\"yAxisName\":\"").append(wid.getTitle()).append("\"");
    } else {
      options.append(",\"yAxisName\":\"").append(wid.getName()).append("\"");
    }
    if (StringUtils.isNotEmpty(wid.getTitle())) {
      options.append(",\"caption\":\"").append(wid.getTitle()).append("\"");
    }
    if (StringUtils.isNotEmpty(wid.getSubtitle())) {
      options.append(",\"subCaption\":\"").append(wid.getSubtitle()).append("\"");
    }
//        options.append(",\"showValues\":\"").append("0").append("\"");
    options.append("}");
    return options.toString();
  }
}
