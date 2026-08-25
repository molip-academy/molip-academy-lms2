package com.back.domain.member.service;

import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	public Member register(String username, String rawPassword, String nickname) {
		if (memberRepository.existsByUsername(username)) {
			throw new ServiceException(HttpStatus.CONFLICT, "username", "이미 사용 중인 아이디입니다.");
		}
		if (memberRepository.existsByNickname(nickname)) {
			throw new ServiceException(HttpStatus.CONFLICT, "nickname", "이미 사용 중인 닉네임입니다.");
		}
		return memberRepository.save(Member.of(username, passwordEncoder.encode(rawPassword), nickname));
	}

	public Member getById(Long id) {
		return memberRepository.findById(id)
				.orElseThrow(() -> new ServiceException(HttpStatus.UNAUTHORIZED, "회원을 찾을 수 없습니다."));
	}
}
