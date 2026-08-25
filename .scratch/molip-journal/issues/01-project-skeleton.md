# 01: 프로젝트 골격과 실행 확인

**What to build:** 백엔드가 개발 모드로 실행되고, 테스트 모드로 스프링 컨텍스트가 뜬다. 아직 도메인은 없다. 이 티켓의 목적은 이후 모든 티켓이 딛고 설 실행 환경과 설정 분리를 세우는 것이다.

특히 **`.gitignore`를 `.env`보다 먼저** 만들어야 한다. 이 저장소는 공개돼 있어 순서가 뒤바뀌면 비밀값이 이력에 남는다.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `back`에 Spring Boot 4.x · JDK 25 · Gradle Kotlin DSL 프로젝트가 있고, 루트 패키지는 `com.back`, 메인 클래스는 `com.back.BackApplication`이다
- [ ] DevTools, Lombok, Spring Data JPA, Validation, Spring Security, H2 의존성이 들어 있다
- [ ] `.gitignore`가 `.env`, `db_dev.mv.db`, 빌드 산출물, `node_modules`를 무시하며, `.env`보다 먼저 커밋된다
- [ ] 비밀값은 `back/.env`에서 `spring.config.import: optional:file:.env[.properties]`로 읽힌다
- [ ] `.env`가 없어도 앱이 뜬다 (개발·테스트 기본값이 프로퍼티에 있다)
- [ ] 개발 모드는 `application.yml` + `application-dev.yml`을 쓰고, 파일 H2(`./db_dev.mv.db`)에 `ddl-auto: update`이며, h2-console에 접속된다
- [ ] 테스트 모드는 `application.yml` + `application-test.yml`을 쓰고, 인메모리 H2에 `ddl-auto: create`다
- [ ] OSIV가 꺼져 있다
- [ ] `@EnableJpaAuditing`이 켜져 있다
- [ ] 테스트 프로파일에서 스프링 컨텍스트가 로드되는 테스트가 초록이다
