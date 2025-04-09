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
import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.cmdb.CloudProvider;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceType;
import it.reply.orchestrator.dto.ranker.AiRankedCloudService;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.exception.service.DeploymentException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@ServiceVersion("v2")
public class CloudProviderRankerServiceV2Impl implements CloudProviderRankerService {
 
  private static final ParameterizedTypeReference<List<AiRankedCloudService>> RESPONSE_TYPE =
      new ParameterizedTypeReference<List<AiRankedCloudService>>() {};
 
  private CprProperties cprProperties;
 
  private RestTemplate restTemplate;
 
  /**
  * Creates a new CloudProviderRankerServiceV2Impl.
  *
  * @param cprProperties the CprProperties
  * @param restTemplateBuilder the RestTemplateBuilder
  */  
  public CloudProviderRankerServiceV2Impl(CprProperties cprProperties,
      RestTemplateBuilder restTemplateBuilder) {
    this.cprProperties = cprProperties;
    this.restTemplate = restTemplateBuilder.build();
  }
 
  @Override
  public List<RankedCloudService> getProviderServicesRanking(
      CloudProviderRankerRequest cloudProviderRankerRequest) {
 
    URI requestUri = UriComponentsBuilder
        .fromHttpUrl(cprProperties.getUrl() + cprProperties.getRankPath())
        .build()
        .normalize()
        .toUri();
 
    HttpEntity<String> entity = new HttpEntity<>(cloudProviderRankerRequest.getDeploymentId());
    List<RankedCloudService> result = new ArrayList<RankedCloudService>();
 
    try {
      List<AiRankedCloudService> airanking = restTemplate.exchange(requestUri, HttpMethod.POST,
          entity, RESPONSE_TYPE).getBody();
      if (airanking.size() > 0) {
        Map<String, CloudProvider> cloudProviders = cloudProviderRankerRequest.getCloudProviders();
        for (AiRankedCloudService aiservice : airanking) {
          for (CloudProvider provider : cloudProviders.values()) {
            if (provider.getName().equalsIgnoreCase(aiservice.getProvider())) {
              for (CloudService service : provider.getServices().values()) {
                if (service.getType() == CloudServiceType.COMPUTE 
                    && (StringUtils.isEmpty(service.getRegion()) 
                    || service.getRegion().equalsIgnoreCase(aiservice.getRegion()))) {
                  result.add(RankedCloudService
                      .builder()
                      .provider(provider.getId())
                      .serviceId(service.getId())
                      .totalScore(aiservice.getClassification())
                      .ranked(false)
                      .build());
                }
              }
            }
          }
        }
        // sort results
        List<RankedCloudService> sortedResult = result.stream()
            .sorted(Comparator.comparing(RankedCloudService::getTotalScore).reversed())
            .collect(Collectors.toList());
        result.clear();
        int count = 1;
        for (RankedCloudService rank : sortedResult) {
          rank.setRank(count);
          rank.setRanked(true);
          result.add(rank);
          count++;
        }
        
      }
      return result;
    } catch (RestClientException ex) {
      throw new DeploymentException("Error retrieving cloud provider ranking data", ex);
    }
  }
}
 