![CI](https://github.com/mi8524835-beep/notification-system/actions/workflows/ci.yml/badge.svg)

# Notification System

Spring Boot 기반 이벤트 알림 시스템입니다.

결제 완료, 강의 시작, 실패 알림 등 다양한 이벤트 발생 시
알림 요청을 저장하고 비동기 방식으로 처리합니다.

알림 발송 실패가 비즈니스 트랜잭션에 영향을 주지 않도록
요청 저장과 실제 처리 로직을 분리했습니다.

---

## 주요 기능

- 비동기 알림 처리
- 재시도 정책 + 지수 백오프
- 예약 발송
- 읽음 처리
- 중복 발송 방지
- 메시지 템플릿
- PostgreSQL 영속 저장
- 낙관적 락 기반 동시성 대응
- 장기 PROCESSING 상태 복구
- 관리자 실패 알림 대시보드
- GitHub Actions CI 자동 테스트
- 운영 보호 정책

---

## CI Status

GitHub Actions 기반 자동 테스트 적용

동작:

```text
Push
↓
GitHub Actions 실행
↓
Gradle Test
↓
성공 여부 검증
```

현재 상태:

자동 테스트 성공 ✅

---

## Dashboard

운영자가 실패 알림 상태를 확인할 수 있는 모니터링 화면

![dashboard](docs/dashboard.png)

제공 기능:

- FAILED 상태 모니터링
- 상태별 통계 카드
- 빈 상태 메시지
- 실패 원인 확인

---

## Swagger API

API 테스트 및 명세 확인 가능

![swagger](docs/swagger.png)

실행 후:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Thymeleaf
- JUnit5
- GitHub Actions
- Gradle

---

# 실행 방법

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

# 아키텍처 흐름

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

목적:

- 장애 격리
- 재시도 가능
- 트랜잭션 보호

---

# 구현 완료

완료:

- [x] CRUD
- [x] 읽음 처리
- [x] 예약 발송
- [x] 재시도
- [x] 지수 백오프
- [x] 중복 방지
- [x] PostgreSQL
- [x] 비동기 처리
- [x] 관리자 대시보드
- [x] 낙관적 락
- [x] PROCESSING 복구
- [x] Swagger
- [x] GitHub Actions
- [x] CI 자동 테스트
- [x] README 정리
- [x] 운영 정책 고려

---

# 개선 가능 사항

- Kafka / RabbitMQ Queue
- Slack 운영 알림
- 관리자 권한
- Testcontainers
- 장애 차단 정책
- Circuit Breaker
- 분산락

---

# AI 활용 범위

활용:

- 구조 설계 아이디어
- README 정리
- 테스트 아이디어
- 운영 시나리오

최종 구현 / 수정 / 테스트 / 실행 / 검증은 직접 수행