package com.back.global.security;

import com.back.global.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

	private final AppProperties properties;

	public SecurityConfig(AppProperties properties) {
		this.properties = properties;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
			throws Exception {

		var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfRepository.setCookieCustomizer(cookie -> {
			cookie.sameSite(properties.cookie().sameSite())
					.secure(properties.cookie().secure())
					.path("/");
			// 프론트의 JS 가 이 쿠키를 읽어 X-XSRF-TOKEN 헤더로 되돌려준다.
			// 서브도메인이 갈리면 상위 도메인을 지정해야 읽을 수 있다.
			var domain = properties.cookie().domain();
			if (domain != null && !domain.isBlank()) {
				cookie.domain(domain);
			}
		});

		// SPA는 쿠키에서 읽은 값을 그대로 헤더에 실으므로 BREACH 마스킹을 쓰지 않는다.
		var csrfHandler = new CsrfTokenRequestAttributeHandler();

		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfRepository)
						.csrfTokenRequestHandler(csrfHandler)
						.ignoringRequestMatchers("/h2-console/**"))
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/h2-console/**").permitAll()
						// 상태 확인은 인증 없이 열어 둔다. 배포 파이프라인과 사람이 둘 다 본다.
						// 노출되는 엔드포인트는 health 하나뿐이고 상세는 감춰져 있다.
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
						// API 문서 경로. springdoc이 꺼진 프로파일에서는 핸들러 자체가 등록되지 않아
						// 이 허용이 있어도 문서가 나가지 않는다(포워딩된 /error가 401을 낸다).
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/csrf").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(new CsrfCookieFilter(), JwtAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		var configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.cors().allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		// 브라우저가 쿠키를 실어 보내야 하므로 자격증명을 허용한다. 와일드카드 오리진은 이때 쓸 수 없다.
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	/** CsrfToken은 지연 로딩이라 실제로 읽어야 XSRF-TOKEN 쿠키가 내려간다. */
	static class CsrfCookieFilter extends OncePerRequestFilter {
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
				throws ServletException, IOException {
			CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (token != null) {
				token.getToken();
			}
			chain.doFilter(request, response);
		}
	}
}
