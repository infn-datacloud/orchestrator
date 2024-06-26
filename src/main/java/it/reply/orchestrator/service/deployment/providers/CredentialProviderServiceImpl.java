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

package it.reply.orchestrator.service.deployment.providers;

import it.reply.orchestrator.dto.security.ServiceCredential;
import it.reply.orchestrator.dto.vault.TokenAuthenticationExtended;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.orchestrator.service.IamServiceException;
import it.reply.orchestrator.service.VaultService;
import it.reply.orchestrator.utils.JwtUtils;
import java.net.URI;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CredentialProviderServiceImpl implements CredentialProviderService  {

  @Autowired
  private VaultService vaultService;

  /**
   * Get credentials stored in vault service.
   *
   * @param  serviceId  is CpComputeServiceId of cloud provider
   * @param  accessToken with audience
   * @param  clazz type of return class
   * @return GenericCredential or GenericCredentialWithTenant
   */
  public <T extends ServiceCredential> T credentialProvider(String serviceId,
      String accessToken, Class<T> clazz) {

    if (serviceId == null || serviceId.isEmpty()) {
      LOG.error("ServiceId is empty.");
    }
    Optional<URI> vaultServiceUri = vaultService.getServiceUri();
    if (!vaultServiceUri.isPresent()) {
      throw new DeploymentException("Vault service is not configured. Service uri is not present.");
    }
    TokenAuthenticationExtended vaultToken =
        vaultService.retrieveToken(
            vaultServiceUri.get(),
            accessToken
            );

    String pathVaultComplete = vaultServiceUri.get()
        + "/v1/secret/data/"
        + vaultToken.getEntityId()
        + vaultService.getServicePath()
        + serviceId;

    return vaultService.readSecret(vaultToken, pathVaultComplete, clazz);
  }

  /**
   * Get credentials stored in vault service.
   *
   * @param serviceId is CpComputeServiceId of cloud provider
   * @param accessToken with audience
   * @param userGroup is the user group
   * @return GenericServiceCredential or GenericServiceCredentialWithTenant
   */
  public Map<String, Object> credentialProvider(String serviceId, String userGroup,
      String accessToken) {

    String sub = null;

    try {
      sub = JwtUtils.getJwtClaimsSet(JwtUtils.parseJwt(accessToken)).getStringClaim("sub");
    } catch (ParseException e) {
      String errorMessage = String.format("Sub not found in user's token. %s", e.getMessage());
      LOG.error(errorMessage);
      throw new IamServiceException(errorMessage, e);
    }

    if (serviceId == null || serviceId.isEmpty()) {
      LOG.error("ServiceId is empty.");
    }
    Optional<URI> vaultServiceUri = vaultService.getServiceUri();
    if (!vaultServiceUri.isPresent()) {
      throw new DeploymentException("Vault service is not configured. Service uri is not present.");
    }
    TokenAuthenticationExtended vaultToken =
        vaultService.retrieveToken(vaultServiceUri.get(), accessToken);

    String pathVaultComplete = vaultServiceUri.get() + "/v1/secrets/data/" + sub
        + vaultService.getServicePath() + serviceId + "/" + userGroup;

    return vaultService.readSecret(vaultToken, pathVaultComplete);
  }

}
