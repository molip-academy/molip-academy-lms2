package com.back.domain.member.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByToken(String token);

	void deleteByToken(String token);

	void deleteByMember(Member member);
}
