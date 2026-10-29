# SafeMedi Backend

Kotlin 2.2 · Spring Boot 4 · Spring Security · Spring Data JPA · Flyway · MySQL.

## 기본 규칙

- 모든 설명, 리뷰 코멘트, 커밋 메시지는 한국어로 작성한다.
- 코드 식별자, 파일 경로, 명령어, 에러 메시지는 원문을 유지한다.
- 요청 범위 밖의 리팩터링, 포맷 변경, 파일 이동은 하지 않는다.
- 작업 전 `git status --short`로 사용자 변경사항을 확인하고, 직접 수정하지 않은 변경은 되돌리지 않는다.
- `.env`, 인증 키, JWT secret, Firebase credential, DB 접속 정보 등 비밀값은 커밋하지 않는다.
- 로그와 예외 응답에 토큰, 개인정보, 민감한 의료 정보가 노출되지 않게 한다.

## 프로젝트 구조

- 루트 패키지: `com.safemedi.app.sefemedi`
- 도메인 코드는 `src/main/kotlin/com/safemedi/app/sefemedi/domain/{domain}/` 아래에 둔다.
- 공통 설정, 예외, JWT, 공통 엔티티는 `global/` 아래에 둔다.
- 도메인 내부는 기존 구조를 따른다.
  - `controller/`: HTTP API 진입점
  - `service/`: 유스케이스와 트랜잭션 경계
  - `entity/`: JPA 엔티티
  - `repository/`: Spring Data JPA repository
  - `dto/`: request/response/command DTO
  - 특수 로직은 기존처럼 `analyzer/`, `fcm/` 등 의미 있는 하위 패키지를 사용한다.
- 테스트는 대상 코드와 같은 패키지 경로로 `src/test/kotlin/...`에 둔다.

## Kotlin & Spring 규칙

- 생성자 주입을 기본으로 한다. 필드 주입은 사용하지 않는다.
- DTO는 `data class`를 우선 사용하고, request/response 이름을 명확히 구분한다.
- API 응답 형태는 기존 `{Feature}Response` 패턴을 따른다.
- 비즈니스 예외는 `BusinessException`과 `ErrorCode`를 사용하고, 임의의 `RuntimeException` 노출을 피한다.
- 컨트롤러는 요청 검증과 서비스 호출에 집중하고, 복잡한 비즈니스 로직은 서비스로 이동한다.
- 조회 메서드는 `@Transactional(readOnly = true)`, 변경 메서드는 `@Transactional`을 명시한다.
- nullable 값은 Kotlin 타입으로 명시하고, `!!`는 불가피한 경우가 아니면 사용하지 않는다.
- 시간 처리는 `TimeConfig`와 기존 `Clock`/auditing 패턴을 우선 확인한다.

## JPA & DB 규칙

- 운영 스키마 변경은 Flyway migration으로 반영한다.
- migration 파일은 `src/main/resources/db/migration/V{number}__{description}.sql` 형식을 따른다.
- 이미 적용된 migration 파일은 수정하지 않는다. 새 변경은 다음 버전 파일로 추가한다.
- `application.yml`의 JPA 설정은 `ddl-auto: validate` 기준이다. 엔티티 변경 시 migration을 함께 고려한다.
- `open-in-view: false`이므로 필요한 연관 데이터는 서비스 트랜잭션 안에서 조회한다.
- N+1 가능성이 있는 조회는 repository 쿼리, fetch join, `@EntityGraph` 등 기존 패턴을 우선 검토한다.
- 삭제 정책은 기존 도메인의 soft delete/deletedAt 패턴을 먼저 확인하고 맞춘다.

## API & 보안

- 인증이 필요한 API는 Spring Security/JWT 흐름과 `SecurityConfig`의 기존 규칙을 따른다.
- 테스트 로그인 기능은 `app.test-login.enabled` 설정을 기준으로 하며, 운영 기본값은 비활성화다.
- Firebase/FCM 관련 기능은 `firebase.enabled`와 outbox 설정을 고려한다.
- Swagger 설정은 `global/config/SwaggerConfig.kt`를 따른다.
- 새 API를 추가하면 컨트롤러 테스트 또는 서비스 테스트 중 위험도에 맞는 최소 테스트를 추가한다.

## 테스트

- 테스트 러너는 JUnit 5다.
- 서비스 테스트는 가능한 한 도메인 로직과 예외 분기를 직접 검증한다.
- 컨트롤러 테스트는 HTTP status, 응답 DTO, 인증/예외 분기를 검증한다.
- repository 테스트는 JPA 매핑, 쿼리, soft delete 조건처럼 DB 동작이 중요한 경우에 작성한다.
- 테스트 이름은 한국어 설명을 선호한다.
- 버그 수정 시 재발 방지에 필요한 가장 좁은 테스트를 추가한다.

## 명령어

| 작업 | 명령어 |
|------|--------|
| 전체 테스트 | `./gradlew test` |
| 단일 테스트 클래스 | `./gradlew test --tests "com.safemedi.app.sefemedi...ClassName"` |
| 빌드 | `./gradlew build` |
| 애플리케이션 실행 | `./gradlew bootRun` |

## 리뷰 기준

리뷰할 때는 반드시 버그, 보안, 데이터 정합성, 트랜잭션 경계, migration 누락, 테스트 누락을 우선 확인한다. 포맷, 취향성 네이밍, 사소한 정리 제안은 blocking 이슈가 아니면 생략한다.

반드시 지적할 항목:

- 비밀값, 토큰, 개인정보, 의료 정보가 코드나 로그에 노출됨
- 엔티티 변경이 있는데 Flyway migration이 없음
- 이미 적용된 migration을 수정함
- 인증이 필요한 API가 security/JWT 규칙을 우회함
- 변경 작업에 `@Transactional`이 없거나 조회 작업이 불필요하게 write transaction으로 동작함
- `open-in-view: false`에서 lazy loading 예외가 발생할 수 있는 응답 매핑
- 예외가 `GlobalExceptionHandler`/`ErrorCode` 체계를 우회해 클라이언트에 불안정하게 노출됨
- 요청 범위 밖의 대규모 리팩터링이 섞임

## Git 규칙

- 브랜치와 PR 흐름은 `dev`를 개발 기준 브랜치로 본다.
- `main` 직접 push/merge는 금지하고 PR로만 반영한다.
- 커밋 요청을 받으면 직접 관여한 파일만 스테이징한다.
- 기능 단위(예: 도메인 로직 → 저장 계층 → 인증 → UI)로 나눠 커밋하고,
  서로 무관한 변경을 하나의 커밋에 섞지 않는다. 메시지는
  `<type>: <한 줄 요약>` 제목 형식을 따르고, type은 `feat` / `fix` /
  `refactor` / `docs` / `chore` 중에서 고른다. **항상 축약형으로 작성**
- PR 제목은 티켓이 있으면 반드시 `[SAF-00] feat: ...` 형식을 따른다.
- PR 본문은 반드시 `.github/PULL_REQUEST_TEMPLATE.md` 형식을 따른다.