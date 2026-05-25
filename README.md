![CI](https://github.com/mi8524835-beep/notification-system/actions/workflows/ci.yml/badge.svg)

# Notification System

Spring Boot 기반 이벤트 알림 시스템입니다.

이벤트 발생 시 알림 요청을 저장하고, 비동기 처리 / 재시도 / 중복 방지 / 운영 모니터링 / 인증 보안을 고려하여 구현했습니다.

---

## 주요 기능

- 알림 생성 / 조회
- 비동기 알림 처리
- 재시도 정책 + 지수 백오프
- 예약 발송
- 읽음 처리
- 중복 발송 방지
- PostgreSQL 영속 저장
- 낙관적 락 기반 동시성 대응
- 장기 PROCESSING 상태 복구
- 관리자 실패 알림 대시보드
- Spring Security 기반 관리자 보호
- BCrypt 비밀번호 암호화
- JWT 토큰 발급
- JWT 인증 필터
- Swagger API 문서화
- GitHub Actions CI 자동 테스트

---

## Dashboard

운영자가 실패 알림 상태를 확인할 수 있는 모니터링 화면입니다.

![dashboard](docs/dashboard.png)

제공 기능:

- FAILED 상태 모니터링
- 상태별 통계 카드
- 실패 원인 확인
- JSON 조회 링크
- 로그아웃 버튼

---

## Swagger API

API 명세와 요청 테스트를 Swagger UI에서 확인할 수 있습니다.

![swagger](docs/swagger.png)

```text
http://localhost:8080/swagger-ui/index.html
```

---

## Security

관리자 대시보드와 API 보호를 위해 Spring Security를 적용했습니다.

구현 내용:

- BCryptPasswordEncoder 기반 비밀번호 암호화
- ADMIN 권한 기반 관리자 페이지 접근 제어
- JWT 토큰 발급 API
- JWT 인증 필터
- Bearer Token 기반 보호 API 접근

JWT 발급:

```http
POST /auth/token
```

Request:

```json
{
  "username": "admin"
}
```

Response:

```text
eyJhbGciOiJIUzI1NiJ9...
```

보호 API 요청 예시:

```http
Authorization: Bearer {token}
```

---

## CI

GitHub Actions 기반 자동 테스트를 적용했습니다.

동작 흐름:

```text
Push
↓
GitHub Actions 실행
↓
PostgreSQL 서비스 컨테이너 실행
↓
Gradle Test
↓
성공 여부 검증
```

---

## 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Security
- JWT
- BCrypt
- PostgreSQL
- Thymeleaf
- Swagger / Springdoc OpenAPI
- JUnit5
- AssertJ
- Gradle
- GitHub Actions

---

## 실행 방법

실행:

```bash
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

관리자 화면:

```text
http://localhost:8080/admin/dashboard
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

---

## 아키텍처 흐름

```text
API 요청
↓
REQUESTED 저장
↓
Processor 처리
↓
PROCESSING
↓
SENT / FAILED
↓
Retry / Recovery
```

---

## 알림 상태

```text
REQUESTED
PROCESSING
SENT
FAILED
```

---

## 핵심 설계

### 요청 저장과 처리 분리

알림 요청을 즉시 발송하지 않고 DB에 먼저 저장합니다.

목적:

- 비즈니스 트랜잭션 보호
- 장애 격리
- 재시도 가능
- 운영 추적 가능

---

### 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용:

- Service 중복 체크
- DB Unique Constraint
- 테스트 검증

---

### 실패 재시도 정책

실패 시:

```text
retryCount 증가
failureReason 저장
nextRetryAt 계산
```

재시도 간격:

```text
1회 실패 → 5초
2회 실패 → 30초
3회 실패 → 5분
```

---

### 장기 PROCESSING 복구

서버 장애 등으로 PROCESSING 상태가 오래 유지될 수 있는 상황을 고려했습니다.

```text
PROCESSING
+
30분 이상 지속
↓
복구 대상
```

---

### 동시성 대응

Notification 엔티티에 낙관적 락을 적용했습니다.

```java
@Version
private Long version;
```

목적:

- 동일 알림 동시 수정 방지
- 읽음 처리 충돌 감지

---

## API 명세

### 알림 생성

```http
POST /notifications
```

### 전체 조회

```http
GET /notifications
```

### 단건 조회

```http
GET /notifications/{id}
```

### 사용자별 조회

```http
GET /users/{receiverId}/notifications
```

### 읽음 처리

```http
PATCH /notifications/{id}/read
```

### 수동 재시도

```http
PATCH /notifications/{id}/retry
```

### JWT 토큰 발급

```http
POST /auth/token
```

### 관리자 대시보드

```http
GET /admin/dashboard
```

---

## DB 모델

| 컬럼 | 설명 |
|---|---|
| id | PK |
| receiverId | 수신자 ID |
| eventId | 이벤트 ID |
| message | 알림 메시지 |
| status | REQUESTED / PROCESSING / SENT / FAILED |
| retryCount | 재시도 횟수 |
| failureReason | 실패 사유 |
| nextRetryAt | 다음 재시도 가능 시간 |
| scheduledAt | 예약 발송 시간 |
| processingStartedAt | 처리 시작 시간 |
| readStatus | 읽음 여부 |
| channel | EMAIL / IN_APP |
| version | 낙관적 락 버전 |

---

## 테스트

실행:

```bash
./gradlew test
```

검증 항목:

- 알림 저장
- 중복 알림 방지
- 실패 시 재시도 횟수 증가
- 읽음 처리
- 낙관적 락 동작
- 장기 PROCESSING 복구 대상 판별
- 운영 보호 정책

---

## 구현 완료

- [x] 알림 생성
- [x] 전체 조회
- [x] 단건 조회
- [x] 사용자 조회
- [x] 읽음 처리
- [x] 예약 발송
- [x] 재시도
- [x] 지수 백오프
- [x] 중복 방지
- [x] PostgreSQL 저장
- [x] 비동기 처리
- [x] 관리자 대시보드
- [x] Swagger API 문서화
- [x] GitHub Actions CI
- [x] Spring Security
- [x] BCrypt 비밀번호 암호화
- [x] JWT 토큰 발급
- [x] JWT 인증 필터
- [x] 보호 API 인증 검증

---

## 미구현 / 개선 가능 사항

- User DB 기반 로그인
- Refresh Token
- 관리자 / 일반 사용자 권한 분리 고도화
- Kafka / RabbitMQ Queue 적용
- Redis 기반 토큰 관리
- Slack 운영 알림
- Docker Compose 실행 환경
- Testcontainers 기반 테스트 환경 분리

---

## AI 활용 범위

ChatGPT를 다음 범위에서 활용했습니다.

- 구조 설계 아이디어
- 테스트 아이디어
- README 정리
- 운영 시나리오 정리
- 에러 로그 분석 보조

최종 구현, 수정, 실행, 테스트 검증은 직접 수행했습니다.