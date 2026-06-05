package com.yanyun.music.storage;

import java.time.Duration;

public final class ObjectStorageProperties {

  private String provider = "local";
  private String environment = "local";
  private String localRootDirectory = "build/local-object-storage/yanyun-works-local";
  private String publicBaseUrl = "http://localhost:9000/yanyun-works-local";
  private String endpoint = "http://localhost:9000";
  private String publicEndpoint;
  private String region = "us-east-1";
  private String bucket = "yanyun-works-local";
  private String accessKey = "";
  private String secretKey = "";
  private boolean pathStyleEnabled = true;
  private boolean autoCreateBucket = true;
  private Duration urlTtl = Duration.ofHours(24);

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getLocalRootDirectory() {
    return localRootDirectory;
  }

  public void setLocalRootDirectory(String localRootDirectory) {
    this.localRootDirectory = localRootDirectory;
  }

  public String getPublicBaseUrl() {
    return publicBaseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getPublicEndpoint() {
    return publicEndpoint;
  }

  public void setPublicEndpoint(String publicEndpoint) {
    this.publicEndpoint = publicEndpoint;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public boolean isPathStyleEnabled() {
    return pathStyleEnabled;
  }

  public void setPathStyleEnabled(boolean pathStyleEnabled) {
    this.pathStyleEnabled = pathStyleEnabled;
  }

  public boolean isAutoCreateBucket() {
    return autoCreateBucket;
  }

  public void setAutoCreateBucket(boolean autoCreateBucket) {
    this.autoCreateBucket = autoCreateBucket;
  }

  public Duration getUrlTtl() {
    return urlTtl;
  }

  public void setUrlTtl(Duration urlTtl) {
    this.urlTtl = urlTtl;
  }
}
