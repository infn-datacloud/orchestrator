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
import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.dto.cmdb.CloudService.VolumeType;
import it.reply.orchestrator.dto.cmdb.ComputeService;
import it.reply.orchestrator.dto.cmdb.Flavor;
import it.reply.orchestrator.dto.cmdb.Image;
import it.reply.orchestrator.dto.dynafed.Dynafed;
import it.reply.orchestrator.dto.onedata.OneData;
import it.reply.orchestrator.dto.policies.ToscaPolicy;
import it.reply.orchestrator.enums.DeploymentProvider;
import it.reply.orchestrator.enums.PrivateNetworkType;
import it.reply.orchestrator.exception.service.ToscaException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jgrapht.graph.DirectedMultigraph;

public interface ToscaService {

  public String getS3ToscaNodeType();

  public String getBucketNameProperty();

  public String getS3UrlProperty();

  public String getEnableVersioningProperty();

  public String getAwsAccessKey();

  public String getAwsSecretKey();

  /**
   * Obtain the string TOSCA template representation from the in-memory representation.
   *
   * @param archiveRoot
   *          the {@link ArchiveRoot} from which serialize the TOSCA template
   * @return the serialized TOSCA template
   */
  public String serialize(ArchiveRoot archiveRoot);

  /**
   * Adds the parameters needed for 'tosca.nodes.indigo.ElasticCluster' nodes (deployment_id,
   * orchestrator_url).
   *
   * @param parsingResult
   *          .
   * @param deploymentId
   *          .
   * @param oauthToken
   *          .
   */
  public void addElasticClusterParameters(ArchiveRoot parsingResult, String deploymentId,
      String oauthToken);

  /**
   * Replace images data in 'tosca.capabilities.indigo.OperatingSystem' capabilities in the TOSCA
   * template with the provider-specific identifier.
   *
   * @param deploymentProvider
   *          the deployment provider.
   * @param parsingResult
   *          the in-memory TOSCA template.
   * @param computeService
   *          the chosen cloud compute service data.
   */
  public void contextualizeAndReplaceImages(ArchiveRoot parsingResult,
      ComputeService computeService, DeploymentProvider deploymentProvider);

  /**
   * Find matches for images data in 'tosca.capabilities.indigo.OperatingSystem' capabilities in the
   * TOSCA template with the provider-specific identifier.
   *
   * @param parsingResult
   *          the in-memory TOSCA template.
   * @param computeService
   *          the chosen cloud compute service data.
   *
   * @return Map of Contextualized images
   */
  public Map<Boolean, Map<NodeTemplate, Image>> contextualizeImages(ArchiveRoot parsingResult,
      ComputeService computeService);

  /**
   * Find matches for volume type in 'tosca.nodes.indigo.BlockStorage' in the
   * TOSCA template with the provider-specific identifier.
   *
   * @param parsingResult
   *          the in-memory TOSCA template.
   * @param computeService
   *          the chosen cloud compute service data.
   *
   * @return Map of Contextualized volume types
   */
  public Map<Boolean, Map<NodeTemplate, VolumeType>> contextualizeVolumeTypes(
      ArchiveRoot parsingResult, ComputeService computeService);

  /**
   * Verifies that all the template's required inputs are present in the user's input list.
   *
   * @param templateInputs
   *          the templates's defined inputs.
   * @param inputs
   *          the user's input list.
   * @throws ToscaException
   *           in case a required input is not present in the user's input or if the user's input
   *           value doesn't match the defined input type.
   */
  public void validateUserInputs(Map<String, PropertyDefinition> templateInputs,
      Map<String, Object> inputs);

  /**
   * Replaces TOSCA input functions with the actual input values (user's input values or default
   * ones).
   *
   * @param archiveRoot
   *          the in-memory TOSCA template.
   * @param inputs
   *          the user's inputs to the template.
   * @throws ToscaException
   *           if the input replacement fails TODO.
   */
  public void replaceInputFunctions(ArchiveRoot archiveRoot, Map<String, Object> inputs);

  /**
   * Parse the TOSCA template (string) and get the in-memory representation.<br>
   * This also checks for validation errors.
   *
   * @param toscaTemplate
   *          the TOSCA template as string.
   * @return an {@link ArchiveRoot} representing the template.
   * @throws ToscaException
   *           if validation errors occur.
   */
  public ArchiveRoot parse(String toscaTemplate);

  /**
   * As for {@link #parse(String)} but also validates user's inputs.
   *
   * @param toscaTemplate
   *          the TOSCA template as string.
   * @param inputs
   *          the input parameters
   * @return an {@link ArchiveRoot} representing the template.
   * @throws ToscaException
   *           if validation errors occur.
   */
  public ArchiveRoot parseAndValidateTemplate(String toscaTemplate, Map<String, Object> inputs);

  /**
   * As for {@link #parseAndValidateTemplate(String, Map)} but also replaces the user's inputs.
   *
   * @param toscaTemplate
   *          the TOSCA template as string.
   * @param inputs
   *          the inputs
   * @return an {@link ArchiveRoot} representing the template.
   * @throws ToscaException
   *           if validation errors occur.
   */
  public ArchiveRoot prepareTemplate(String toscaTemplate, Map<String, Object> inputs);

  public Optional<AbstractPropertyValue> getNodePropertyByName(NodeTemplate node,
      String propertyName);

  public Optional<Capability> getNodeCapabilityByName(NodeTemplate node, String propertyName);

