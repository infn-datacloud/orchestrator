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

import it.reply.orchestrator.dal.entity.Resource;
import it.reply.orchestrator.exception.service.S3ServiceException;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.services.s3.S3Client;

public interface S3Service {

  public void enableBucketVersioning(S3Client s3Client, String bucketName)
      throws S3ServiceException;

  public void deleteAllBuckets(
      Map<Boolean, Set<Resource>> resources, String userGroup, String accessToken, Boolean force)
      throws S3ServiceException;

  public Map<String, Object> manageBucketCreation(
      String bucketName, String s3Url, String userGroup, String accessToken)
      throws S3ServiceException;

  public Boolean checkBucketName(String bucketName);
}
