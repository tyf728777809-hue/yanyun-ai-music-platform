package com.yanyun.music.prompt;

public final class MockPromptTemplateService implements PromptTemplateService {

  public static final int TEMPLATE_VERSION = 1;

  @Override
  public PromptRenderResult render(PromptRenderRequest request) {
    String prompt =
        """
        template=%s
        operation=%s
        input=%s
        current_lyrics=%s
        instruction=%s
        music_style=%s
        vocal=%s
        references=%s
        """
            .formatted(
                request.templateKey(),
                request.operation(),
                safe(request.userInput()),
                safe(request.currentLyrics()),
                safe(request.instruction()),
                safe(request.musicStyle()),
                safe(request.vocalPreference()),
                request.knowledgeReferences());
    return new PromptRenderResult(request.templateKey(), TEMPLATE_VERSION, prompt);
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }
}
