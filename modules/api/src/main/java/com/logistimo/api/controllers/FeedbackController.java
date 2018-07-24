package com.logistimo.api.controllers;

import com.logistimo.api.action.SubmitFeedbackAction;
import com.logistimo.api.models.FeedbackModel;
import com.logistimo.config.models.ConfigurationException;
import com.logistimo.logger.XLog;
import com.logistimo.services.ServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by yuvaraj on 02/07/18.
 */
@Controller
@RequestMapping("/feedback")
public class FeedbackController {

  private static final XLog xLogger = XLog.getLog(FeedbackController.class);

  private SubmitFeedbackAction submitFeedbackAction;

  @Autowired
  public void setSubmitFeedbackAction(SubmitFeedbackAction submitFeedbackAction) {
    this.submitFeedbackAction = submitFeedbackAction;
  }

  @RequestMapping(value = "", method = RequestMethod.POST)
  public
  @ResponseBody
  void create(@RequestBody FeedbackModel model, HttpServletRequest request) throws ServiceException,
      ConfigurationException {
    String app = request.getHeader("X-app-name");
    String appVer = request.getHeader("X-app-ver");
    model.setApp(app);
    model.setAppVersion(appVer);
    xLogger.fine("Feedback request with data {0}",model);
    submitFeedbackAction.invoke(model);
  }

}
