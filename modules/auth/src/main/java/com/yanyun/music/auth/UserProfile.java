package com.yanyun.music.auth;

import java.util.List;

public record UserProfile(String userId, String nickname, String avatarUrl, List<String> roles) {}
