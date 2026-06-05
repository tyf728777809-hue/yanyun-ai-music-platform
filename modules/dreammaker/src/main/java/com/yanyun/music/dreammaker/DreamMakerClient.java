package com.yanyun.music.dreammaker;

public interface DreamMakerClient {

  DreamMakerSubmitResponse submit(DreamMakerRunRequest request);

  DreamMakerStatusResponse status(DreamMakerStatusRequest request);
}
