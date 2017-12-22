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

package com.logistimo.pagination.processor;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.logistimo.pagination.Results;
import com.logistimo.utils.MetricsUtil;

import javax.jdo.PersistenceManager;

/**
 * Created by kumargaurav on 22/12/17.
 */
public abstract class InstrumentedProcessor implements Processor {

  private final Timer timer = MetricsUtil.getTimer(this.getClass(),"timer");
  private final Meter meter = MetricsUtil.getMeter(this.getClass(),"meter");

  @Override
  public String process(Long domainId, Results results, String prevOutput, PersistenceManager pm)
      throws ProcessingException {
    meter.mark();
    Timer.Context context = timer.time();
    try {
      return execute(domainId, results, prevOutput, pm);
    } finally {
      context.stop();
    }
  }

  public abstract String execute(Long domainId, Results results, String prevOutput, PersistenceManager pm)
      throws ProcessingException;
}
