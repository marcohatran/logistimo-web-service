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

package com.logistimo.bulkuploads;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.utils.FieldLimits;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by vani on 31/07/18.
 */
public class BulkUploadMgrTest {

  @Test
  public void testIsGenderValidWithValidInputs() throws Exception {
    assertTrue(BulkUploadMgr.isGenderValid(CharacterConstants.EMPTY));
    assertTrue(BulkUploadMgr.isGenderValid(IUserAccount.GENDER_MALE));
    assertTrue(BulkUploadMgr.isGenderValid(IUserAccount.GENDER_FEMALE));
    assertTrue(BulkUploadMgr.isGenderValid(IUserAccount.GENDER_OTHER));
    assertTrue(BulkUploadMgr.isGenderValid("M"));
    assertTrue(BulkUploadMgr.isGenderValid("F"));
    assertTrue(BulkUploadMgr.isGenderValid("O"));
  }

  @Test
  public void testIsGenderValidWithInvalidInputs() throws Exception {
    assertFalse(BulkUploadMgr.isGenderValid("invalid gender"));
  }

  @Test
  public void testIsDateOfBirthValidForValidInputs() throws Exception {
    assertTrue(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(2018, 8, 1), LocalDate.of(2018, 8, 1)));
    assertTrue(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(2018,7,1),LocalDate.of(2018,8,1)));
    assertTrue(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(2000,7,1),LocalDate.of(2018,8,1)));
    assertTrue(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(1918,8,1),LocalDate.of(2018,8,1)));
    assertTrue(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(1918,9,1),LocalDate.of(2018,8,1)));
  }

  @Test
  public void testIsDateOfBirthValidForInvalidInputs() throws Exception {
    assertFalse(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 8, 1)));
    assertFalse(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(1918,7,1),LocalDate.of(2018,8,1)));
    assertFalse(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(1918,7,31),LocalDate.of(2018,8,1)));
    assertFalse(BulkUploadMgr.isDateOfBirthValid(LocalDate.of(1917,8,1),LocalDate.of(2018,8,1)));
  }

  @Test
  public void testIsOperationValidForInvalidInputs() throws Exception {
    assertFalse(BulkUploadMgr.isOperationValid(""));
    assertFalse(BulkUploadMgr.isOperationValid(null));
    assertFalse(BulkUploadMgr.isOperationValid("invalid"));
  }

  @Test
  public void testIsOperationValidForValidInputs() throws Exception {
    assertTrue(BulkUploadMgr.isOperationValid(BulkUploadMgr.OP_ADD));
    assertTrue(BulkUploadMgr.isOperationValid(BulkUploadMgr.OP_EDIT));
    assertTrue(BulkUploadMgr.isOperationValid(BulkUploadMgr.OP_DELETE));
  }

  @Test
  public void testIsPermissionValidForInvalidInput() throws Exception {
    assertFalse(BulkUploadMgr.isPermissionValid("invalid"));
  }
  @Test
  public void testIsPermissionValidForValidInput() throws Exception {
    assertTrue(BulkUploadMgr.isPermissionValid(""));
    assertTrue(BulkUploadMgr.isPermissionValid(IUserAccount.PERMISSION_DEFAULT));
    assertTrue(BulkUploadMgr.isPermissionValid(IUserAccount.PERMISSION_VIEW));
    assertTrue(BulkUploadMgr.isPermissionValid(IUserAccount.PERMISSION_ASSET));
  }

  @Test
  public void testIsTokenExpiryValidForInvalidInput() throws Exception {
    assertFalse(BulkUploadMgr.isTokenExpiryValid("invalid"));
    assertFalse(BulkUploadMgr.isTokenExpiryValid("1000"));
    assertFalse(BulkUploadMgr.isTokenExpiryValid("-1"));
  }
  @Test
  public void testIsTokenExpiryValidForValidInput() throws Exception {
    assertTrue(BulkUploadMgr.isTokenExpiryValid(""));
    assertTrue(BulkUploadMgr.isTokenExpiryValid("0"));
    assertTrue(BulkUploadMgr.isTokenExpiryValid("999"));
    assertTrue(BulkUploadMgr.isTokenExpiryValid("500"));
  }

  @Test
  public void testIsGuiThemeValidForValidInput() throws Exception {
    assertTrue(BulkUploadMgr.isGuiThemeValid(""));
    assertTrue(BulkUploadMgr.isGuiThemeValid(String.valueOf(FieldLimits.GUI_THEME_SAME_AS_IN_DOMAIN_CONFIGURATION)));
    assertTrue(BulkUploadMgr.isGuiThemeValid(String.valueOf(FieldLimits.GUI_THEME_DEFAULT)));
    assertTrue(BulkUploadMgr.isGuiThemeValid(String.valueOf(
        FieldLimits.GUI_THEME_SIDEBAR_AND_LANDING_SCREEN)));
  }

  @Test
  public void testIsGuiThemeValidForInvalidInput() throws Exception {
    assertFalse(BulkUploadMgr.isGuiThemeValid("invalid"));
  }
}