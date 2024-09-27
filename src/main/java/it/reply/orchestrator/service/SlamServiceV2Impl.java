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

import com.google.common.collect.Lists;
import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.SlamProperties;
import it.reply.orchestrator.dal.entity.OidcTokenId;
import it.reply.orchestrator.dto.fedreg.Quota;
import it.reply.orchestrator.dto.fedreg.UserGroup;
import it.reply.orchestrator.dto.slam.Preference;
import it.reply.orchestrator.dto.slam.PreferenceCustomer;
import it.reply.orchestrator.dto.slam.Priority;
import it.reply.orchestrator.dto.slam.Restrictions;
import it.reply.orchestrator.dto.slam.Sla;
import it.reply.orchestrator.dto.slam.SlamPreferences;
import it.reply.orchestrator.dto.slam.Target;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.lang.reflect.Field;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@ServiceVersion("v2")
public class SlamServiceV2Impl implements SlamService {

  private SlamProperties slamProperties;

  private OAuth2TokenService oauth2TokenService;

  private RestTemplate restTemplate;

  private static final Double DEFAULT_WEIGHT = 1.0;

  private static final List<String> ATTRIBUTES_TO_DISCARD =
      Lists.newArrayList("description", "perUser", "type", "usage", "uid", "service");

  /**
   * Creates a new SlamServiceImpl.
   *
   * @param slamProperties the SlamProperties to use
   * @param oauth2TokenService the OAuth2TokenService to use
   * @param restTemplateBuilder the RestTemplateBuilder to use
   */
  public SlamServiceV2Impl(SlamProperties slamProperties, OAuth2TokenService oauth2TokenService,
      RestTemplateBuilder restTemplateBuilder) {
    this.slamProperties = slamProperties;
    this.oauth2TokenService = oauth2TokenService;
    this.restTemplate = restTemplateBuilder.build();
  }

  private List<Preference> remapAttributesForPreferences(UserGroup userGroup) {
    List<Preference> listOfPreferences = new ArrayList<>();
    List<PreferenceCustomer> preferences = new ArrayList<>();
    List<String> serviceTypesPresent = new ArrayList<>();
    List<String> listOfServiceIds = new ArrayList<>();
    userGroup.getSlas().forEach(slaFedReg -> {
      slaFedReg.getProjects().forEach(projectFedReg -> {
        for (Quota quotaFedReg : projectFedReg.getQuotas()) {
          String serviceId = quotaFedReg.getService().getUid();

          // Skip the quota if it refers to the current usage of resources or if the serviceId is
          // already added
          if (Boolean.TRUE.equals(quotaFedReg.getUsage()) || listOfServiceIds.contains(serviceId)) {
            continue;
          }

          listOfServiceIds.add(serviceId);
          String serviceType = quotaFedReg.getService().getType();
          Priority priority = new Priority(slaFedReg.getUid(), serviceId, DEFAULT_WEIGHT);

          // If the serviceType type is already seen as preferenceCustomer type, update this
          // preferenceCustomer adding the new priority
          if (serviceTypesPresent.contains(serviceType)) {
            preferences.forEach(preferencesElem -> {
              if (preferencesElem.getServiceType().equals(serviceType)) {
                preferencesElem.getPriority().add(priority);
              }
            });
          } else {
            // Create a new preferenceCustomer and add the new priority
            serviceTypesPresent.add(serviceType);
            List<Priority> listOfPriorities = new ArrayList<>();
            listOfPriorities.add(priority);
            PreferenceCustomer preferenceCustomer =
                new PreferenceCustomer(serviceType, listOfPriorities);
            preferences.add(preferenceCustomer);
          }
        }
      });
    });
    Preference preference = new Preference(userGroup.getUid(), preferences, null);
    listOfPreferences.add(preference);
    return listOfPreferences;
  }

