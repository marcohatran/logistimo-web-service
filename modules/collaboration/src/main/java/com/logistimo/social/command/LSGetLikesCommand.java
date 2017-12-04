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

package com.logistimo.social.command;

import com.logistimo.collaboration.core.models.GetLikeResponseModel;
import com.logistimo.collaboration.core.models.LikeModel;
import com.logistimo.common.builder.MediaBuilder;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.dao.JDOUtils;
import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.media.endpoints.IMediaEndPoint;
import com.logistimo.media.entity.IMedia;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.social.model.ContentQuerySpecs;
import com.logistimo.social.model.LSGetLikeModel;
import com.logistimo.social.model.LSLikeResponseModel;
import com.logistimo.social.model.LSSocialLikeModel;
import com.logistimo.social.model.LSSocialLikerModel;
import com.logistimo.social.provider.ContentProvider;
import com.logistimo.users.entity.IUserAccount;
import com.logistimo.users.service.impl.UsersServiceImpl;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

/**
 * Created by kumargaurav on 21/11/17.
 */
public class LSGetLikesCommand extends HystrixCommand<LSLikeResponseModel> {

  private final LSGetLikeModel request;
  private final RestTemplate restTemplate;

  public LSGetLikesCommand(RestTemplate restTemplate, LSGetLikeModel request) {
    super(HystrixCommandGroupKey.Factory.asKey("LSCollaborationClient"),
        5000);
    this.restTemplate = restTemplate;
    this.request = request;
  }

  @Override
  protected LSLikeResponseModel run() throws Exception {

    URI link = null;
    UriBuilder builder = JerseyUriBuilder.fromUri(getLikeUrl());

    builder.queryParam("object_id", request.getObjectId());
    builder.queryParam("object_type", request.getObjectType());
    if (!StringUtils.isEmpty(request.getContextId())) {
      builder.queryParam("context_id", request.getContextId());
    }
    builder.queryParam("offset", request.getOffset());
    builder.queryParam("size", request.getSize());
    link = builder.build();
    try {
      ResponseEntity<GetLikeResponseModel>
          entity =
          restTemplate.getForEntity(link, GetLikeResponseModel.class);
      return map(entity.getBody());
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String getLikeUrl() {
    String
        baseUrl =
        ConfigUtil.get("collaboration-service.url", "http://localhost:9070/v1/collaboration/likes");
    return baseUrl;
  }

  private LSLikeResponseModel map(GetLikeResponseModel model) {
    LSLikeResponseModel res = convert(model);
    return res;
  }

  public LSLikeResponseModel convert(GetLikeResponseModel model) {
    LSLikeResponseModel res = new LSLikeResponseModel();
    res.setTotal(model.getTotal());
    res.setOffset(model.getOffset());
    res.setSize(model.getSize());
    if (model.getLikes() != null && model.getLikes().size() > 0) {
      List<LSSocialLikeModel> likes = new ArrayList<>();
      LSSocialLikeModel likeModel = null;
      for (LikeModel lm : model.getLikes()) {
        likeModel = new LSSocialLikeModel();
        likeModel.setObjectId(lm.getObjectId());
        likeModel.setObjectType(lm.getObjectType());
        likeModel.setContextId(lm.getContextId());
        likeModel.setContextType(lm.getContextType());
        likeModel.setContextAttribute(lm.getContextAttributes());
        likeModel.setUser(lm.getLiker());
        likeModel.setLiker(new LSSocialLikerModel());
        likeModel.setTimestamp(lm.getCreatedOn());
        likes.add(likeModel);
      }
      addLikerAttributes(likes);
      addTextSummaries(likes);
      res.setLikes(likes);
    }
    return res;
  }

  private void addLikerAttributes(List<LSSocialLikeModel> model) {

    UsersServiceImpl
        usersService =
        StaticApplicationContext.getApplicationContext().getBean(UsersServiceImpl.class);
    List<String> userIds = model.stream().map(m -> m.getUser()).collect(Collectors.toList());
    List<IUserAccount> users = usersService.getUsersByIds(userIds);

    model.stream().flatMap(m -> users.stream()
        .filter(user -> user.getUserId().equals(m.getUser()))
        .map(u -> addProperty(m, u))).count();
  }


  private void addTextSummaries(List<LSSocialLikeModel> model) {
    model.stream().map(m -> addTextSummary(m)).count();
  }

  private LSSocialLikeModel addTextSummary(LSSocialLikeModel model) {
    ContentProvider
        contentProvider =
        StaticApplicationContext.getApplicationContext().getBean(ContentProvider.class);
    ContentQuerySpecs querySpecs = new ContentQuerySpecs();
    querySpecs.setObjectId(model.getObjectId());
    querySpecs.setObjectType(model.getObjectType());
    querySpecs.setContextId(model.getContextId());
    querySpecs.setContextType(model.getContextType());
    querySpecs.setContextAttribute(model.getContextAttribute());
    querySpecs.setUser(model.getUser());
    model.setTextSummary(contentProvider.generateContent(querySpecs));
    return model;
  }

  private LSSocialLikeModel addProperty(LSSocialLikeModel m, IUserAccount u) {
    m.getLiker().setLiker(u.getUserId());
    m.getLiker().setName(u.getFullName());
    m.getLiker().setCreatedOn(m.getTimestamp());
    if (u.getTags() != null && u.getTags().size() > 0) {
      m.getLiker().setUserTags(StringUtils.arrayToCommaDelimitedString(u.getTags().toArray()));
    }
    IMediaEndPoint endPoint = JDOUtils.createInstance(IMediaEndPoint.class);
    MediaBuilder builder = new MediaBuilder();
    List<IMedia> mediaList = endPoint.getMedias(u.getUserId());
    m.getLiker().setUserMedia(builder.constructMediaModelList(mediaList));
    return m;
  }
}
