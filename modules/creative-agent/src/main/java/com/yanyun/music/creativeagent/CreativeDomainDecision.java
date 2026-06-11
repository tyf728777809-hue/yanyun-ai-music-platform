package com.yanyun.music.creativeagent;

public enum CreativeDomainDecision {
  PASS,
  REWRITE_TO_YANYUN,
  REJECT;

  public boolean rejected() {
    return this == REJECT;
  }
}
