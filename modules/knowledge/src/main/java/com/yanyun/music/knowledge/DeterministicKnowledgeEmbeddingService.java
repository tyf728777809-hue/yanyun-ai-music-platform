package com.yanyun.music.knowledge;

import java.text.Normalizer;
import java.util.Locale;

public final class DeterministicKnowledgeEmbeddingService implements KnowledgeEmbeddingService {

  public static final int DIMENSIONS = 64;

  @Override
  public int dimensions() {
    return DIMENSIONS;
  }

  @Override
  public double[] embed(String text) {
    double[] vector = new double[DIMENSIONS];
    String normalized = normalize(text);
    if (normalized.isBlank()) {
      vector[0] = 1.0d;
      return vector;
    }
    normalized.codePoints().forEach(codePoint -> vector[index(codePoint)] += weight(codePoint));
    double norm = 0.0d;
    for (double value : vector) {
      norm += value * value;
    }
    if (norm <= 0.0d) {
      vector[0] = 1.0d;
      return vector;
    }
    double scale = Math.sqrt(norm);
    for (int i = 0; i < vector.length; i++) {
      vector[i] = vector[i] / scale;
    }
    return vector;
  }

  static String normalize(String value) {
    if (value == null) {
      return "";
    }
    return Normalizer.normalize(value, Normalizer.Form.NFKC)
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", "");
  }

  private int index(int codePoint) {
    return Math.floorMod(codePoint * 31 + 17, DIMENSIONS);
  }

  private double weight(int codePoint) {
    return 1.0d + Math.floorMod(codePoint, 7) * 0.03d;
  }
}
