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

package com.logistimo.returns.actions;

import com.logistimo.returns.service.ReturnsRepository;
import com.logistimo.returns.utility.ReturnsGsonMapper;
import com.logistimo.returns.utility.ReturnsTestConstant;
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.vo.ReturnsTrackingDetailsVO;
import com.logistimo.returns.vo.ReturnsVO;

import org.jose4j.http.Get;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.testng.Assert;

import java.io.IOException;

import static com.logistimo.returns.utility.ReturnsTestUtility.getReturnsVO;
import static com.logistimo.returns.utility.ReturnsTestUtility.getTrackingDetails;
import static com.logistimo.returns.utility.ReturnsTestUtility.getUpdatedReturnsVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Created by pratheeka on 03/07/18.
 */
@RunWith(PowerMockRunner.class)
public class UpdateReturnActionTest {

  @Mock
  private ReturnsRepository returnsRepository;

  @Mock
  private UpdateReturnsTrackingDetailAction updateReturnsTrackingDetailAction;


  @InjectMocks
  private UpdateReturnAction updateReturnAction;

  @Mock
  private GetReturnsAction getReturnsAction;

  @Test
  public void testUpdateReturns() throws IOException {
    ReturnsVO oldReturnVO = getReturnsVO();
    oldReturnVO.setReturnsTrackingDetailsVO(
        getTrackingDetails(ReturnsTestConstant.RETURNS_TRACKING_DETAILS));
    ReturnsVO newReturnVO = getUpdatedReturnsVO();
    ReturnsTrackingDetailsVO updatedTrackingVO =
        getTrackingDetails(ReturnsTestConstant.UPDATED_RETURNS_TRACKING_DETAILS);
    newReturnVO.setReturnsTrackingDetailsVO(updatedTrackingVO);
    doNothing().when(returnsRepository).saveReturns(any());
    when(getReturnsAction.invoke(any())).thenReturn(oldReturnVO);
    when(returnsRepository.saveReturnsTrackingDetails(any())).thenReturn(updatedTrackingVO);
    ReturnsVO updatedReturnsVO = updateReturnAction.invoke(newReturnVO);
    Assert.assertEquals(updatedReturnsVO.getReturnsTrackingDetailsVO().getId(),
        oldReturnVO.getReturnsTrackingDetailsVO().getId());
    Assert.assertEquals(updatedReturnsVO.getId(), oldReturnVO.getId());
    Assert.assertEquals(updatedReturnsVO.getItems().size(), 4);
  }

}
