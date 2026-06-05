package com.yanyun.music.api.workflow;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yanyun.workflow")
public class WorkflowDispatchProperties {

  private DispatchMode dispatchMode = DispatchMode.SYNC;
  private Outbox outbox = new Outbox();

  public DispatchMode getDispatchMode() {
    return dispatchMode;
  }

  public void setDispatchMode(DispatchMode dispatchMode) {
    this.dispatchMode = dispatchMode == null ? DispatchMode.SYNC : dispatchMode;
  }

  public Outbox getOutbox() {
    return outbox;
  }

  public void setOutbox(Outbox outbox) {
    this.outbox = outbox == null ? new Outbox() : outbox;
  }

  public boolean outboxMode() {
    return dispatchMode == DispatchMode.OUTBOX;
  }

  public enum DispatchMode {
    SYNC,
    OUTBOX
  }

  public enum DispatchTarget {
    LOCAL,
    TEMPORAL
  }

  public static class Outbox {

    private boolean dispatcherEnabled;
    private DispatchTarget dispatchTarget = DispatchTarget.LOCAL;
    private Duration pollInterval = Duration.ofSeconds(5);
    private int batchSize = 5;
    private int maxAttempts = 3;
    private Duration lockTimeout = Duration.ofSeconds(60);
    private Duration retryDelay = Duration.ofSeconds(10);

    public boolean isDispatcherEnabled() {
      return dispatcherEnabled;
    }

    public void setDispatcherEnabled(boolean dispatcherEnabled) {
      this.dispatcherEnabled = dispatcherEnabled;
    }

    public DispatchTarget getDispatchTarget() {
      return dispatchTarget;
    }

    public void setDispatchTarget(DispatchTarget dispatchTarget) {
      this.dispatchTarget = dispatchTarget == null ? DispatchTarget.LOCAL : dispatchTarget;
    }

    public Duration getPollInterval() {
      return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
      this.pollInterval =
          pollInterval == null || pollInterval.isNegative() ? Duration.ofSeconds(5) : pollInterval;
    }

    public int getBatchSize() {
      return batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = Math.max(1, batchSize);
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = Math.max(1, maxAttempts);
    }

    public Duration getLockTimeout() {
      return lockTimeout;
    }

    public void setLockTimeout(Duration lockTimeout) {
      this.lockTimeout =
          lockTimeout == null || lockTimeout.isNegative() ? Duration.ofSeconds(60) : lockTimeout;
    }

    public Duration getRetryDelay() {
      return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
      this.retryDelay =
          retryDelay == null || retryDelay.isNegative() ? Duration.ofSeconds(10) : retryDelay;
    }
  }
}
