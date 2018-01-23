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
package com.logistimo.tags;

import com.logistimo.constants.CharacterConstants;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.tags.dao.ITagDao;
import com.logistimo.tags.entity.ITag;
import com.logistimo.utils.StringUtil;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Arun
 */
public class TagUtil {

  // Tags
  public static final String ALL = "All";
  // Types
  public static final String TYPE_MATERIAL = "mt";
  public static final String TYPE_ENTITY = "en";
  public static final String TYPE_ORDER = "or";
  public static final String TYPE_USER = "us";
  // URLs
  private static final String URL_CREATECONFIG = "/task/createconfig";

  public static List<String> getList(List<? extends ITag> tgs) {
    List<String> tags = null;
    if (tgs != null) {
      tags = new ArrayList<>(tgs.size());
      for (ITag tag : tgs) {
        if (tag.getName() != null) {
          tags.add(tag.getName());
        }
      }
    }
    return tags;
  }

  // Get a set of tags with white-space removed around individual tag items; also de-duplicates the tags and sorts them
  public static String getCleanTags(String commaSepTags, boolean sort) {
    if (commaSepTags == null || commaSepTags.isEmpty()) {
      return commaSepTags;
    }
    String[] tagsA = commaSepTags.split(",");
    if (tagsA.length == 0) {
      return commaSepTags.trim();
    }
    if (sort) {
      Arrays.sort(tagsA, new TagUtil.TagComparator());
    }
    String tTags = "";
    ArrayList<String> dedupL = new ArrayList<>();
    for (int i = 0; i < tagsA.length; i++) {
      if (!dedupL.contains(tagsA[i].toLowerCase())) {
        dedupL.add(tagsA[i].toLowerCase());
        if (i != 0) {
          tTags += ",";
        }
        tTags += tagsA[i].trim();
      }
    }

    return tTags;
  }

  public static String[] getTagsArray(String commaSepTags) {
    String[] arr = null;
    if (commaSepTags != null && !commaSepTags.isEmpty()) {
      arr = commaSepTags.split(",");
      if (arr.length == 0) {
        arr = new String[1];
        arr[0] = commaSepTags;
      }
    }
    return arr;
  }


  /**
   * +     * @param tags string csv of tags
   * +     * @param type tag type : eg: ktag, mtag,utag
   * +
   */
  public static String getTagsByNames(String tags, int type) {
    String tagIds = null;
    if (StringUtils.isBlank(tags)) {
      return null;
    }
    List<String> tagNameList = StringUtil.getList(tags);
    ITagDao tagDao = StaticApplicationContext.getBean(ITagDao.class);
    List<ITag> iTags = tagDao.getTagsByNames(tagNameList, type);
    if (iTags != null) {
      tagIds = getTagIdCSV(iTags, CharacterConstants.COMMA);
    }
    return tagIds;
  }

  /**
   * @param list      of ITAGs
   * @param separator delimiter eg " ' " so that tag id's will be constructed to a string with the given delimiter.
   */
  public static String getTagIdCSV(List<? extends ITag> list, String separator) {
    String str = null;
    if (list != null) {
      Iterator<ITag> it = (Iterator<ITag>) list.iterator();
      if (it.hasNext()) {
        str = CharacterConstants.S_QUOTE + it.next().getId() + CharacterConstants.S_QUOTE;
      }
      while (it.hasNext()) {
        str +=
            separator + CharacterConstants.S_QUOTE + it.next().getId() + CharacterConstants.S_QUOTE;
      }
    }
    return str;
  }

  public static String getTagById(Long id, int type) {
    ITagDao tagDao = StaticApplicationContext.getBean(ITagDao.class);
    ITag iTag = tagDao.getTagById(id, type);
    return (iTag != null) ? iTag.getName() : "";
  }

  private static class TagComparator implements Comparator<String> {

    public TagComparator() {

    }

    @Override
    public int compare(String o1, String o2) {
      return o1.toLowerCase().compareTo(o2.toLowerCase());
    }

  }

  public static String getTagCSV(List<? extends ITag> list) {
    return getTagCSV(list, ",");
  }

  public static String getTagCSV(List<? extends ITag> list, String separator) {
    String str = null;
    if (list != null) {
      Iterator<ITag> it = (Iterator<ITag>) list.iterator();
      if (it.hasNext()) {
        str = it.next().getName();
      }
      while (it.hasNext()) {
        str += separator + it.next().getName();
      }
    }
    return str;
  }
}
