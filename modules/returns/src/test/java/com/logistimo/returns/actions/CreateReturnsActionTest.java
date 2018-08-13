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
import com.logistimo.returns.utility.ReturnsTestUtility;
import com.logistimo.returns.vo.ReturnsItemBatchVO;
import com.logistimo.returns.vo.ReturnsItemVO;
import com.logistimo.returns.vo.ReturnsVO;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.logistimo.returns.utility.ReturnsTestUtility.getTrackingDetailsVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by pratheeka on 03/07/18.
 */
@RunWith(PowerMockRunner.class)
public class CreateReturnsActionTest {

  @Mock
  private ReturnsRepository returnsRepository;

  @InjectMocks
  private CreateReturnsAction createReturnsAction;

  @Before
  public void init() throws IOException {
    when(returnsRepository.saveReturnsTrackingDetails(any())).thenReturn(null);
    doNothing().when(returnsRepository).saveReturns(any());
  }

  @Test
  public void testCreateReturns() throws IOException {
    ReturnsVO returnsVO=ReturnsTestUtility.getReturnsVO();
    returnsVO.setItems(getReturnItemVOList());
    returnsVO.setReturnsTrackingDetailsVO(getTrackingDetailsVO());
    createReturnsAction.invoke(returnsVO);
    verify(returnsRepository,times(1)).saveReturnsTrackingDetails(any());
    verify(returnsRepository,times(1)).saveReturns(any());
  }

  private List<ReturnsItemVO> getReturnItemVOList(){
   return ReturnsTestUtility.getReturnsItemVOList();
  }


}
