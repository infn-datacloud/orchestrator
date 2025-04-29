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
import it.reply.orchestrator.service.deployment.providers.CredentialProviderService;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteMarkerEntry;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable;

@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

  @Autowired
  private CredentialProviderService credProvServ;

  private static final String S3_TOSCA_NODE_TYPE = "tosca.nodes.indigo.S3Bucket";
  private static final String BUCKET_NAME_PROPERTY = "bucket_name";
  private static final String S3_URL_PROPERTY = "s3_url";
  private static final String AWS_ACCESS_KEY = "aws_access_key";
  private static final String AWS_SECRET_KEY = "aws_secret_key";
  private static final String AWS_REGION = "us-east-1";

  /**
   * Enable bucket versioning.
   *
   * @param s3Client the S3 Client
   * @param bucketName the name of the bucket to create
   * @throws S3ServiceException when fails to enable bucket versioning
   */
  public void enableBucketVersioning(S3Client s3Client, String bucketName)
      throws S3ServiceException {
    try {
      PutBucketVersioningRequest request = PutBucketVersioningRequest.builder().bucket(bucketName)
          .versioningConfiguration(conf -> conf.status(BucketVersioningStatus.ENABLED)).build();
      s3Client.putBucketVersioning(request);
      GetBucketVersioningResponse response =
          s3Client.getBucketVersioning(builder -> builder.bucket(bucketName));
      if (response.status().equals(BucketVersioningStatus.ENABLED)) {
        LOG.info("Versioning enabled for bucket " + bucketName);
      } else {
        String errorMessage =
            String.format("Failure to enable versioning for bucket %s", bucketName);
        LOG.error(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    } catch (RuntimeException e) {
      String errorMessage = String.format(
          "Failure in the creation of bucket with bucket name %s. %s", bucketName, e.getMessage());
      LOG.error(errorMessage);
      throw new S3ServiceException(errorMessage, e);
    }
  }

  /**
   * Create a bucket.
   *
   * @param s3Client the S3 Client
   * @param bucketName the name of the bucket to create
   * @throws S3ServiceException when fails to create the bucket
   */
  private static void createBucket(S3Client s3Client, String bucketName) throws S3ServiceException {
    try {
      CreateBucketRequest createBucketRequest =
          CreateBucketRequest.builder().bucket(bucketName).build();
      s3Client.createBucket(createBucketRequest);
      LOG.info("Bucket successfully created: {}", bucketName);
    } catch (RuntimeException e) {
      String errorMessage = String.format(
          "Failure in the creation of bucket with bucket name %s. %s", bucketName, e.getMessage());
      LOG.error(errorMessage);
      throw new S3ServiceException(errorMessage, e);
    }
  }

  /**
   * Delete a bucket.
   *
   * @param s3Client the S3 Client
   * @param bucketName the name of the bucket to delete
   * @throws S3ServiceException when fails to delete the bucket
   */
  public static void deleteBucket(S3Client s3Client, String bucketName) throws S3ServiceException {
    LOG.info("Deleting bucket with name {}", bucketName);
    try {

      ListObjectsV2Request listObjectsV2Request =
          ListObjectsV2Request.builder().bucket(bucketName).build();
      ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
      List<S3Object> listObjects = listObjectsV2Response.contents();

      if (!listObjects.isEmpty()) {
        List<ObjectIdentifier> objectsToDelete = new ArrayList<>();
        for (S3Object s3Object : listObjects) {
          objectsToDelete.add(ObjectIdentifier.builder().key(s3Object.key()).build());
        }

        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
            .bucket(bucketName).delete(Delete.builder().objects(objectsToDelete).build()).build();

        s3Client.deleteObjects(deleteObjectsRequest);
        LOG.info("Objects deleted from bucket {}", bucketName);
      }

      ListObjectVersionsRequest listObjectVersionsRequest =
          ListObjectVersionsRequest.builder().bucket(bucketName).build();
      ListObjectVersionsIterable listObjectVersionsIterable =
          s3Client.listObjectVersionsPaginator(listObjectVersionsRequest);

      for (ListObjectVersionsResponse listObjectVersionsResponse : listObjectVersionsIterable) {
        List<ObjectIdentifier> versionsToDelete = new ArrayList<>();
        for (ObjectVersion objectVersion : listObjectVersionsResponse.versions()) {
          versionsToDelete.add(ObjectIdentifier.builder().key(objectVersion.key())
              .versionId(objectVersion.versionId()).build());
        }

        for (DeleteMarkerEntry deleteMarkerEntry : listObjectVersionsResponse.deleteMarkers()) {
          versionsToDelete.add(ObjectIdentifier.builder().key(deleteMarkerEntry.key())
              .versionId(deleteMarkerEntry.versionId()).build());
        }

        if (!versionsToDelete.isEmpty()) {
          DeleteObjectsRequest deleteObjectsRequest =
              DeleteObjectsRequest.builder().bucket(bucketName)
                  .delete(Delete.builder().objects(versionsToDelete).build()).build();

          s3Client.deleteObjects(deleteObjectsRequest);
          LOG.info("Object versions and delete markers deleted from bucket {}", bucketName);
        }
      }

      DeleteBucketRequest deleteBucketRequest =
          DeleteBucketRequest.builder().bucket(bucketName).build();
      s3Client.deleteBucket(deleteBucketRequest);
      LOG.info("Bucket {} successfully deleted", bucketName);
    } catch (NoSuchBucketException e) {
      LOG.warn("Bucket {} was not found", bucketName);
    } catch (RuntimeException e) {
      String errorMessage = String.format(
          "Failure in the deletion of bucket with bucket name %s. %s", bucketName, e.getMessage());
      LOG.error(errorMessage);
      throw new S3ServiceException(errorMessage, e);
    }
  }

  /**
   * Delete all buckets.
   *
   * @param resources object used to make HTTP requests
   * @param accessToken the identity provider
   * @throws S3ServiceException when fails to delete a bucket
   */
  public void deleteAllBuckets(Map<Boolean, Set<Resource>> resources, String userGroup,
      String accessToken, Boolean force) throws S3ServiceException {
    if (Boolean.TRUE.equals(force)) {
      LOG.info("Skipping deletion of S3 buckets");
      return;
    }
    for (Resource resource : resources.get(false)) {
      if (resource.getToscaNodeType().equals(S3_TOSCA_NODE_TYPE)) {
        Map<String, String> resourceMetadata = resource.getMetadata();
        if (resourceMetadata != null && resourceMetadata.containsKey(BUCKET_NAME_PROPERTY)
            && resourceMetadata.containsKey(S3_URL_PROPERTY)) {
          String bucketName = resourceMetadata.get(BUCKET_NAME_PROPERTY);
          String s3Url = resourceMetadata.get(S3_URL_PROPERTY);
          Map<String, Object> result = setupS3Client(s3Url, userGroup, accessToken);
          S3Client s3Client = (S3Client) result.get("s3Client");
          // Delete the bucket
          deleteBucket(s3Client, bucketName);
        } else {
          LOG.info("Found node of type {} but no bucket info is registered in metadata",
              S3_TOSCA_NODE_TYPE);
        }
      }
    }
  }

  /**
   * Create the S3 Client object.
   *
   * @param s3Url the S3 URL
   * @param accessToken the user's accessToken
   * @return the s3Result map containing the s3Client, the accessKeyId, and secretKey
   * @throws S3ServiceException when fails to create an S3Client object
   */
  private Map<String, Object> setupS3Client(String s3Url, String userGroup, String accessToken)
      throws S3ServiceException {
    S3Client s3Client = null;
    String accessKeyId = null;
    String secretKey = null;

    // Read credentials from Vault
    try {
      Map<String, Object> vaultOutput =
          credProvServ.credentialProvider(s3Url.split("//")[1], userGroup, accessToken);
      @SuppressWarnings("unchecked")
      Map<String, String> s3Credentials = (Map<String, String>) vaultOutput.get("data");
      accessKeyId = s3Credentials.get(AWS_ACCESS_KEY);
      secretKey = s3Credentials.get(AWS_SECRET_KEY);
    } catch (RuntimeException e) {
      String errorMessage =
          String.format("Cannot access and get credentials from Vault. %s", e.getMessage());
      LOG.error(errorMessage);
      throw new S3ServiceException(errorMessage, e);
    }

    // Configure S3 client with credentials
    try {
      s3Client = S3Client.builder().endpointOverride(URI.create(s3Url))
          .region(Region.of(AWS_REGION)).forcePathStyle(true)
          .credentialsProvider(
              StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey)))
          .build();
    } catch (RuntimeException e) {
      String errorMessage = String.format(
          "Cannot create an S3Client using the credentials read from Vault. %s", e.getMessage());
      LOG.error(errorMessage);
      throw new S3ServiceException(errorMessage, e);
    }
    Map<String, Object> s3Result = new HashMap<>();
    s3Result.put("s3Client", s3Client);
    s3Result.put("accessKeyId", accessKeyId);
    s3Result.put("secretKey", secretKey);
    return s3Result;
  }

  /**
   * Manage the creation of a bucket.
   *
   * @param bucketName the name of the bucket to create
   * @param accessToken the user's accessToken
   * @return the s3Result map containing the s3Client, the accessKeyId, and secretKey
   * @throws S3ServiceException when fails to create a bucket
   */
  public Map<String, Object> manageBucketCreation(String bucketName, String s3Url, String userGroup,
      String accessToken) throws S3ServiceException {
    // Try to create an S3Client
    Map<String, Object> s3Result = setupS3Client(s3Url, userGroup, accessToken);
    S3Client s3Client = (S3Client) s3Result.get("s3Client");
    // Try to create a bucket
    createBucket(s3Client, bucketName);
    return s3Result;
  }

  /**
   * Check if the bucket name satisfies the AWS bucket naming rules.
   *
   * @param bucketName the name of the bucket to create
   * @return true if the rules are satisfied
   */
  public Boolean checkBucketName(String bucketName) {
    String regex = "^(?!.*\\.\\.)[a-z0-9.-]{1,62}[a-z0-9](?<!-s3alias)(?<!--ol-s3)$";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(bucketName);
    return matcher.matches();
  }
}
