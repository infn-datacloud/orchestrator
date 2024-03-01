/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.controller;

import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntity;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.exception.http.ForbiddenException;
import it.reply.orchestrator.resource.DeploymentResource;
import it.reply.orchestrator.resource.DeploymentResourceAssembler;
import it.reply.orchestrator.service.DeploymentService;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import it.reply.orchestrator.utils.JwtUtils;
import it.reply.orchestrator.utils.MdcUtils;
import java.text.ParseException;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DeploymentController {

  private static final String OFFLINE_ACCESS_REQUIRED_CONDITION =
      "#oauth2.throwOnError(#oauth2.hasAnyScope('offline_access', 'fts:submit-transfer'))";

  @Autowired
  private DeploymentService deploymentService;

  @Autowired
  private DeploymentResourceAssembler deploymentResourceAssembler;

  @Autowired
  private OAuth2TokenService oauth2Tokenservice;

  @Autowired
  private OidcProperties oidcProperties;

  /**
   * Check if there is a group claim in user's token and verify that the group requested by the user
   * is in the user's allowed groups.
   *
   * @param userToken user's token
   * @param requestedGroup group requested by the user
   * @throws ParseException if the claim value is not of required type when parsing user's token
   * @throws ForbiddenException if there is no groups or wlcg groups claim, or they are both empty,
   *         or the requested group is not in the user's allowed groups
   */
  public void authorizeRequestedGroup(String userToken, String requestedGroup)
      throws ParseException, ForbiddenException {
    List<String> groups = null;
    List<String> wlcgGroups = null;
    try {
      groups = JwtUtils.getJwtClaimsSet(JwtUtils.parseJwt(userToken)).getStringListClaim("groups");
      wlcgGroups =
          JwtUtils.getJwtClaimsSet(JwtUtils.parseJwt(userToken)).getStringListClaim("wlcg.groups");
    } catch (ParseException e) {
      LOG.error(e.getMessage());
      throw e;
    }
    if ((groups == null || groups.isEmpty()) && (wlcgGroups == null || wlcgGroups.isEmpty())) {
      String errorMessage = "User's token does not contain a group claim or it is empty";
      LOG.error(errorMessage);
      throw new ForbiddenException(errorMessage);
    }
    if ((groups != null && !groups.contains(requestedGroup))
        || (wlcgGroups != null && !wlcgGroups.contains("/" + requestedGroup))) {
      String errorMessage =
          String.format("The group %s is not in the user's authorized groups", requestedGroup);
      LOG.error(errorMessage);
      throw new ForbiddenException(errorMessage);
    }
  }

  /**
   * Get all deployments.
   *
   * @param createdBy
   *          created by name
   * @param userGroup
   *          user group
   * @param pageable
   *          {@link Pageable}
   * @param pagedAssembler
   *          {@link PagedResourcesAssembler}
   * @throws ParseException if the claim value is not of required type when parsing user's token
   * @throws ForbiddenException if there is no groups or wlcg groups claim, or they are both empty,
   *         or the requested group is not in the user's allowed groups
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public PagedResources<DeploymentResource> getDeployments(
      @RequestParam(name = "createdBy", required = false) @Nullable String createdBy,
      @RequestParam(name = "userGroup", required = false) @Nullable String userGroup,
      @PageableDefault(sort = "createdAt", direction = Direction.DESC) Pageable pageable,
      PagedResourcesAssembler<Deployment> pagedAssembler)
      throws ParseException, ForbiddenException {

    if (oidcProperties.isEnabled() && userGroup != null) {
      String userToken = oauth2Tokenservice.getOAuth2TokenFromCurrentAuth();
      authorizeRequestedGroup(userToken, userGroup);
    }

    Page<Deployment> deployments = deploymentService.getDeployments(pageable, createdBy, userGroup);

    return pagedAssembler.toResource(deployments, deploymentResourceAssembler,
        ControllerLinkBuilder
            .linkTo(
                DummyInvocationUtils
                    .methodOn(DeploymentController.class)
                    .getDeployments(createdBy, userGroup, pageable, pagedAssembler))
            .withSelfRel());

  }

  /**
   * Create a deployment.
   *
   * @param request {@link DeploymentRequest}
   * @return {@link DeploymentResource}
   * @throws ParseException if the claim value is not of required type when parsing user's token
   * @throws ForbiddenException if there is no groups or wlcg groups claim, or they are both empty,
   *         or the requested group is not in the user's allowed groups
   */
  @ResponseStatus(HttpStatus.CREATED)
  @RequestMapping(value = "/deployments", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public DeploymentResource createDeployment(@Valid @RequestBody DeploymentRequest request)
      throws ParseException, ForbiddenException {
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      String userToken = null;
      String requestedGroup = null;
      userToken = oauth2Tokenservice.getOAuth2TokenFromCurrentAuth();
      requestedGroup = request.getUserGroup();
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
      authorizeRequestedGroup(userToken, requestedGroup);
    }
    Deployment deployment = deploymentService.createDeployment(request, owner, requestedWithToken);
    return deploymentResourceAssembler.toResource(deployment);
  }

  /**
   * Update the deployment.
   *
   * @param id
   *          the deployment id
   * @param request
   *          {@link DeploymentRequest}
   */
  @ResponseStatus(HttpStatus.ACCEPTED)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.PUT,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void updateDeployment(@PathVariable("deploymentId") String id,
      @Valid @RequestBody DeploymentRequest request) {
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
    }
    deploymentService.updateDeployment(id, request, requestedWithToken);
  }

  static class DeploymentStatus {
    private String status;

    public void setStatus(String status) {
      this.status = status;
    }

    public String getStatus() {
      return this.status;
    }
  }

  /**
   * Reset the deployment status to an error state.
   *
   * @param id
   *          the deployment id
   * @param status
   *          {@link DeploymentStatus}
   */
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.PATCH,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void resetDeployment(@PathVariable("deploymentId") String id,
      @Valid @RequestBody DeploymentStatus status) {
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
    }
    deploymentService.resetDeployment(id, status.getStatus(), requestedWithToken);
  }

  /**
   * Get the deployment.
   *
   * @param id
   *          the deployment id
   * @return {@link DeploymentResource}
   */
  @ResponseStatus(HttpStatus.OK)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public DeploymentResource getDeployment(@PathVariable("deploymentId") String id) {

    Deployment deployment = deploymentService.getDeployment(id);
    MdcUtils.setDeploymentId(deployment.getId());
    return deploymentResourceAssembler.toResource(deployment);
  }

  /**
   * Get the infrastructure log by deploymentId.
   *
   * @param uuid
   *          the uuid of the deployment
   * @return the log
   */
  @GetMapping(path = "/deployments/{deploymentId}/log")
  @ResponseStatus(HttpStatus.OK)
  public CharSequence getDeploymentLog(@PathVariable("deploymentId") String uuid) {
    MdcUtils.setDeploymentId(uuid);
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
    }
    return deploymentService.getDeploymentLog(uuid, requestedWithToken);
  }

  /**
   * Get the infrastructure info for deploymentId.
   *
   * @param uuid
   *          the uuid of the deployment
   * @return the extra info
   */
  @GetMapping(path = "/deployments/{deploymentId}/extrainfo")
  @ResponseStatus(HttpStatus.OK)
  public CharSequence getDeploymentExtraInfo(@PathVariable("deploymentId") String uuid) {
    MdcUtils.setDeploymentId(uuid);
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
    }
    return deploymentService.getDeploymentExtendedInfo(uuid, requestedWithToken);
  }

  /**
   * Delete the deployment.
   *
   * @param id
   *          the deployment id
   */
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(value = "/deployments/{deploymentId}", method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(OFFLINE_ACCESS_REQUIRED_CONDITION)
  public void deleteDeployment(@PathVariable("deploymentId") String id,
      @RequestParam(name = "force", required = false) @Nullable String force) {
    OidcEntity owner = null;
    OidcTokenId requestedWithToken = null;
    if (oidcProperties.isEnabled()) {
      owner = oauth2Tokenservice.getOrGenerateOidcEntityFromCurrentAuth();
      requestedWithToken = oauth2Tokenservice.exchangeCurrentAccessToken();
    }
    deploymentService.deleteDeployment(id, requestedWithToken, force);
  }
}
