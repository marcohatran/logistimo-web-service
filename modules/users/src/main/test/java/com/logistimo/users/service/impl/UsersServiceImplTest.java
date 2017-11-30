package com.logistimo.users.service.impl;

import com.logistimo.constants.Constants;
import com.logistimo.tags.entity.ITag;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.entity.UserAccount;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

/**
 * Created by smriti on 16/11/17.
 */
public class UsersServiceImplTest extends TestCase {
  UsersServiceImpl usersService = spy(UsersServiceImpl.class);
  PersistenceManager pm = mock(PersistenceManager.class);
  Query query = mock(Query.class);
  List<String> parameters = new ArrayList<>();
  List<IUserAccount> userAccounts = new ArrayList<>();

  @Before
  public void setUp() {
    doReturn(pm).when(usersService).getPM();
  }

  @Test
  public void testGetUsersByTag() throws Exception {
    String userTags = "'SIO',DIO'";
    StringBuilder queryString = new StringBuilder("SELECT * FROM USERACCOUNT WHERE USERID IN (SELECT USERID FROM USERTOKIOSK WHERE KIOSKID = ?");
    queryString.append(" AND USERID IN (SELECT USERID FROM USER_TAGS WHERE ID IN (SELECT ID FROM TAG WHERE TYPE = ?")
        .append(" AND NAME IN(").append(userTags).append("))))");
    parameters.add(String.valueOf(1l));
    parameters.add(String.valueOf(ITag.USER_TAG));
    userAccounts.add(setUserAccount("domain_owner", 1343724l));
    userAccounts.add(setUserAccount("kiosk_owner", 1343726l));
    when(pm.newQuery(Constants.JAVAX_JDO_QUERY_SQL, queryString.toString())).thenReturn(query);
    when(query.executeWithArray(parameters.toArray())).thenReturn(userAccounts);
    when(pm.detachCopyAll(userAccounts)).thenReturn(userAccounts);
    List<IUserAccount> results = usersService.getUsersByTag(1l,"kiosk", userTags);
    assertEquals(results.size(), 2);
  }

  private IUserAccount setUserAccount(String userId, Long domainId) {
    UserAccount userAccount = new UserAccount();
    userAccount.setUserId(userId);
    userAccount.setDomainId(domainId);
    return userAccount;
  }
}