  private List<Sla> remapAttributesForSla(UserGroup userGroup) {
    List<Sla> listOfSlas = new ArrayList<>();
    userGroup.getSlas().forEach(slaFedReg -> {
      HashMap<it.reply.orchestrator.dto.fedreg.Service, HashMap<String, Restrictions>>
          mapForTargets = new HashMap<>();
      slaFedReg.getProjects().forEach(projectFedReg -> {
        // could go outside this loop

        for (Quota quotaFedReg : projectFedReg.getQuotas()) {
          // Skip the quota if it refers to the current usage of resources
          if (Boolean.TRUE.equals(quotaFedReg.getUsage())) {
            continue;
          }

          Field[] fields = quotaFedReg.getClass().getDeclaredFields();
          HashMap<String, Restrictions> mapForRestrictions = new HashMap<>();
          if (mapForTargets.containsKey(quotaFedReg.getService())) {
            mapForRestrictions = mapForTargets.get(quotaFedReg.getService());
          }

          // Loop over the fields of the Quota class.
          for (Field f : fields) {
            String fieldName = f.getName();
            // Set the attributes as accessible
            f.setAccessible(true);
            // Retrive the value of the field f of the object quotaFedReg
            Object valueObject;
            try {
              valueObject = f.get(quotaFedReg);
            } catch (IllegalArgumentException | IllegalAccessException e) {
              valueObject = null;
              LOG.error(e.getMessage());
            }

            // Skip fields when the value is null or when they belong to the ATTRIBUTES_TO_DISCARD
            // list. They are different depending of the type of Quota.
            if (ATTRIBUTES_TO_DISCARD.contains(fieldName) || valueObject == null) {
              continue;
            }

            Integer value = (Integer) valueObject;
            Restrictions restriction = new Restrictions();
            restriction.setInstanceGuaranteed(null);
            restriction.setInstanceLimit(null);

            // if mapForRestrictions already contains the fieldName key,
            // load the related restriction
            if (mapForRestrictions.containsKey(fieldName)) {
              restriction = mapForRestrictions.get(fieldName);
            }

            // If the quota does not refer to perUser limits set the total* attributes
            // otherwise set the user* attributes
            if (Boolean.FALSE.equals(quotaFedReg.getPerUser())) {
              restriction.setTotalGuaranteed(value);
              restriction.setTotalLimit(value);
            } else {
              restriction.setUserGuaranteed(value);
              restriction.setUserLimit(value);
            }

            // Add the restriction for a given fieldName
            mapForRestrictions.put(fieldName, restriction);
          }

          // Add the mapForRestrictions for a given Service
          mapForTargets.put(quotaFedReg.getService(), mapForRestrictions);
        }

        // Create the listOfTargets from mapForTargets,
        // create the slamService from listOfTargets
        // create and fill the listOfServices from the slamServices
        List<it.reply.orchestrator.dto.slam.Service> listOfServices = new ArrayList<>();
        mapForTargets.keySet().forEach(service -> {
          HashMap<String, Restrictions> value = mapForTargets.get(service);
          List<Target> listOfTargets = new ArrayList<>();
          value.keySet().forEach(key -> {
            Target target = new Target(key, null, value.get(key));
            listOfTargets.add(target);
          });
          it.reply.orchestrator.dto.slam.Service slamService =
              new it.reply.orchestrator.dto.slam.Service(service.getType(), service.getUid(),
                  listOfTargets);
          listOfServices.add(slamService);
        });

        // Create a sla and fill the listOfSlas
        Sla sla = new Sla(userGroup.getUid(), projectFedReg.getProvider().getUid(),
            new SimpleDateFormat("yyyy-MM-dd").format(slaFedReg.getStartDate()),
            new SimpleDateFormat("yyyy-MM-dd").format(slaFedReg.getEndDate()), listOfServices,
            slaFedReg.getUid());
        listOfSlas.add(sla);
      });
    });
    return listOfSlas;
  }

  @Override
  public SlamPreferences getCustomerPreferences(OidcTokenId tokenId, @Nullable String userGroup) {

    String slamCustomer =
        Optional.ofNullable(userGroup).orElse(oauth2TokenService.getOrganization(tokenId));

    URI requestUriFedRegUserGroup = UriComponentsBuilder
        .fromHttpUrl(slamProperties.getUrl() + slamProperties.getCustomerPreferencesPath())
        .queryParam("with_conn", true).queryParam("name", slamCustomer)
        .queryParam("idp_endpoint", tokenId.getOidcEntityId().getIssuer())
        .queryParam("provider_status", "active").build().normalize().toUri();

    List<UserGroup> userGroupCall = null;
    try {
      userGroupCall = oauth2TokenService.executeWithClientForResult(tokenId, accessToken -> {
        HeadersBuilder<?> requestBuilder = RequestEntity.get(requestUriFedRegUserGroup);
        if (accessToken != null) {
          requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return restTemplate.exchange(requestBuilder.build(),
            new ParameterizedTypeReference<List<UserGroup>>() {});
      }, OAuth2TokenService.restTemplateTokenRefreshEvaluator).getBody();
    } catch (RestClientException ex) {
      throw new DeploymentException(
          "Error fetching SLA for customer <" + slamCustomer + "> from SLAM.", ex);
    }

    if (userGroupCall == null) {
      throw new DeploymentException(
          "The call to the user_group endpoint of the federation-registry is null for customer <"
              + slamCustomer + ">.");
    }

    return new SlamPreferences(remapAttributesForPreferences(userGroupCall.get(0)),
        remapAttributesForSla(userGroupCall.get(0)));
  }

}
