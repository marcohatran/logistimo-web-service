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

package com.logistimo.communications.service;

import com.logistimo.logger.XLog;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.NullLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by kumargaurav on 24/07/18.
 */
public class TemplateEmailTask implements Callable<String> {

  private static final XLog xLogger = XLog.getLog(TemplateEmailTask.class);

  private static final String UTF = "UTF-8";

  private final String template;

  private final Map<String, Object> attributes;

  private final String subject;

  private final String address;

  private final Long domainId;

  public TemplateEmailTask(String template, Map<String, Object> attributes,
                           String subject, String address, Long domainId) {
    this.template = template;
    this.attributes = attributes;
    this.subject = subject;
    this.address = address;
    this.domainId = domainId;
  }

  @Override
  public String call() {

    VelocityContext context = buildVelocityContext(this.attributes);
    VelocityEngine engine = initVelocityEngine();
    String message = buildContent(context,engine);
    sendEmail(domainId ,address,subject,message);
    return "success";
  }

  protected String buildContent(VelocityContext context, VelocityEngine engine ) {
    StringWriter out = new StringWriter();
    Template velocityTemplate = engine.getTemplate(this.template, UTF);
    velocityTemplate.merge(context, out);
    return out.toString();
  }

  protected VelocityEngine initVelocityEngine () {
    VelocityEngine ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
    ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, NullLogChute.class.getName());
    ve.setProperty("runtime.log.logsystem.log4j.logger", xLogger);
    ve.setProperty("input.encoding", UTF);
    ve.setProperty("output.encoding", UTF);
    ve.init();
    return ve;
  }

  protected VelocityContext buildVelocityContext (Map<String,Object> attributes) {
    VelocityContext vc = new VelocityContext();
    for(Map.Entry<String,Object> entry:attributes.entrySet()) {
      Object val = entry.getValue();
      if(val instanceof String){
        vc.put(entry.getKey(),val);
      } else if (val instanceof Number){
        vc.put(entry.getKey(),String.valueOf(val));
      } else {
        xLogger.warn("Unsupported attribute entry with key {0}",entry.getKey());
      }
    }
    return vc;
  }

  protected void sendEmail(Long domainId, String emailAddresses, String subject, String message) {
    try {
      EmailService svc = EmailService.getInstance();
      svc.sendHTML(domainId, Collections.singletonList(emailAddresses), subject, message, null);
    } catch (Exception e) {
      xLogger.warn("Issue with sending feedback email {0} ",e.getMessage(), e);
    }
  }

  @Override
  public String toString() {
    return "TemplateEmailTask{" +
        "subject='" + subject + '\'' +
        ", address='" + address + '\'' +
        ", domainId=" + domainId +
        '}';
  }
}
