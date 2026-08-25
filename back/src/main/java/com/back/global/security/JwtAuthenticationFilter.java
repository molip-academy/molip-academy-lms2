package com.back.global.security;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 액세스 토큰 쿠키가 유효하면 인증을 채운다. 없거나 틀리면 그냥 통과시켜 뒤에서 막히게 한다. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;
	private final MemberRepository memberRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			AuthCookies.read(request, AuthCookies.ACCESS_TOKEN)
					.flatMap(jwtProvider::readAccessTokenSubject)
					.flatMap(memberRepository::findById)
					.ifPresent(this::authenticate);
		}

		chain.doFilter(request, response);
	}

	private void authenticate(Member member) {
		var principal = new MemberPrincipal(member.getId(), member.getUsername());
		var authentication = new UsernamePasswordAuthenticationToken(
				principal, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
