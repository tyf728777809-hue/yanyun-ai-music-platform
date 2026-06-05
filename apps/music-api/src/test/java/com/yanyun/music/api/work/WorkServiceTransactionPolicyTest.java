package com.yanyun.music.api.work;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

class WorkServiceTransactionPolicyTest {

  @Test
  void idempotentWriteMethodsDoNotConvertBusinessConflictsIntoRollbackFailures() {
    List<String> methodNames =
        List.of(
            "createFromInspiration",
            "createFromLyrics",
            "polishLyrics",
            "continueLyrics",
            "confirmWork",
            "retryMusic",
            "regenerateCover",
            "rerenderVideo",
            "markPublishPackageFetched",
            "refreshPublishPackageUrl");

    methodNames.forEach(
        methodName -> {
          Transactional transactional = method(methodName).getAnnotation(Transactional.class);

          assertThat(transactional).as(methodName).isNotNull();
          assertThat(transactional.noRollbackFor())
              .as(methodName)
              .contains(ResponseStatusException.class);
        });
  }

  private Method method(String methodName) {
    return Arrays.stream(WorkService.class.getDeclaredMethods())
        .filter(method -> method.getName().equals(methodName))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing WorkService method: " + methodName));
  }
}
