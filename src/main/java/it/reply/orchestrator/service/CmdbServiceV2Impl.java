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

package it.reply.orchestrator.service;

import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.CmdbProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudService.SupportedIdp;
import it.reply.orchestrator.dto.cmdb.CloudService.VolumeType;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.cmdb.CmdbIdentifiable;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.cmdb.Tenant;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbDataWrapper;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbHasManyList;
import it.reply.orchestrator.dto.cmdb.wrappers.CmdbRow;
import it.reply.orchestrator.dto.fedreg.AuthMethod;
import it.reply.orchestrator.dto.fedreg.Network;
import it.reply.orchestrator.dto.fedreg.Project;
import it.reply.orchestrator.dto.fedreg.Quota;
import it.reply.orchestrator.dto.fedreg.Region;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@ServiceVersion("v2")
public class CmdbServiceV2Impl implements CmdbService {

  private OAuth2TokenService oauth2TokenService;

  private static final ParameterizedTypeReference<CmdbDataWrapper<CloudProvider>>
      PROVIDER_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<CloudProvider>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<CloudService>>
      CLOUD_SERVICES_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<CloudService>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<CloudService>>
      CLOUD_SERVICE_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<CloudService>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<Image>>
      IMAGES_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<Image>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<Image>>
      IMAGE_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<Image>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<Flavor>>
      FLAVORS_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<Flavor>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<Flavor>>
      FLAVOR_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<Flavor>>() {
      };

  private static final ParameterizedTypeReference<CmdbHasManyList<Tenant>>
      TENANTS_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbHasManyList<Tenant>>() {
      };

  private static final ParameterizedTypeReference<CmdbDataWrapper<Tenant>>
      TENANT_RESPONSE_TYPE =
      new ParameterizedTypeReference<CmdbDataWrapper<Tenant>>() {
      };

  private CmdbProperties cmdbProperties;

  private RestTemplate restTemplate;

  /**
   * Costrcutor of the CmdbServiceV2Impl class.
   *
   * @param cmdbProperties contains cmdb properties
   * @param oauth2TokenService the service used to handle tokens
   * @param restTemplateBuilder the builder for restTemplate
   */
  public CmdbServiceV2Impl(CmdbProperties cmdbProperties, OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.cmdbProperties = cmdbProperties;
    this.restTemplate = restTemplateBuilder.build();
    this.oauth2TokenService = oauth2TokenService;
  }

  private <T extends CmdbIdentifiable> T unwrap(CmdbDataWrapper<T> wrapped) {
    String id = wrapped.getId();
    T unwrapped = wrapped.getData();
    unwrapped.setId(id);
    return unwrapped;
  }

  private <T extends CmdbIdentifiable> T get(URI from,
      ParameterizedTypeReference<CmdbDataWrapper<T>> type) {
    return unwrap(restTemplate
        .exchange(from, HttpMethod.GET, null, type)
        .getBody());
  }

  private <T extends CmdbIdentifiable> List<T> getAll(URI from,
      ParameterizedTypeReference<CmdbHasManyList<T>> type) {
    return restTemplate
        .exchange(from, HttpMethod.GET, null, type)
        .getBody()
        .getRows()
        .stream()
        .map(CmdbRow::getDoc)
        .map(this::unwrap)
        .collect(Collectors.toList());
  }

  @Override
  public CloudProvider getProviderById(String providerId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getProviderByIdPath())
        .buildAndExpand(providerId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, PROVIDER_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public CloudService getServiceById(String serviceId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getServiceByIdPath())
        .buildAndExpand(serviceId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, CLOUD_SERVICE_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for service <" + serviceId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<CloudService> getServicesByProvider(String providerId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getServicesByProviderIdPath())
        .buildAndExpand(providerId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, CLOUD_SERVICES_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading services list for provider <" + providerId + "> from CMDB.", ex);
    }
  }

  @Override
  public Image getImageById(String imageId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getImageByIdPath())
        .buildAndExpand(imageId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, IMAGE_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException("Error loading info for image <" + imageId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<Image> getImagesByTenant(String tenantId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getImagesByTenantIdPath())
        .buildAndExpand(tenantId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, IMAGES_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading images list for tenant <" + tenantId + "> from CMDB.", ex);
    }
  }

  @Override
  public Flavor getFlavorById(String flavorId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getFlavorByIdPath())
        .buildAndExpand(flavorId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, FLAVOR_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for flavor <" + flavorId + "> from CMDB.",
          ex);
    }
  }

