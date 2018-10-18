package com.logistimo.api.controllers;

import com.logistimo.api.builders.UserDevicesBuilder;
import com.logistimo.config.entity.Config;
import com.logistimo.config.service.ConfigurationMgmtService;
import com.logistimo.constants.SourceConstants;
import com.logistimo.services.ServiceException;
import com.logistimo.twofactorauthentication.service.TwoFactorAuthenticationService;
import com.logistimo.twofactorauthentication.vo.UserDevicesVO;
import com.logistimo.utils.CommonUtils;
import com.logistimo.utils.TwoFactorAuthenticationUtil;

import junit.framework.TestCase;

import org.junit.Before;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author smriti
 */
public class AuthControllerMV1Test extends TestCase {

  AuthControllerMV1 authController = new AuthControllerMV1();
  HttpServletResponse response;
  UserDevicesBuilder userDevicesBuilder = new UserDevicesBuilder();
  TwoFactorAuthenticationService
      twoFactorAuthenticationService;

  @Before
  public void setUp() throws ServiceException {
    response = new MockHttpServletResponse();

    ConfigurationMgmtService configurationMgmtService = mock(ConfigurationMgmtService.class);
    Config config = new Config();
    config.setKey("domain.132453");
    config.setDomainId(132453l);
    config.setLastUpdated(new Date());

    userDevicesBuilder.setConfigurationMgmtService(configurationMgmtService);
    authController.setUserDevicesBuilder(userDevicesBuilder);

    twoFactorAuthenticationService =
        mock(TwoFactorAuthenticationService.class);
    authController.setTwoFactorAuthenticationService(twoFactorAuthenticationService);

    when(configurationMgmtService.getConfiguration(anyString())).thenReturn(config);
    doNothing().when(twoFactorAuthenticationService).createUserDevices(any(UserDevicesVO.class));
  }

  public void testCreateUserDeviceInformation() throws Exception {
    authController.createUserDeviceInformation(response, SourceConstants.WEB, "user");
    String responseHeader =
        String.valueOf(((MockHttpServletResponse) response).getHeader("Set-Cookie"));
    assertNotNull(responseHeader);
    assertTrue(String.valueOf(true),
        responseHeader.contains(TwoFactorAuthenticationUtil.generateAuthKey("user")));
  }

  public void testUpdateHeaderForUserDevice() throws Exception {
    String cookieKey = "_di" + CommonUtils.getMD5("user");
    String cookieValue = "abc678@qhdkk#462b!nd";

    authController.updateHeaderForUserDevice(response, cookieValue, "user");
    String responseHeader =
        String.valueOf(((MockHttpServletResponse) response).getHeader("Set-Cookie"));
    assertNotNull(responseHeader);
    assertEquals(cookieKey + "=" + cookieValue + ";Path=/; HttpOnly", responseHeader);
  }
}