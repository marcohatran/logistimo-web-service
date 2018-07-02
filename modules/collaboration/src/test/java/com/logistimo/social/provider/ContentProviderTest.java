package com.logistimo.social.provider;

import com.logistimo.config.models.DomainConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Locale;

import static com.logistimo.constants.Constants.LANG_ENGLISH;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by yuvaraj on 28/06/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DomainConfig.class})
public class ContentProviderTest {

  //private DomainConfig domainConfig;

  @Before
  public void setUp() {
    //domainConfig = new DomainConfig();
    mockStatic(DomainConfig.class);
    PowerMockito.when(DomainConfig.getInstance(anyLong())).thenReturn(new DomainConfig());
  }
  @Test
  public void getUserLocale() throws Exception {
    ContentProvider contentProvider = new ContentProvider();
    Locale locale = contentProvider.getUserLocale(1L);
    assertNotNull(locale);
  }



}