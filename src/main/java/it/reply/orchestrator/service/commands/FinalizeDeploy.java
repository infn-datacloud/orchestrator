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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.reply.orchestrator.dto.deployment.DeploymentMessage;
import it.reply.orchestrator.utils.WorkflowConstants;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component(WorkflowConstants.Delegate.FINALIZE_DEPLOY)
@Slf4j
public class FinalizeDeploy extends BaseDeployCommand {

  @Override
  public void execute(DelegateExecution execution, DeploymentMessage deploymentMessage) {
    getDeploymentProviderService(deploymentMessage).finalizeDeploy(deploymentMessage);
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode logData = objectMapper.createObjectNode();
    logData.put("uuid", deploymentMessage.getDeploymentId());
    logData.put("status", "CREATE_COMPLETE");

    // Print information about the submission of the deployment
    String jsonString = null;
    try {
      jsonString = objectMapper.writeValueAsString(logData);
      LOG.info("Deployment completed successfully. {}", jsonString);
    } catch (JsonProcessingException e) {
      LOG.error(e.getMessage());
    }
  }

  @Override
  protected String getErrorMessagePrefix() {
    return "Error finalizing deployment";
  }

}
