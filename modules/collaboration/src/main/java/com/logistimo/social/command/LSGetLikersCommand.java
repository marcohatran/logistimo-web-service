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

import com.logistimo.common.builder.MediaBuilder;
import com.logistimo.context.StaticApplicationContext;
import com.logistimo.dao.JDOUtils;
import com.logistimo.exception.ErrorResponse;
import com.logistimo.exception.HttpBadRequestException;
import com.logistimo.media.endpoints.IMediaEndPoint;
import com.logistimo.media.entity.IMedia;
import com.logistimo.services.utils.ConfigUtil;
import com.logistimo.collaboration.core.models.LikerResponseModel;
import com.logistimo.collaboration.core.models.SocialLikerModel;
import com.logistimo.social.model.LSGetLikeModel;
import com.logistimo.social.model.LSLikerResponseModel;
import com.logistimo.social.model.LSSocialLikerModel;
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
public class LSGetLikersCommand extends HystrixCommand<LSLikerResponseModel> {

  private final LSGetLikeModel request;
  private final RestTemplate restTemplate;

  public LSGetLikersCommand(RestTemplate restTemplate, LSGetLikeModel request) {
    super(HystrixCommandGroupKey.Factory.asKey("LSCollaborationClient"),
        5000);
    this.restTemplate = restTemplate;
    this.request = request;
  }

  @Override
  protected LSLikerResponseModel run() throws Exception {

    URI link = null;
    String contextId  = request.getContextId();
    UriBuilder builder =  JerseyUriBuilder.fromUri(getLikerUrl(contextId));
    builder.queryParam("offset",request.getOffset());
    builder.queryParam("size",request.getSize());
    if(StringUtils.isEmpty(contextId)) {
      link = builder.build(request.getObjectType(), request.getObjectId());
    } else {
      link = builder.build(request.getObjectType(), request.getObjectId(),contextId);
    }
    try {
      ResponseEntity<LikerResponseModel>
          entity =
          restTemplate.getForEntity(link, LikerResponseModel.class);
      return map(entity.getBody());
    } catch (HttpClientErrorException exception) {
      throw new HystrixBadRequestException(exception.getMessage(),
          new HttpBadRequestException(ErrorResponse.getErrorResponse(exception), exception));
    }
  }

  private String getLikerUrl (String contextId) {
    String baseUrl = ConfigUtil.get("collaboration-service.url",
        "http://localhost:9070/v1/collaboration/likes");
    if(StringUtils.isEmpty(contextId)){
      return baseUrl+"/{obj_ty}/{obj_id}/likers";
    }
    return baseUrl+"/{obj_ty}/{obj_id}/{context_id}/likers";
  }

  private LSLikerResponseModel map (LikerResponseModel model) {
    LSLikerResponseModel res = convert(model);
    transform(res.getLikers());
    return res;
  }

  public LSLikerResponseModel convert(LikerResponseModel model) {
    LSLikerResponseModel res = new LSLikerResponseModel();
    res.setTotal(model.getTotal());
    res.setOffset(model.getOffset());
    res.setSize(model.getSize());
    if (model.getLikers() != null && model.getLikers().size() >0) {
      List<LSSocialLikerModel> likers = new ArrayList<>();
      LSSocialLikerModel likerModel = null;
      for (SocialLikerModel socialLikerModel : model.getLikers()) {
        likerModel = new LSSocialLikerModel();
        likerModel.setLiker(socialLikerModel.getLiker());
        likerModel.setCreatedOn(socialLikerModel.getCreatedOn());
        likerModel.setSrc(socialLikerModel.getSrc());
        likers.add(likerModel);
      }
      res.setLikers(likers);
    }
    return res;
  }

  private void transform (List<LSSocialLikerModel> model) {

    UsersServiceImpl usersService = StaticApplicationContext.getApplicationContext().getBean(UsersServiceImpl.class);
    List<String> userIds = model.stream().map(m-> m.getLiker()).collect(Collectors.toList());
    List<IUserAccount> users = usersService.getUsersByIds(userIds);

    model.stream().flatMap(m ->users.stream()
                  .filter(user ->  user.getUserId().equals(m.getLiker()))
                  .map(u -> addProperty(m,u))).count();
  }

  private LSSocialLikerModel addProperty (LSSocialLikerModel m, IUserAccount u) {
    m.setName(u.getFullName());
    if (u.getTags() != null && u.getTags().size() >0) {
      m.setUserTags(StringUtils.arrayToCommaDelimitedString(u.getTags().toArray()));
    }
    IMediaEndPoint endPoint = JDOUtils.createInstance(IMediaEndPoint.class);
    MediaBuilder builder = new MediaBuilder();
    List<IMedia> mediaList = endPoint.getMedias(u.getUserId());
    m.setUserMedia(builder.constructMediaModelList(mediaList));
    return m;
  }

}
