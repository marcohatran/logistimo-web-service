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

package com.logistimo.utils;

import com.logistimo.api.builders.SMSBuilder;
import com.logistimo.api.util.SMSDecodeUtil;
import com.logistimo.services.ServiceException;

import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

/**
 * Created by chandrakant on 03/10/17.
 */
public class SMSDecodeTest {

  @Test
  public void encodingTest() throws UnsupportedEncodingException {
    for (long l = 0; l < 10000000; l++) {
      String encodedStr = SMSDecodeUtil.encode(l);
      long decodedLong = SMSDecodeUtil.decode(encodedStr);
      assert decodedLong == l;
    }
  }

  @Test
  public void smsBuilderTest() throws ServiceException {
    String message = "O=d951:S=QXpNWBO:V=2:P=1:U=phc1dvs1_operator:K=5fLf:I=K;r0q_i,39,k,9,"
        + "\"B1_MA2_BE\",5fLg;iou_w,2P,9,8,\"B1_MA2_BE\",*L;a6A_i,2Q,K,7,,5fLg;RSM_p,"
        + "26,HQ,6,,";
    SMSBuilder smsBuilder = new SMSBuilder();
    smsBuilder.buildSMSModel(message);
  }

  /*@Test
  public void smsControllerTest() throws ServiceException, UnsupportedEncodingException {
    SMSController smsController = new SMSController();
    MockHttpServletRequest request = new MockHttpServletRequest();
    String message = "O%3D74f4%3AS%3DQXvGmDv%3AV%3D2%3AP%3D0%3AU%3Dckop230%3AK%3D5f9z%3AI%3DG"
                     + "%3B3fo_i%2CC6%2C13%2C%2C%22NEW+BATCH+11%22%2C%3B3fg_i%2CPA%2Cj%2C%2C%22NEW"
                     + "+BATCH+12%22%2C%3BMO_w%2CB3%2Cj%2C%2C%22NEW+BATCH+11%22%2C";
    request.setParameter(MessageHandler.MESSAGE, message);
    smsController.updateTransactions(request);
  }*/
}
