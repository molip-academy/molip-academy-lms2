package com.back.global.security;

/** 인증된 회원. 요청 본문이나 경로가 아니라 오직 여기서만 "누구인지"가 나온다. */
public record MemberPrincipal(Long id, String username) {}
