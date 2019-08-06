/*
 * Copyright © 2019 Logistimo.
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

package com.logistimo.auth.service.impl;

import com.logistimo.auth.Authorize;
import com.logistimo.auth.Role;
import com.logistimo.auth.SecurityUtil;
import com.logistimo.auth.utils.SecurityUtils;
import com.logistimo.exception.ForbiddenAccessException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuthorizeImpl {

  @Around("execution(* * (..)) && @annotation(com.logistimo.auth.Authorize)")
  public Object wrap(final ProceedingJoinPoint point) throws Throwable {
    final Method method = ((MethodSignature) point.getSignature()).getMethod();
    Role requiredRole = method.getAnnotation(Authorize.class).role();
    if (SecurityUtil
        .compareRoles(SecurityUtils.getUserDetails().getRole(), requiredRole.getValue())
        < 0) {
      throw new ForbiddenAccessException("Forbidden","G003");
    }
    return point.proceed();
  }

}
