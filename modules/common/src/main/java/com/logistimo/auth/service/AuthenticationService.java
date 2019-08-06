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

package com.logistimo.auth.service;

import com.logistimo.communications.MessageHandlingException;
import com.logistimo.exception.UnauthorizedException;
import com.logistimo.models.AuthRequest;
import com.logistimo.models.users.PersonalAccessToken;
import com.logistimo.models.users.UserDetails;
import com.logistimo.services.ServiceException;
import com.logistimo.users.entity.IUserToken;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


/**
 * Created by naveensnair on 04/11/15.
 */
public interface AuthenticationService {

  IUserToken generateUserToken(String userId, Integer accessInitiator)
      throws ServiceException;

  IUserToken authenticateToken(String token, Integer accessInitiator) throws UnauthorizedException;

  String getUserIdByToken(String token);

  /**
   * Clear tokens associated with the userId, and if includeOtherTokens is passed Bulletin board and Personal access tokens will also be removed.
   * @param userId - user id.
   * @param includeOtherTokens - remove bulletin board and personal access tokens.
   * @return - true/false
   */
  Boolean clearUserTokens(String userId, boolean includeOtherTokens);

  /**
   * Update users session with new session Id. This also clears user tokens generated for mobile.
   * sessionId can be null, Send null when being logged in via mobile.
   */
  void updateUserSession(String userId, String sessionId);

  String getUserToken(String userId);

  String resetPassword(String userId, int mode, String otp, String src,
                              String au)
      throws ServiceException, MessageHandlingException, IOException;

  /**
   * Generate OTP for Email
   */
  String generateOTP(String userId, int mode, String src, String hostUri)
      throws MessageHandlingException, IOException, ServiceException;

  Optional<IUserToken> checkAccessKeyStatus(String accessKey) throws ServiceException;

  void authoriseAccessKey(String accessKey) throws ServiceException;

  String createJWT(String userid, long ttlMillis);

  String[] decryptJWT(String token);

  String setNewPassword(String token, String newPassword, String confirmPassword, boolean isEnhanced)
      throws ServiceException;

  String generatePassword(String id);

  void validateOtpMMode(String userId, String otp);

  void validateOtpMMode(String userId, String otp, boolean isTwoFactorAuthenticationOTP);

  String generateAccessKey();

  UserDetails authenticate(AuthRequest authRequest) throws ServiceException;

  /**
   *
   * @param userId
   * @param otp
   * @param oldPassword
   * @param newPassword
   * @param isEnhanced -this flag indicates that client is sending sha512(salt+password) and compatibale with security chnages
   * @throws ServiceException
   */
  void changePassword(String userId, String otp, String oldPassword, String newPassword, boolean isEnhanced) throws ServiceException;

  void enableAccount(String userId) throws ServiceException;

  void disableAccount(String userId) throws ServiceException;

  void removeUserFromCache(String userId);

  String getUserSalt(String userId);

  String generateRandomSalt();

  void logForbiddenAccess(AuthRequest authRequest);


  boolean verifyCaptcha(String captchaResponse);

  /**
   * Generate 2FA OTP for mobile
   *
   * @return success message
   */
  String generate2FAOTP(String userId) throws MessageHandlingException, IOException;

  boolean isUserDeviceAuthenticated(String deviceKey, String userId, Long domainId)
      throws IOException;

  boolean authenticateUserByCredentials(String userId, String deviceKey, Integer loginSource,
                                        String otp, boolean isTwoFactorAuthenticationOTP)
      throws IOException, MessageHandlingException;

  /**
   * Register a new personal access token
   * @param tokenDescription - Some description about this token.
   * @return
   */
  PersonalAccessToken generatePersonalAccessToken(String tokenDescription) throws ServiceException;

  /**
   *
   * @param username - User id
   * @param domainId - domain id
   * @return Personal access tokens registered by this user in the given domain.
   */
  List<PersonalAccessToken> getPersonalAccessTokens(String username, Long domainId);

  /**
   * Remove a token, including personal access tokens.
   * @param token - encrypted token.
   */
  void deleteToken(String token);
}
