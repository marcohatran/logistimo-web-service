package com.logistimo.api.filters;

import com.logistimo.auth.SecurityMgr;
import com.logistimo.constants.Constants;
import com.logistimo.security.SecureUserDetails;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by smriti on 24/11/17.
 */
public class MediaSecurityFilterTest {

  String servletPath = "/_ah";
  String path = "/api/mediaendpoint";

  @Test
  public void testDoFilter() throws Exception {
    MediaSecurityFilter securityFilter = new MediaSecurityFilter();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletPath()).thenReturn(servletPath);
    when(request.getPathInfo()).thenReturn(path);
    HttpServletResponse response = mock(HttpServletResponse.class);
    HttpSession session = mock(HttpSession.class);
    SecureUserDetails secureUserDetails = new SecureUserDetails();
    secureUserDetails.setUsername("charan");
    when(session.getAttribute(Constants.PARAM_USER)).thenReturn(secureUserDetails);
    when(request.getSession()).thenReturn(session);
    FilterChain filterChain = mock(FilterChain.class);
    Mockito.doThrow(new RuntimeException()).when(filterChain).doFilter(request, response);
    try {
      securityFilter.doFilter(request, response, filterChain);
      fail("Filter chain not invoked");
    } catch (RuntimeException re) {
      //Runtime exception should be thrown
    }
  }

  @Test
  public void testDoFilterForXAccessUser() throws Exception {
    MediaSecurityFilter securityFilter = new MediaSecurityFilter();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getServletPath()).thenReturn(servletPath);
    when(request.getPathInfo()).thenReturn(path);
    when(request.getHeader("x-access-user")).thenReturn("smriti");
    SecureUserDetails secureUserDetails = new SecureUserDetails();
    secureUserDetails.setUsername("smriti");
    HttpSession session = mock(HttpSession.class);
    when(request.getSession()).thenReturn(session);
    when(SecurityMgr.getSessionDetails(request.getSession())).thenReturn(secureUserDetails);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    Mockito.doThrow(new RuntimeException()).when(filterChain).doFilter(request, response);
    try {
      securityFilter.doFilter(request, response, filterChain);
      fail("Filter chain not invoked");
    } catch (RuntimeException re) {
      //Runtime exception should be thrown
    }
  }
}