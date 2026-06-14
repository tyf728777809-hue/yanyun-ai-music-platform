package com.yanyun.music.creativeagent;

import java.util.Locale;
import java.util.Set;

public final class CreativeBoundaryTerms {

  private static final Set<String> OTHER_IP_TERMS =
      Set.of("高达", "gundam", "原神", "genshin", "星穹", "崩坏", "鸣潮", "王者荣耀", "火影", "海贼王");

  private static final Set<String> YANYUN_TERMS =
      Set.of(
          "燕云", "十六声", "江湖", "武学", "奇术", "门派", "乱世", "家国", "寻声", "侠", "边城", "雁门", "清河", "不羡仙",
          "弱水岸", "若水岸", "九流门");

  private CreativeBoundaryTerms() {}

  public static boolean containsOtherIpTerm(String value) {
    String normalized = normalize(value);
    return OTHER_IP_TERMS.stream()
        .map(CreativeBoundaryTerms::normalize)
        .anyMatch(normalized::contains);
  }

  public static boolean containsYanyunTerm(String value) {
    String normalized = normalize(value);
    return YANYUN_TERMS.stream()
        .map(CreativeBoundaryTerms::normalize)
        .anyMatch(normalized::contains);
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }
}