  @Override
  public List<Flavor> getFlavorsByTenant(String tenantId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getFlavorsByTenantIdPath())
        .buildAndExpand(tenantId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, FLAVORS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading flavor list for tenant <" + tenantId + "> from CMDB.", ex);
    }
  }

  @Override
  public List<Tenant> getTenantsByService(String serviceId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsByServiceIdPath())
        .buildAndExpand(serviceId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, TENANTS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading tenant list for service <" + serviceId + "> from CMDB.", ex);
    }
  }

  /*  @Override
  public List<Tenant> getTenantsByOrganisation(String organisationId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsByOrganizationIdPath())
        .buildAndExpand(organisationId)
        .normalize()
        .toUri();

    try {
      return getAll(requestUri, TENANTS_LIST_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading tenant list for organisation <" + organisationId + "> from CMDB.", ex);
    }
  }
  */

  /**
   * Temporary hack: cmdbProperties.getTenantsByOrganizationIdPath() does not work with
   * organization names that contain slash, e.g. kube/users. Therefore, as a workaround
   * here we first get the full list of tenants and then we filter the list
   */
  @Override
  public List<Tenant> getTenantsByOrganisation(String organisationId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsListPath())
        .build()
        .normalize()
        .toUri();

    try {
      List<Tenant> tenants = getAll(requestUri, TENANTS_LIST_RESPONSE_TYPE);
      return tenants.stream().filter(t -> Objects.nonNull(t.getIamOrganisation())
                  && t.getIamOrganisation().equals(organisationId)).collect(Collectors.toList());
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading tenant list for organisation <" + organisationId + "> from CMDB.", ex);
    }
  }

  @Override
  public Tenant getTenantById(String tenantId) {

    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantByIdPath())
        .buildAndExpand(tenantId)
        .normalize()
        .toUri();

    try {
      return get(requestUri, TENANT_RESPONSE_TYPE);
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error loading info for tenant <" + tenantId + "> from CMDB.",
          ex);
    }
  }

  private CloudProvider remapAttributes(Project project) {
    List<SupportedIdp> supportedIdps = new ArrayList<>();
    List<VolumeType> volumeTypes = new ArrayList<>();
    AtomicReference<String> idpProtocol = new AtomicReference<>(null);
    HashMap<String, CloudService> mapOfCloudServices = new HashMap<>();
    AuthMethod relationship = project.getSla().getUserGroup().getIdentityProvider().getProviders()
        .get(0).getRelationship();
    SupportedIdp supportedIdp = new SupportedIdp(relationship.getIdpName(),
        project.getSla().getUserGroup().getIdentityProvider().getEndpoint());
    supportedIdps.add(supportedIdp);
    idpProtocol.set(relationship.getProtocol());
    String publicNetworkName = null;
    String privateNetworkName = null;
    String privateNetworkProxyHost = null;
    String privateNetworkProxyUser = null;

    for (Network network : project.getNetworks()) {
      if (Boolean.TRUE.equals(network.getIsDefault())) {
        if (Boolean.TRUE.equals(network.getIsShared())) {
          publicNetworkName = network.getName();
        } else {
          privateNetworkName = network.getName();
          privateNetworkProxyHost = network.getProxyHost();
          privateNetworkProxyUser = network.getProxyUser();
        }
      }
    }

    for (Quota quotaFedReg : project.getQuotas()) {

      // skip quotaFedReg if it is related to resource usage or if it is specific for a user
      if (Boolean.TRUE.equals(quotaFedReg.getUsage())
          || Boolean.TRUE.equals(quotaFedReg.getPerUser())) {
        continue;
      }

      String serviceType = quotaFedReg.getService().getType().replace('-', '_').toUpperCase();
      String regionName = quotaFedReg.getService().getRegion().getName();
      Region region = project.getProvider().getRegions().stream()
          .filter(r -> r.getName().equals(regionName)).collect(Collectors.toList()).get(0);
      String serviceEndpoint = region.getIdentityServices().get(0).getEndpoint();
      URL url = null;
      try {
        url = new URL(serviceEndpoint);
      } catch (MalformedURLException e) {
        LOG.error(e.getMessage());
      }

      // If the service is of type COMPUTE creates a ComputeService otherwise create a CloudService
      if (CloudServiceType.valueOf(serviceType).equals(CloudServiceType.COMPUTE)) {
        List<Image> listOfImages = new ArrayList<>();
        List<Flavor> listOfFlavors = new ArrayList<>();
        project.getImages().forEach(imageElem -> {
          Image image = new Image(imageElem.getUid(), imageElem.getUuid(), imageElem.getName(),
              imageElem.getDescription(), imageElem.getArchitecture(), imageElem.getOsType(),
              imageElem.getOsDistro(), imageElem.getOsVersion(), null, null,
              imageElem.getGpuDriver(), null, imageElem.getCudaSupport(), null, null);
          listOfImages.add(image);
        });
        project.getFlavors().forEach(flavorElem -> {
          Flavor flavor = new Flavor(flavorElem.getUid(), flavorElem.getUuid(),
              flavorElem.getName(), flavorElem.getRam().doubleValue(), flavorElem.getVcpus(),
              flavorElem.getDisk().doubleValue(), flavorElem.getGpus(), flavorElem.getGpuVendor(),
              flavorElem.getGpuModel(), flavorElem.getInfiniband());
          listOfFlavors.add(flavor);
        });

        ComputeService computeService = new ComputeService(quotaFedReg.getService().getUid(),
            quotaFedReg.getService().getName(), serviceEndpoint, null,
            project.getProvider().getUid(), CloudServiceType.valueOf(serviceType),
            project.getProvider().getIsPublic(), project.getName(), regionName, url.getHost(), null,
            listOfImages, listOfFlavors, true, idpProtocol.get(), true, supportedIdps, volumeTypes,
            publicNetworkName, null, privateNetworkName, privateNetworkProxyHost,
            privateNetworkProxyUser);
        mapOfCloudServices.put(quotaFedReg.getService().getUid(), computeService);
      } else {
        CloudService cloudService = new CloudService(quotaFedReg.getService().getUid(),
            quotaFedReg.getService().getName(), serviceEndpoint, null,
            project.getProvider().getUid(), CloudServiceType.valueOf(serviceType),
            project.getProvider().getIsPublic(), project.getName(), regionName, url.getHost(), null,
            true, idpProtocol.get(), true, supportedIdps, volumeTypes);
        mapOfCloudServices.put(quotaFedReg.getService().getUid(), cloudService);
      }
    }

    return new CloudProvider(project.getProvider().getUid(), project.getProvider().getName(),
        mapOfCloudServices);
  }

  @Override
  public CloudProvider fillCloudProviderInfo(String providerId, Set<String> servicesWithSla,
      String organisation, RankCloudProvidersMessage rankCloudProvidersMessage) {

    OidcTokenId requestedWithToken = rankCloudProvidersMessage.getRequestedWithToken();

    URI requestUriFedRegProject = UriComponentsBuilder
        .fromHttpUrl(cmdbProperties.getUrl() + cmdbProperties.getTenantsListPath())
        .queryParam("with_conn", true)
        .queryParam("user_group_uid",
            rankCloudProvidersMessage.getSlamPreferences().getPreferences().get(0).getCustomer())
        .queryParam("provider_uid", providerId).build().normalize().toUri();

    List<Project> projectCall =
        oauth2TokenService.executeWithClientForResult(requestedWithToken, accessToken -> {
          HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUriFedRegProject);
          if (accessToken != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
          }
          return restTemplate.exchange(requestBuilder.build(),
              new ParameterizedTypeReference<List<Project>>() {});
        }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();

    return remapAttributes(projectCall.get(0));
  }

}
