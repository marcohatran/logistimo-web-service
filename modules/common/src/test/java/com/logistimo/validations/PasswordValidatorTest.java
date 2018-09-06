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

package com.logistimo.validations;

import com.logistimo.auth.SecurityConstants;
import com.logistimo.exception.ValidationException;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by pratheeka on 14/08/18.
 */


public class PasswordValidatorTest {



  @Test(expected = ValidationException.class)
  public void testInvalidAdminPassword(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_DOMAINOWNER,"dsfsffsf");
  }

  @Test(expected = ValidationException.class)
  public void testInvalidAdminPasswordForNumbers(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_DOMAINOWNER,"dsfsffsfD@#");
  }
  @Test(expected = ValidationException.class)
  public void testInvalidAdminPasswordForSpecialCharacters(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_DOMAINOWNER,"dsfsffsfD@#Å");
  }

  @Test
  public void testValidAdminPassword(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_DOMAINOWNER,"dsfsffs24fD@[[]{} !`'()$%##");
  }
  @Test
  public void testValidSuperuserPassword(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_SUPERUSER,"dsfsffs24fD@[[]{} !`'()$%##");
  }

  @Test(expected = ValidationException.class)
  public void testInvalidAdminPasswordForUpperCase(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_DOMAINOWNER,"dsfsffsf33@#");
  }

  @Test(expected = ValidationException.class)
  public void testManagerPwdForNumbers(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_SERVICEMANAGER,"dsdsds");
  }
  @Test(expected = ValidationException.class)
  public void testManagerPwdForCharacters(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_SERVICEMANAGER,"24435");
  }

  @Test(expected = ValidationException.class)
  public void testManagerPwdForLength(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_SERVICEMANAGER,"hj");
  }
  @Test()
  public void testValidPwdForManager(){
    PasswordValidator.validate("abc", SecurityConstants.ROLE_SERVICEMANAGER,"hj2fsfsddff");
  }

}
