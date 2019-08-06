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

package com.logistimo.api.servlets.mobile;

import com.google.gson.Gson;

import com.logistimo.api.response.Country;
import com.logistimo.api.response.District;
import com.logistimo.api.response.LocationMetadataResponse;
import com.logistimo.api.response.State;
import com.logistimo.api.servlets.SgServlet;
import com.logistimo.api.util.RESTUtil;
import com.logistimo.config.entity.IConfig;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.config.service.impl.ConfigurationMgmtServiceImpl;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.logger.XLog;
import com.logistimo.proto.RestConstantsZ;
import com.logistimo.services.ObjectNotFoundException;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserAccount;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LocationConfigServlet extends SgServlet {
  private static final long serialVersionUID = -4934073385876857714L;
  private static final String ACTION_GETLOCATIONCONFIG = "getlocationconfig";
  private static final String ACTION_FORMAT_OLD_LOCATIONCONFIG = "formatold";
  private static final XLog xLogger = XLog.getLog(LocationConfigServlet.class);

  @Override
  protected void processGet(HttpServletRequest request,
                            HttpServletResponse response,
                            ResourceBundle messages) throws ServletException, IOException,
      ServiceException {
    String action = request.getParameter(RestConstantsZ.ACTION);
    if (ACTION_GETLOCATIONCONFIG.equals(action)) {
      String userId = request.getParameter("userid");
      String password = request.getParameter("password");
      String key = request.getParameter("key");
      ///String authenticateResponse = authenticate(userId, password);
      try {
        IUserAccount u = RESTUtil.authenticate(userId, password, null, request, response);
        if (userId == null) // will be the case if BasicAuth is used
        {
          userId = u.getUserId();
        }
        // Get config. and send
        String config = getLocationConfig();
        if (StringUtils.isNotBlank(config)) {
          response.setContentType("text/plain; charset=UTF-8");
          LocationMetadataResponse h = new Gson().fromJson(config, LocationMetadataResponse.class);
          if (StringUtils.isNotBlank(key)) {
            writeResponse(response.getWriter(),
                new Gson().toJson(getSingleCountryResponse(key, h)));
          } else {
            writeResponse(response.getWriter(), new Gson().toJson(h));
          }
        }
      } catch (Exception e) {
        xLogger.warn("Exception: {0}", e);
        writeResponse(response.getWriter(), "Invalid user name or password");
      }
      /** OLD way of doing it
       if (StringUtils.isEmpty(authenticateResponse)){
       String config = getLocationConfig();
       if (StringUtils.isNotBlank(config)){
       response.setContentType("text/plain; charset=UTF-8");
       LocationMetadataResponse h = new Gson().fromJson(config, LocationMetadataResponse.class);
       if (StringUtils.isNotBlank(key)){
       writeResponse(response.getWriter(), new Gson().toJson(getSingleCountryResponse(key, h)));
       } else {
       writeResponse(response.getWriter(), new Gson().toJson(h));
       }
       }
       } else {
       writeResponse(response.getWriter(), authenticateResponse);
       }
       */
    } else if (ACTION_FORMAT_OLD_LOCATIONCONFIG.equals(action)) {
      String config = getLocationConfig();
      if (StringUtils.isNotBlank(config)) {
        response.setContentType("text/plain; charset=UTF-8");
        LocationMetadataResponse h = format(config);
        writeResponse(response.getWriter(), new Gson().toJson(h));
      } else {
        writeResponse(response.getWriter(), "config not found");
      }
    } else {
      xLogger.severe("Invalid action: " + action);
    }
  }

  private LocationMetadataResponse getSingleCountryResponse(String key,
                                                            LocationMetadataResponse fullLocationConfig) {
    LocationMetadataResponse r = new LocationMetadataResponse();
    r.countries = new HashMap<String, Country>();
    r.countries.put(key, fullLocationConfig.countries.get(key));
    return r;
  }

  private void writeResponse(PrintWriter pw, String responseText) {
    pw.write(responseText);
    pw.close();
  }

  private String getLocationConfig() {
    try {
      ConfigurationMgmtService cms =
          StaticApplicationContext.getBean(ConfigurationMgmtServiceImpl.class);
      IConfig config = cms.getConfiguration(IConfig.LOCATIONS);
      return config.getConfig();
    } catch (ServiceException e) {
      xLogger.severe("{0} when getting config. for key {1}: {2}", e.getClass().getName(),
          IConfig.LOCATIONS, e);
    } catch (ObjectNotFoundException e) {
      xLogger.warn("Configuration not available for key: " + IConfig.LOCATIONS);
    }
    return null;
  }

  @Override
  protected void processPost(HttpServletRequest request,
                             HttpServletResponse response,
                             ResourceBundle messages) throws ServletException, IOException,
      ServiceException {
  }

  private LocationMetadataResponse format(String config) {
    JSONObject locations;
    try {
      locations = new JSONObject(config);
      Map<String, List<String>> states = parseStates(locations.getJSONArray("states"));
      Map<String, List<String>> districts = parseDistricts(locations.getJSONArray("districts"));
      Map<String, List<String>> taluks = parseTaluks(locations.getJSONArray("taluks"));
      LocationMetadataResponse h = new LocationMetadataResponse();
      h.countries = new HashMap<>();
      for (String country : states.keySet()) {
        Country c = new Country();
        c.name = country;
        c.states = new HashMap<String, State>();
        List<String> st = states.get(country);
        if (st != null && !st.isEmpty()) {
          for (String s : st) {
            State a = new State();
            a.districts = getDistricts(country, s, districts, taluks);
            c.states.put(s, a);
          }
        }
        h.countries.put(country, c);
      }
      return h;
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Map<String, District> getDistricts(String country, String state,
                                             Map<String, List<String>> districts,
                                             Map<String, List<String>> taluks) {
    List<String> dists = districts.get(country + "." + state);
    HashMap<String, District> typeSafe = new HashMap<String, District>();
    if (dists != null && !dists.isEmpty()) {
      for (String d : dists) {
        District dt = new District();
        dt.taluks = taluks.get(country + "." + state + "." + d);
        typeSafe.put(d, dt);
      }
    }
    return typeSafe;
  }

  private Map<String, List<String>> parseTaluks(JSONArray taluks) throws JSONException {
    Map<String, List<String>> m = new HashMap<String, List<String>>();
    for (int i = 0; i < taluks.length(); i++) {
      JSONObject o = taluks.getJSONObject(i);
      Iterator<String> keys = o.keys();
      while (keys.hasNext()) {
        String k = keys.next();
        String value = o.getString(k);
        //value is a list of taluks
        JSONArray jsonArray = new JSONArray(value);
        List<String> l = new ArrayList<String>();
        for (int j = 0; j < jsonArray.length(); j++) {
          l.add(jsonArray.getString(j));
        }
        m.put(k, l);
      }
    }
    return m;
  }

  private Map<String, List<String>> parseStates(JSONArray states) throws JSONException {
    Map<String, List<String>> m = new HashMap<String, List<String>>();
    for (int i = 0; i < states.length(); i++) {
      JSONObject o = states.getJSONObject(i);
      Iterator<String> keys = o.keys();
      while (keys.hasNext()) {
        String k = keys.next();
        String value = o.getString(k);
        //value is a list of taluks
        JSONArray jsonArray = new JSONArray(value);
        List<String> l = new ArrayList<String>();
        for (int j = 0; j < jsonArray.length(); j++) {
          l.add(jsonArray.getString(j));
        }
        m.put(k, l);
      }
    }
    return m;
  }

  private Map<String, List<String>> parseDistricts(JSONArray districts) throws JSONException {
    Map<String, List<String>> m = new HashMap<String, List<String>>();
    for (int i = 0; i < districts.length(); i++) {
      JSONObject o = districts.getJSONObject(i);
      Iterator<String> keys = o.keys();
      while (keys.hasNext()) {
        String k = keys.next();
        String value = o.getString(k);
        JSONArray jsonArray = new JSONArray(value);
        List<String> l = new ArrayList<String>();
        for (int j = 0; j < jsonArray.length(); j++) {
          l.add(jsonArray.getString(j));
        }
        m.put(k, l);
      }
    }
    return m;
  }


}
