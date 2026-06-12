package com.yanyun.music.dreammaker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DreamMakerFailureMapperTest {

  @Test
  void mapsProviderCreditAndConcurrencyMessagesToAccountLimit() {
    assertEquals(
        DreamMakerFailureMapper.PROVIDER_ACCOUNT_LIMIT,
        DreamMakerFailureMapper.fromProviderError(0, "当前有0任务未扣积分，剩下的积分已不够创作"));
    assertEquals(
        DreamMakerFailureMapper.PROVIDER_ACCOUNT_LIMIT,
        DreamMakerFailureMapper.fromProviderError(0, "超过最大并发任务数50，请等待已创作的完成之后在进行提交"));
    assertEquals(
        DreamMakerFailureMapper.PROVIDER_ACCOUNT_LIMIT,
        DreamMakerFailureMapper.fromProviderError(0, "信用不足"));
    assertEquals(
        DreamMakerFailureMapper.PROVIDER_ACCOUNT_LIMIT,
        DreamMakerFailureMapper.fromProviderError(0, "not enough credits for this account"));
  }
}
