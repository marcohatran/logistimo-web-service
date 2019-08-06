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

/**
 *
 */
package com.logistimo.pagination;

import com.logistimo.services.Resources;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpSession;

/**
 * A utility to help in page navigation
 *
 * @author Arun
 */
public class Navigator {

  private final HttpSession session;
  private final String attribute;
  private int offset = 1;
  private int size = PageParams.DEFAULT_SIZE;
  private Map<Integer, String> cursorMap = null;
  private String urlbase = null;
  private int numResults = 0;
  private int total = 0;

  @SuppressWarnings("unchecked")
  public Navigator(HttpSession session, String attribute, int offset, int size, String urlbase,
                   int total) {
    this.offset = offset;
    this.size = size;
    this.urlbase = urlbase;
    this.total = total;
    this.session = session;
    this.attribute = attribute;
    cursorMap = (Map<Integer, String>) session.getAttribute(attribute);
    if (cursorMap == null) {
      cursorMap = new HashMap<Integer, String>();
      session.setAttribute(attribute, cursorMap);
    } else if ((offset == 1 || offset == 0) && !cursorMap.isEmpty()) {
      // Reset cursorMap
      cursorMap.clear();
      session.setAttribute(attribute, cursorMap);
    }
    // Ensure URL base is correct
    if (this.urlbase != null) {
      int i = this.urlbase.indexOf("?");
      if (i == -1) // no ? symbol
      {
        this.urlbase += "?";
      } else if (i < (this.urlbase.length()
          - 1)) // there is already something after the ?, typically a query string
      {
        this.urlbase += "&";
      }
    }
  }

  public String getCursor(int offset) {
    return cursorMap.get(new Integer(offset));
  }

  ;

  public void putCursor(int offset, String cursor) {
    cursorMap.put(new Integer(offset), cursor);
    session.setAttribute(attribute, cursorMap);
  }

  public int getNextOffset() {
    return offset + size;
  }

  public int getPrevOffset() {
    return offset - size;
  }

  public int getMaxRange() {
    if (numResults < size) {
      return offset + numResults - 1;
    } else {
      return getNextOffset() - 1;
    }
  }

  public void setResultParams(Results results) {
    numResults = results.getSize();
    if (numResults >= size) {
      putCursor(getNextOffset(), results.getCursor());
    }
  }

  public String getFirstUrl() {
    if (getPrevOffset() > 0) {
      return urlbase + "s=" + size;
    }
    return null;
  }

  public String getNextUrl() {
    if (numResults >= size) {
      return urlbase + "s=" + size + "&o=" + getNextOffset();
    }
    return null;
  }

  public String getPrevUrl() {
    int prevOffset = getPrevOffset();
    if (prevOffset > 0) {
      return urlbase + "s=" + size + "&o=" + prevOffset;
    }
    return null;
  }

  public String getNavigationHTML(Locale locale) {
    ResourceBundle jsMessages = Resources.getBundle(locale);
    String html = "";
    if (getFirstUrl() != null) {
      html +=
          "<a href=\"" + getFirstUrl() + "\">&lt;&lt;" + jsMessages.getString("first")
              + "</a>&nbsp;";
    }
    if (getPrevUrl() != null) {
      html += "<a href=\"" + getPrevUrl() + "\">&lt;" + jsMessages.getString("prev") + "</a>&nbsp;";
    }
    if (numResults > 0) {
      html += "&nbsp;<b>" + offset + "-" + getMaxRange() + "</b>&nbsp;";
      if (total > 0) {
        html += jsMessages.getString("of") + " <b>" + total + "</b>&nbsp;";
      }
      if (getNextUrl() != null) {
        html +=
            "&nbsp;<a href=\"" + getNextUrl() + "\">" + jsMessages.getString("next") + "&gt;</a>";
      }
    }
    return html;
  }
}
