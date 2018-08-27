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

package com.logistimo.exports.handlers;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.UserAccount;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;

/**
 * Created by vani on 14/08/18.
 */

public class UserExportHandlerTest {
  UserExportHandler userExportHandler;
  private static final String PERMISSION_DEFAULT = "Default";
  private static final String PERMISSION_VIEW_ONLY = "View only";
  private static final String PERMISSION_ASSET_VIEW_ONLY = "Asset view only";
  private static final String THEME_SAME_AS_IN_DOMAIN_CONFIG = "Same as in domain configuration";
  private static final String THEME_DEFAULT = "Default";
  private static final String THEME_SIDEBAR_AND_LANDING_SCREEN = "Sidebar & Landing screen";

  ResourceBundle mockResourceBundle = new ResourceBundle() {
    @Override
    protected Object handleGetObject(String key) {
      String value = null;
      switch(key) {
        case "default.caps":
          value = PERMISSION_DEFAULT;
          break;
        case "user.permission.view":
          value = PERMISSION_VIEW_ONLY;
          break;
        case "user.permission.asset":
          value = PERMISSION_ASSET_VIEW_ONLY;
          break;
        case "gui.theme.default":
          value = THEME_DEFAULT;
          break;
        case "gui.theme.same.as.in.domain.configuration":
          value = THEME_SAME_AS_IN_DOMAIN_CONFIG;
          break;
        case "sidebar.landing.screen":
          value = THEME_SIDEBAR_AND_LANDING_SCREEN;
          break;
      }
      return value;
    }

    @Override
    public Enumeration<String> getKeys() {
      return Collections.emptyEnumeration();
    }
  };

  @Before
  public void setup() {
    userExportHandler = new UserExportHandler(new UserAccount());
  }

  @Test
  public void testGetPermissionForValidInputs() throws Exception {
    assertEquals(
        userExportHandler.getPermission(IUserAccount.PERMISSION_DEFAULT, mockResourceBundle),
        PERMISSION_DEFAULT);
    assertEquals(
        userExportHandler.getPermission(IUserAccount.PERMISSION_VIEW, mockResourceBundle),
        PERMISSION_VIEW_ONLY);
    assertEquals(
        userExportHandler.getPermission(IUserAccount.PERMISSION_ASSET, mockResourceBundle),
        PERMISSION_ASSET_VIEW_ONLY);
  }

  @Test
  public void testGetPermissionForInvalidInputs() throws Exception {
    assertEquals(userExportHandler.getPermission("Invalid",mockResourceBundle), CharacterConstants.EMPTY);
  }

  @Test
  public void testGetStoreAppThemeForValidInputs() throws Exception {
    assertEquals(userExportHandler.getStoreAppTheme(-1,mockResourceBundle),THEME_SAME_AS_IN_DOMAIN_CONFIG);
    assertEquals(userExportHandler.getStoreAppTheme(0,mockResourceBundle),THEME_DEFAULT);
    assertEquals(userExportHandler.getStoreAppTheme(1,mockResourceBundle),THEME_SIDEBAR_AND_LANDING_SCREEN);
  }

  @Test
  public void testGetStoreAppThemeForInvalidInputs() throws Exception {
    assertEquals(userExportHandler.getStoreAppTheme(10,mockResourceBundle),CharacterConstants.EMPTY);
  }
}