  public Optional<DeploymentArtifact> getNodeArtifactByName(NodeTemplate node,
      String artifactName);

  public List<RelationshipTemplate> getRelationshipTemplatesByCapabilityName(
      Map<String, RelationshipTemplate> relationships, String capabilityName);

  /**
   * Finds all the nodes associated to the given {@link NodeTemplate} with a capability with the
   * given name.
   *
   * @param nodes
   *          the template's node map.
   * @param nodeTemplate
   *          the origin node.
   * @param capabilityName
   *          the name of the capability.
   * @return a map with the nodes (and their names) associated to the origin node with given
   *         capability.
   */
  public Map<String, NodeTemplate> getAssociatedNodesByCapability(Map<String, NodeTemplate> nodes,
      NodeTemplate nodeTemplate, String capabilityName);

  public Collection<NodeTemplate> getScalableNodes(ArchiveRoot archiveRoot);

  public Optional<Long> getCount(NodeTemplate nodeTemplate);

  /**
   * Get the list of resources to be removed.
   *
   * @param nodeTemplate
   *          {@link NodeTemplate}
   * @return the list of resources to be removed or an empty list
   */
  public List<String> getRemovalList(NodeTemplate nodeTemplate);

  public String updateTemplate(String template);

  // public String updateCount(ArchiveRoot archiveRoot, int count) throws IOException;

  /**
   * Extracts OneData requirements (i.e. space, favorite providers, etc) from the TOSCA template.
   *
   * @param archiveRoot
   *          an {@link ArchiveRoot} representing the template.
   * @param inputs
   *          the user's input list.
   * @return a Map of {@link OneData} requirement, index by node name.<br>
   *         <b>WARNING:</b> (TEMPORARY) currently OneData nodes are not supported; thus the name
   *         used are hard-coded and are either 'input', 'output' or 'service'.
   */
  public Map<String, OneData> extractOneDataRequirements(ArchiveRoot archiveRoot,
      Map<String, Object> inputs);

  public Map<String, Dynafed> extractDyanfedRequirements(ArchiveRoot archiveRoot,
      Map<String, Object> inputs);

  /**
   * Extracts the placement policies from the TOSCA template.
   *
   * @param archiveRoot
   *          an {@link ArchiveRoot} representing the template.
   * @return the list of placementPolicies
   */
  public @NonNull Map<String, ToscaPolicy> extractPlacementPolicies(ArchiveRoot archiveRoot);

  public DirectedMultigraph<NodeTemplate, RelationshipTemplate> buildNodeGraph(
      Map<String, NodeTemplate> nodes, boolean checkForCycles);

  public PrivateNetworkType getPrivateNetworkType(ArchiveRoot archiveRoot);

  public boolean isHybridDeployment(ArchiveRoot archiveRoot);

  public boolean isElasticClusterDeployment(ArchiveRoot archiveRoot);

  public boolean isMesosGpuRequired(ArchiveRoot archiveRoot);

  public Collection<NodeTemplate> getNodesOfType(ArchiveRoot archiveRoot, String type);

  public Collection<NodeTemplate> getNodesLikeType(ArchiveRoot archiveRoot, String regexType);

  public Map<NodeTemplate, Image> extractImageRequirements(ArchiveRoot parsingResult);

  public Map<NodeTemplate, VolumeType> extractVolumeRequirements(ArchiveRoot parsingResult);

  boolean isLikeToscaType(NodeTemplate node, String regexNodeType);

  boolean isOfToscaType(NodeTemplate node, String nodeType);

  boolean isOfToscaType(Resource resource, String nodeType);

  void removeRemovalList(NodeTemplate node);

  boolean isScalable(NodeTemplate nodeTemplate);

  public ArchiveRoot setDeploymentTags(ArchiveRoot ar, String orchestratorUrl,
      String deplymentId, String userEmail, String preferredUsername);

  public Map<String, Map<String, String>> getS3Properties(ArchiveRoot ar);

  public ArchiveRoot setDeploymentClientIam(ArchiveRoot ar,
      Map<String,Map<String,String>> iamTemplateOutput);

  public ArchiveRoot setDeploymentS3Buckets(ArchiveRoot ar,
      Map<String, Map<String, String>> s3TemplateOutput);

  public Map<String,Map<String,String>> getIamProperties(ArchiveRoot ar);

  public ArchiveRoot setHybridDeployment(ArchiveRoot ar, String publicNetworkName,
      String privateNetworkName, String privateNetworkCidr);

  public ArchiveRoot setHybridUpdateDeployment(ArchiveRoot ar,
      boolean newResourcesOnDifferentService,
      String publicNetworkName, String privateNetworkName, String privateNetworkCidr);

  public ArchiveRoot setNetworkNames(ArchiveRoot ar,
      String publicNetworkName,
      String privateNetworkName,
      String privateNetworkProxyHost,
      String privateNetworkProxyUser);

  public Map<NodeTemplate, Flavor> extractFlavorRequirements(ArchiveRoot parsingResult);

  public Map<Boolean, Map<NodeTemplate, Flavor>> contextualizeFlavors(ArchiveRoot parsingResult,
      ComputeService computeService);

  public void contextualizeAndReplaceFlavors(ArchiveRoot parsingResult,
      ComputeService computeService,
      DeploymentProvider deploymentProvider);

  public void contextualizeAndReplaceVolumeTypes(ArchiveRoot parsingResult,
      ComputeService computeService,
      DeploymentProvider deploymentProvider);

}
