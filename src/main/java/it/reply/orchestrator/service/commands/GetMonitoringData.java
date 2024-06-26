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

package it.reply.orchestrator.service.commands;

import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.Group;
import it.reply.monitoringpillar.domain.dsl.monitoring.pillar.wrapper.paas.PaasMachine;
import it.reply.orchestrator.config.properties.MonitoringProperties;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.service.MonitoringService;
import it.reply.orchestrator.utils.WorkflowConstants;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.GET_MONITORING_DATA)
@Slf4j
public class GetMonitoringData extends BaseRankCloudProvidersCommand {

  @Autowired
  private MonitoringService monitoringService;

  @Autowired
  private MonitoringProperties monitoringProperties;

  @Override
  public void execute(DelegateExecution execution,
      RankCloudProvidersMessage rankCloudProvidersMessage) {

    if (isValid(monitoringProperties.getUrl())) {
      // Get monitoring data for each Cloud Provider
      Map<String, List<PaasMachine>> metrics =
          rankCloudProvidersMessage.getCloudProviders().keySet().stream().map(providerId -> {
            try {
              return monitoringService.getProviderData(providerId);
            } catch (RuntimeException ex) {
              LOG.warn("Error retrieving monitoring data for provider <{}>", providerId, ex);
              return null;
            }
          }).filter(Objects::nonNull)
              .collect(Collectors.toMap(Group::getGroupName, Group::getPaasMachines));

      rankCloudProvidersMessage.setCloudProvidersMonitoringData(metrics);
    }
  }

  private boolean isValid(URI url) {

    if (url == null) {
      return false;
    }
    return !url.toString().trim().isEmpty();
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error retrieving info from monitoring service";
  }
}
