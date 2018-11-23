package com.logistimo.communications.service;

import com.logistimo.communications.MessageHandlingException;
import com.logistimo.entity.IMessageLog;
import com.logistimo.utils.MessageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;

/** @author Mohan Raja */
@Service
public class MessageLogService {
  public void updateStatus(String jobId, String mobileNo, String status, Date doneDate)
      throws MessageHandlingException {
    IMessageLog mlog = MessageUtil.getLog(jobId, mobileNo);
    mlog.setStatus(status);
    mlog.setDoneTime(doneDate);
    MessageUtil.log(mlog);
  }
}
