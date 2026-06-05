package com.yanyun.music.storage;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public final class S3ObjectStorageClient implements ObjectStorageClient {

  private final String bucket;
  private final boolean autoCreateBucket;
  private final Duration urlTtl;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final AtomicBoolean bucketChecked = new AtomicBoolean(false);

  public S3ObjectStorageClient(ObjectStorageProperties properties) {
    if (properties == null) {
      throw new IllegalArgumentException("properties is required");
    }
    this.bucket = requireNonBlank(properties.getBucket(), "bucket");
    String accessKey = requireNonBlank(properties.getAccessKey(), "accessKey");
    String secretKey = requireNonBlank(properties.getSecretKey(), "secretKey");
    this.autoCreateBucket = properties.isAutoCreateBucket();
    this.urlTtl = validTtl(properties.getUrlTtl());

    Region region = Region.of(requireNonBlank(properties.getRegion(), "region"));
    StaticCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    S3Configuration s3Configuration =
        S3Configuration.builder().pathStyleAccessEnabled(properties.isPathStyleEnabled()).build();
    URI clientEndpoint = URI.create(requireNonBlank(properties.getEndpoint(), "endpoint"));
    URI presignEndpoint =
        URI.create(firstNonBlank(properties.getPublicEndpoint(), properties.getEndpoint()));

    this.s3Client =
        S3Client.builder()
            .credentialsProvider(credentialsProvider)
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .region(region)
            .endpointOverride(clientEndpoint)
            .serviceConfiguration(s3Configuration)
            .build();
    this.s3Presigner =
        S3Presigner.builder()
            .credentialsProvider(credentialsProvider)
            .region(region)
            .endpointOverride(presignEndpoint)
            .serviceConfiguration(s3Configuration)
            .build();
  }

  @Override
  public StoredObject putObject(ObjectStoragePutRequest request) {
    ensureBucket();
    String objectKey = ObjectStorageKeys.requireSafeObjectKey(request.objectKey());
    byte[] content = request.content();
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(request.contentType())
            .contentLength((long) content.length)
            .build();
    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
    return new StoredObject(
        objectKey, createDownloadUrl(objectKey).url(), request.contentType(), content.length);
  }

  @Override
  public ObjectStorageDownloadUrl createDownloadUrl(String objectKey) {
    String safeObjectKey = ObjectStorageKeys.requireSafeObjectKey(objectKey);
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucket).key(safeObjectKey).build();
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(urlTtl)
            .getObjectRequest(getObjectRequest)
            .build();
    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return new ObjectStorageDownloadUrl(
        safeObjectKey, presignedRequest.url().toString(), OffsetDateTime.now().plus(urlTtl));
  }

  private void ensureBucket() {
    if (!autoCreateBucket || bucketChecked.get()) {
      return;
    }
    if (bucketChecked.compareAndSet(false, true)) {
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      } catch (S3Exception exception) {
        if (exception.statusCode() != 404) {
          throw exception;
        }
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
      }
    }
  }

  private Duration validTtl(Duration ttl) {
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("urlTtl must be positive");
    }
    return ttl;
  }

  private String requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    return value.trim();
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? requireNonBlank(fallback, "endpoint") : value.trim();
  }
}
