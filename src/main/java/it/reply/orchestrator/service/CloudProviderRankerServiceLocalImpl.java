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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.reply.orchestrator.annotation.ServiceVersion;
import it.reply.orchestrator.config.properties.CprProperties;
import it.reply.orchestrator.dto.ranker.CloudProviderRankerRequest;
import it.reply.orchestrator.dto.ranker.RankedCloudService;
import it.reply.orchestrator.exception.OrchestratorException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@ServiceVersion(CloudProviderRankerServiceLocalImpl.SERVICE_VERSION)
public class CloudProviderRankerServiceLocalImpl implements CloudProviderRankerService {

  public static final String SERVICE_VERSION = "local";

  private final CprProperties cprProperties;
  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;
  
  /**
  * Creates a new CloudProviderRankerServiceLocalImpl.
  *
  * @param cprProperties the CprProperties
  * @param restTemplateBuilder the RestTemplateBuilder
  * @param objectMapper the objectMapper
  * @param resourceLoader the ResourceLoader
  */
  public CloudProviderRankerServiceLocalImpl(CprProperties cprProperties, 
      RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper, 
      ResourceLoader resourceLoader) {
    this.cprProperties = cprProperties;
    this.objectMapper = objectMapper;
    this.resourceLoader = resourceLoader;
  }

  @Override
  public List<RankedCloudService> getProviderServicesRanking(
      CloudProviderRankerRequest cloudProviderRankerRequest) {

    return loadData();
  }

  private List<RankedCloudService> loadData() {
    String location = cprProperties.getUrl().toString();
    Resource serializedPreferences = resourceLoader
          .getResource(location);
    try (InputStream is = serializedPreferences.getInputStream()) {
      TypeReference<List<RankedCloudService>> typeRef 
          = new TypeReference<List<RankedCloudService>>() {};
      return objectMapper.readValue(is, typeRef);
    } catch (IOException e) {
      throw new OrchestratorException("Error loading local CPR preferences from " + location, e);
    }
  }

}
