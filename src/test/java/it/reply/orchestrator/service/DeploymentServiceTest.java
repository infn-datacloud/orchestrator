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

import alien4cloud.tosca.model.ArchiveRoot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import it.reply.orchestrator.config.properties.OidcProperties;
import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dal.entity.OidcEntityId;
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dal.repository.DeploymentRepository;
import it.reply.orchestrator.dal.repository.ResourceRepository;
import it.reply.orchestrator.dto.request.DeploymentRequest;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.NodeStates;
import it.reply.orchestrator.enums.Status;
import it.reply.orchestrator.exception.http.BadRequestException;
import it.reply.orchestrator.exception.http.ConflictException;
import it.reply.orchestrator.exception.http.NotFoundException;
import it.reply.orchestrator.service.deployment.providers.DeploymentProviderServiceRegistry;
import it.reply.orchestrator.service.deployment.providers.ImServiceImpl;
import it.reply.orchestrator.service.security.OAuth2TokenService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.util.Lists;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@JsonTest
@RunWith(JUnitParamsRunner.class)
public class DeploymentServiceTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @InjectMocks
  private DeploymentService deploymentService = new DeploymentServiceImpl();

  @Mock
  private DeploymentProviderServiceRegistry deploymentProviderServiceRegistry;

  @Mock
  private ImServiceImpl imService;

  @Mock
  private DeploymentRepository deploymentRepository;

  @Mock
  private ResourceRepository resourceRepository;

  @Spy
  private ToscaServiceImpl toscaService = new ToscaServiceImpl();

  @Mock
  private RuntimeServiceImpl wfService;

  @Mock
  private OAuth2TokenService oauth2TokenService;

  @Mock
  private OidcProperties oidcProperties;

  @Spy
  @Autowired
  private ObjectMapper objectMapper;

  private static final String nodeTypeCompute = "tosca.nodes.indigo.Compute";

  @Test
  public void getDeploymentsSuccessful() throws Exception {
    List<Deployment> deployments = ControllerTestUtils.createDeployments(5);

    Mockito
        .when(deploymentRepository.findAllByOwner((OidcEntityId)null, (Pageable) null))
        .thenReturn(new PageImpl<Deployment>(deployments));

    Page<Deployment> pagedDeployment = deploymentService.getDeployments(null, null, null);

    assertThat(pagedDeployment.getContent()).isEqualTo(deployments);

  }

  @Test
  public void getDeploymentsPagedSuccessful() throws Exception {
    Pageable pageable = new PageRequest(0, 10);
    List<Deployment> deployments = ControllerTestUtils.createDeployments(10);

    Mockito
        .when(deploymentRepository.findAllByOwner((OidcEntityId)null,pageable))
        .thenReturn(new PageImpl<Deployment>(deployments));

    Page<Deployment> pagedDeployment = deploymentService.getDeployments(pageable, null, null);

    assertThat(pagedDeployment.getContent()).isEqualTo(deployments);
    assertThat(pagedDeployment.getNumberOfElements()).isEqualTo(10);
  }

  @Test
  public void getDeploymentSuccessful() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    Deployment returneDeployment = deploymentService.getDeployment(deployment.getId());

    assertThat(returneDeployment).isEqualTo(deployment);
  }

  @Test
  public void getDeploymentError() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(null);

    assertThatThrownBy(() -> deploymentService.getDeployment(deployment.getId()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Parameters({"true", "false"})
  public void getDeploymentExtendedInfo(boolean ispresent) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentProviderServiceRegistry.getDeploymentProviderService(deployment.getId()))
        .thenReturn(imService);
    if (ispresent) {
      Mockito.when(imService.getDeploymentExtendedInfo(Mockito.any()))
          .thenReturn(Optional.of("extendedinfo"));
      assertThat(deploymentService.getDeploymentExtendedInfo(deployment.getId(), null)).isEqualTo("extendedinfo");
    } else {
      Mockito.when(imService.getDeploymentExtendedInfo(Mockito.any()))
          .thenReturn(Optional.empty());
      assertThat(deploymentService.getDeploymentExtendedInfo(deployment.getId(), null)).isEqualTo("[]");
    }
  }

  @Test
  @Parameters({"true", "false"})
  public void getDeploymentLog(boolean ispresent) {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    Mockito.when(deploymentProviderServiceRegistry.getDeploymentProviderService(deployment.getId()))
        .thenReturn(imService);
    if (ispresent) {
      Mockito.when(imService.getDeploymentLog(Mockito.any()))
          .thenReturn(Optional.of("deploymentlog"));
      assertThat(deploymentService.getDeploymentLog(deployment.getId(), null)).isEqualTo("deploymentlog");
    } else {
      Mockito.when(imService.getDeploymentLog(Mockito.any()))
          .thenReturn(Optional.empty());
      assertThat(deploymentService.getDeploymentLog(deployment.getId(), null)).isEqualTo("");
    }
  }

 private Deployment basecreateDeploymentSuccessful(DeploymentRequest deploymentRequest,
      Map<String, NodeTemplate> nodeTemplates) throws Exception {

    if (nodeTemplates == null) {
      nodeTemplates = Maps.newHashMap();
    }

    ArchiveRoot parsingResult = new ArchiveRoot();
    parsingResult.setTopology(new Topology());
    parsingResult.getTopology().setNodeTemplates(nodeTemplates);

    Mockito.doReturn(parsingResult).when(toscaService).prepareTemplate(
        deploymentRequest.getTemplate(),
        deploymentRequest.getParameters());

    Mockito
        .when(deploymentRepository.save(Mockito.any(Deployment.class)))
        .thenAnswer(y -> {
          Deployment deployment = (Deployment) y.getArguments()[0];
          deployment.setId(UUID.randomUUID().toString());
          return deployment;
        });

    Mockito.when(resourceRepository.save(Mockito.any(Resource.class))).thenAnswer(y -> {
      Resource res = (Resource) y.getArguments()[0];
      res.getDeployment().getResources().add(res);
      return res;
    });

    ProcessInstanceBuilder builder = Mockito.spy(new ProcessInstanceBuilderImpl(wfService));
    Mockito
        .when(wfService.createProcessInstanceBuilder())
        .thenReturn(builder);
    Mockito.when(builder.start()).thenReturn(Mockito.mock(ProcessInstance.class));

    return deploymentService.createDeployment(deploymentRequest, null, null);
  }

  @Test
  public void createComputeDeploymentSuccessful() throws Exception {

    String nodeName1 = "server1";
    String nodeName2 = "server2";

    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();

    Map<String, Capability> capabilities = Maps.newHashMap();

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeTypeCompute);
    nt.setName(nodeName1);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeTypeCompute);
    nt.setName(nodeName2);
    nts.put(nodeName2, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources()).hasSize(2);

    assertThat(returneDeployment.getResources())
        .extracting(Resource::getToscaNodeName)
        .containsExactlyInAnyOrder(nodeName1, nodeName2);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeTypeCompute);
      assertThat(resource.getState()).isEqualTo(NodeStates.INITIAL);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));

    Mockito.verify(deploymentRepository, Mockito.atLeast(1)).save(returneDeployment);
  }

  @Test
  public void createComputeScalableDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();

    Capability capability = new Capability();
    capability.setProperties(Maps.newHashMap());

    Map<String, Capability> capabilities = Maps.newHashMap();
    capabilities.put("scalable", capability);

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeTypeCompute);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put("server", nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources()).hasSize(1);
  }

  @Test
  public void createComputeScalableWithCountDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();

    String nodeName = "server";

    Capability capability = new Capability();
    capability.setProperties(Maps.newHashMap());
    ScalarPropertyValue countValue = new ScalarPropertyValue("2");
    capability.getProperties().put("count", countValue);

    Map<String, Capability> capabilities = Maps.newHashMap();
    capabilities.put("scalable", capability);

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeTypeCompute);
    nt.setName(nodeName);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources()).hasSize(2);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeName()).isEqualTo(nodeName);
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeTypeCompute);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));

  }

  @Test
  public void createDeploymentWithCallbackSuccessful() throws Exception {
    String callback = "http://localhost:8080";
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .callback(callback)
        .build();

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, null);

    assertThat(returneDeployment.getCallback()).isEqualTo(callback);
  }

  @Test
  @Parameters({"nul,nul", "nul,1", "1,nul", "1,1", "1,2", "2,1"})
  public void createDeploymentWithTimeoutValuesError(String timeout,
      String providerTimeout) throws Exception {
    @Nullable Integer iTimeout = (timeout.compareTo("nul") == 0 ? null : Integer.parseInt(timeout));
    @Nullable Integer iProviderTimeout = (providerTimeout.compareTo("nul") == 0 ? null : Integer.parseInt(providerTimeout));
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .timeoutMins(iTimeout)
        .providerTimeoutMins(iProviderTimeout)
        .build();

    AbstractThrowableAssert<?, ? extends Throwable> assertion = assertThatCode(
        () -> basecreateDeploymentSuccessful(deploymentRequest, null));
    if (iTimeout != null && iProviderTimeout != null
        && iProviderTimeout > iTimeout) {
      assertion.isInstanceOf(BadRequestException.class)
          .hasMessage("ProviderTimeout must be <= Timeout");
    } else {
      assertion.doesNotThrowAnyException();
    }
  }

  @Test
  public void createChronosDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();

    String nodeName1 = "job1";
    String nodeName2 = "job2";
    String nodeType = "tosca.nodes.indigo.Container.Application.Docker.Chronos";

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    NodeTemplate nt = new NodeTemplate();
    nt.setType(nodeType);
    nt.setName(nodeName1);
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setType(nodeType);
    nt.setName(nodeName2);
    nts.put(nodeName2, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources())
        .extracting(Resource::getToscaNodeName)
        .containsExactlyInAnyOrder(nodeName1, nodeName2);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeType);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));
  }

  @Test
  public void createQcgDeploymentSuccessful() throws Exception {
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();

    String nodeName1 = "job1";
    String nodeType = "tosca.nodes.indigo.Qcg.Job";

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    NodeTemplate nt = new NodeTemplate();
    nt.setType(nodeType);
    nt.setName(nodeName1);
    nts.put(nodeName1, nt);

    Deployment returneDeployment = basecreateDeploymentSuccessful(deploymentRequest, nts);

    assertThat(returneDeployment.getResources())
        .extracting(Resource::getToscaNodeName)
        .containsExactlyInAnyOrder(nodeName1);
    assertThat(returneDeployment.getResources()).allSatisfy(resource -> {
      assertThat(resource.getToscaNodeType()).isEqualTo(nodeType);
    });

    returneDeployment
        .getResources()
        .forEach(resource -> Mockito.verify(resourceRepository).save(resource));
  }


  @Test
  public void deleteDeploymentNotFound() throws Exception {
    Mockito.when(deploymentRepository.findOne("id")).thenReturn(null);

    String force = "false";
    assertThatThrownBy(() -> deploymentService.deleteDeployment("id", null, force))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Parameters({
      "DELETE_IN_PROGRESS",
      "DELETE_COMPLETE" })
  public void deleteDeploymentFailForConflict(Status status) throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(status);
    String force = "false";

    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    assertThatThrownBy(() -> deploymentService.deleteDeployment(deployment.getId(), null, force))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @Parameters({
      "CREATE_IN_PROGRESS",
      "CREATE_COMPLETE",
      "CREATE_FAILED",
      "UPDATE_IN_PROGRESS",
      "UPDATE_COMPLETE",
      "UPDATE_FAILED",
      "DELETE_FAILED",
      "UNKNOWN" })
  public void deleteDeploymentSuccesfulNoProvider(Status status) throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(status);
    String force = "false";
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    ExecutionQueryImpl executionQueryImpl = Mockito.spy(new ExecutionQueryImpl());
    Mockito.when(wfService.createExecutionQuery()).thenReturn(executionQueryImpl);
    Mockito.doReturn(Lists.emptyList()).when(executionQueryImpl).list();

    deploymentService.deleteDeployment(deployment.getId(), null, force);

    Mockito.verify(wfService, Mockito.never()).startProcessInstance(Mockito.any());
    Mockito.verify(deploymentRepository, Mockito.times(1)).delete(deployment);
  }

  @Test
  @Parameters({
      "CREATE_IN_PROGRESS",
      "CREATE_COMPLETE",
      "CREATE_FAILED",
      "UPDATE_IN_PROGRESS",
      "UPDATE_COMPLETE",
      "UPDATE_FAILED",
      "DELETE_FAILED",
      "UNKNOWN" })
  public void deleteDeploymentSuccesfulWithProvider(Status status) throws Exception {

    Deployment deployment = ControllerTestUtils.createDeployment();
    deployment.setStatus(status);
    deployment.setDeploymentProvider(DeploymentProvider.IM);
    String force = "false";
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);

    ProcessInstanceBuilderImpl builder = Mockito.spy(new ProcessInstanceBuilderImpl(wfService));
    Mockito
        .when(wfService.createProcessInstanceBuilder())
        .thenReturn(builder);
    Mockito.when(builder.start()).thenReturn(Mockito.mock(ProcessInstance.class));

    ExecutionQueryImpl executionQueryImpl = Mockito.spy(new ExecutionQueryImpl());
    Mockito.when(wfService.createExecutionQuery()).thenReturn(executionQueryImpl);
    Mockito.doReturn(Lists.emptyList()).when(executionQueryImpl).list();

    deploymentService.deleteDeployment(deployment.getId(), null, force);

    Mockito.verify(wfService, Mockito.times(1)).startProcessInstance(Mockito.eq(builder));
    Mockito.verify(deploymentRepository, Mockito.times(0)).delete(deployment);
  }

  // test fail with chrono
  // TO-DO

  @Test
  @Parameters({
      "CHRONOS",
      "MARATHON",
      "QCG"})
  public void updateDeploymentBadRequest(DeploymentProvider provider) throws Exception {

    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment(id);
    deployment.setDeploymentProvider(provider);
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();
    assertThatThrownBy(() -> deploymentService.updateDeployment(id, deploymentRequest, null))
        .isInstanceOf(BadRequestException.class);

  }

  @Test
  public void updateDeploymentNotFound() throws Exception {
    String id = UUID.randomUUID().toString();
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(null);
    assertThatThrownBy(() -> deploymentService.updateDeployment(id, null, null))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @Parameters({
      "CREATE_FAILED",
      "CREATE_IN_PROGRESS",
      "DELETE_IN_PROGRESS",
      "DELETE_FAILED",
      "DELETE_COMPLETE",
      "UPDATE_IN_PROGRESS",
      "UNKNOWN" })
  public void updateDeploymentConflict(Status status) throws Exception {
    String id = UUID.randomUUID().toString();
    Deployment deployment = ControllerTestUtils.createDeployment(id);
    deployment.setDeploymentProvider(DeploymentProvider.HEAT);
    deployment.setStatus(status);
    Mockito.when(deploymentRepository.findOne(id)).thenReturn(deployment);
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();

    assertThatThrownBy(() -> deploymentService.updateDeployment(id, deploymentRequest, null))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @Parameters({
      "CREATE_COMPLETE",
      "UPDATE_FAILED",
      "UPDATE_COMPLETE" })
  public void updateDeploymentSuccess(Status status) throws Exception {
    DeploymentRequest deploymentRequest = DeploymentRequest
        .builder()
        .template("template")
        .build();
    Map<String, NodeTemplate> nts = getNodeTemplates();

    // case create complete
    Deployment deployment = basecreateDeploymentSuccessful(deploymentRequest, nts);
    deployment.setDeploymentProvider(DeploymentProvider.IM);

    deployment.setStatus(status);
    Mockito.when(deploymentRepository.findOne(deployment.getId())).thenReturn(deployment);
    deploymentService.updateDeployment(deployment.getId(), deploymentRequest, null);
    assertThat(deployment.getStatus()).isEqualTo(Status.UPDATE_IN_PROGRESS);
  }

  private static Map<String, NodeTemplate> getNodeTemplates() {
    String nodeName1 = "server1";
    String nodeName2 = "server2";

    Map<String, Capability> capabilities = Maps.newHashMap();

    NodeTemplate nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeTypeCompute);

    Map<String, NodeTemplate> nts = Maps.newHashMap();
    nts.put(nodeName1, nt);

    nt = new NodeTemplate();
    nt.setCapabilities(capabilities);
    nt.setType(nodeTypeCompute);
    nts.put(nodeName2, nt);
    return nts;
  }
}
