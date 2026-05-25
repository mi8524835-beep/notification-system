# Notification System

Spring Boot 기반 이벤트 알림 시스템입니다.

결제 완료, 강의 시작, 실패 알림 등 다양한 이벤트 발생 시
알림 요청을 저장하고 비동기 방식으로 처리합니다.

알림 발송 실패가 비즈니스 트랜잭션에 영향을 주지 않도록
요청 저장과 실제 처리 로직을 분리했습니다.

구현 기능:

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
- Swagger 기반 API 문서
- 운영 보호 정책

---

## Dashboard

운영자가 실패 알림 상태를 확인할 수 있는 모니터링 화면입니다.

![dashboard](docs/dashboard.png)

---

## Swagger API Documentation

실행 후 아래 주소에서 API 문서를 확인할 수 있습니다.

```http
http://localhost:8080/swagger-ui/index.html
```

Swagger UI를 통해 API 목록과 요청 형식을 확인할 수 있습니다.

![swagger](docs/swagger.png)

---

## 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Gradle
- JUnit5
- AssertJ
- Thymeleaf
- Springdoc OpenAPI / Swagger UI

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

```http
http://localhost:8080/admin/dashboard
```

Swagger:

```http
http://localhost:8080/swagger-ui/index.html
```

---

## 핵심 설계 방향

### 1. 요청 저장과 실제 처리 분리

알림 요청 즉시 발송하지 않습니다.

```text
API 요청
↓
DB 저장 (REQUESTED)
↓
Processor
↓
PROCESSING
↓
SENT / FAILED
```

목적:

- 비즈니스 로직 보호
- 장애 격리
- 재시도 가능

---

### 2. 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용:

- Service 중복 체크
- DB Unique Constraint
- 예외 처리
- 테스트 검증

동시에 같은 요청이 들어와 Service 중복 체크를 통과하더라도
DB 제약 조건이 마지막 방어선 역할을 하도록 설계했습니다.

---

### 3. 실패 재시도 정책

실패 시 저장 정보:

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

목적:

- 일시 장애 대응
- 서버 부하 감소
- 외부 시스템 복구 시간 확보

---

### 4. 장기 PROCESSING 상태 복구

알림 처리 중 서버 장애가 발생하면
`PROCESSING` 상태로 오래 남을 수 있습니다.

이를 고려하여 처리 시작 시간을 저장하는
`processingStartedAt` 필드를 추가했습니다.

```text
PROCESSING
+
30분 이상 지속
↓
복구 대상
```

---

### 5. 동시성 대응

Notification 엔티티에 `@Version` 기반 낙관적 락을 적용했습니다.

```java
@Version
private Long version;
```

적용 목적:

- 읽음 처리 충돌 감지
- 동일 알림 동시 수정 방지

낙관적 락 동작을 검증하는 테스트를 포함했습니다.

---

### 6. 관리자 실패 알림 대시보드

최종 실패 상태의 알림을 운영자가 확인할 수 있도록
간단한 관리자 대시보드를 구현했습니다.

```http
GET /admin/dashboard
```

제공 기능:

- FAILED 알림 조회
- 실패 원인 확인
- 상태 통계 카드
- 전체 알림 JSON 링크
- 빈 상태 메시지 표시

---

### 7. Swagger API 문서화

Springdoc OpenAPI를 적용하여
구현된 API를 Swagger UI에서 확인할 수 있도록 했습니다.

이를 통해 API 목록, HTTP Method, Path를 한 화면에서 확인할 수 있습니다.

---

### 8. 운영 보호 정책

반복 실패 사용자 보호 정책을 고려했습니다.

```text
동일 receiverId 기준
FAILED 상태가 반복 누적되는 경우
↓
추가 알림 요청 제한 가능
```

목적:

- 무한 재시도 방지
- 외부 장애 확산 방지
- 운영 안정성 확보

현재는 설계 정책으로 정의했으며,
운영 환경에서는 차단 정책으로 확장 가능합니다.

---

## API 명세

### 알림 생성

```http
POST /notifications
```

Request:

```json
{
  "receiverId":"민경",
  "eventId":"PAYMENT_SUCCESS",
  "channel":"EMAIL"
}
```

---

### 전체 조회

```http
GET /notifications
```

---

### 단건 조회

```http
GET /notifications/{id}
```

---

### 사용자별 조회

```http
GET /users/{receiverId}/notifications
```

---

### 읽음 필터

```http
GET /users/{receiverId}/notifications?readStatus=true
```

---

### 읽음 처리

```http
PATCH /notifications/{id}/read
```

---

### 수동 재시도

```http
PATCH /notifications/{id}/retry
```

---

### 관리자 실패 대시보드

```http
GET /admin/dashboard
```

---

## 상태 관리

상태:

```text
REQUESTED
PROCESSING
SENT
FAILED
```

정상 흐름:

```text
REQUESTED
↓
PROCESSING
↓
SENT
```

실패 흐름:

```text
PROCESSING
↓
FAILED
↓
재시도 가능 시간 이후 재처리
```

---

## DB 모델

### Notification

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
| processingStartedAt | PROCESSING 시작 시간 |
| readStatus | 읽음 여부 |
| channel | EMAIL / IN_APP |
| version | 낙관적 락 버전 |

Unique Constraint:

```text
receiverId + eventId
```

---

## 테스트

실행:

```bash
./gradlew test
```

검증 항목:

- shouldSaveNotification
- shouldPreventDuplicateNotification
- shouldIncreaseRetryCountWhenFailed
- shouldMarkNotificationAsRead
- shouldApplyOptimisticLockWhenSameNotificationIsUpdatedConcurrently
- shouldDetectLongProcessingStatusAsRecoveryTarget

테스트 실행 결과:

```text
BUILD SUCCESSFUL
```

---

## 테스트 네이밍 컨벤션 개선

초기 테스트 코드는 한글 메서드명을 사용했습니다.

예:

```text
실패시재시도횟수증가()
```

협업 환경과 Java 테스트 네이밍 관례를 고려하여
영문 기반 메서드명으로 변경했습니다.

변경 예:

```text
shouldIncreaseRetryCountWhenFailed()
```

목적:

- 협업 가독성 향상
- IDE 검색 편의성
- 테스트 의도 명확화

---

## 구현 완료

- [x] 알림 생성
- [x] 전체 조회
- [x] 단건 조회
- [x] 사용자 조회
- [x] 읽음 필터
- [x] 읽음 처리
- [x] 예약 발송
- [x] 재시도
- [x] 지수 백오프
- [x] 중복 방지
- [x] 동시 중복 요청 예외 처리
- [x] PostgreSQL 영속 저장
- [x] 비동기 처리
- [x] 메시지 템플릿
- [x] 관리자 실패 알림 대시보드
- [x] Swagger API 문서화
- [x] 읽음 처리 동시성 대응
- [x] 장기 PROCESSING 복구
- [x] 테스트 코드 작성
- [x] 운영 정책 고려

---

## 미구현 / 제약사항

- 실제 이메일 발송은 과제 조건에 따라 구현하지 않고 로그 출력으로 대체했습니다.
- Kafka/RabbitMQ는 직접 설치하지 않았지만, 요청 저장과 처리 로직을 분리하여 Queue 기반 구조로 전환 가능하도록 설계했습니다.
- 다중 인스턴스 환경은 DB Unique Constraint, 낙관적 락, 상태 기반 처리로 일부 대응했으며, 운영 환경에서는 DB Lock 또는 분산락으로 확장할 수 있습니다.
- 관리자 기능은 별도 로그인/권한 처리는 생략하고, 최종 실패 알림 확인용 대시보드로 구현했습니다.

---

## 개선 가능 사항

- Kafka/RabbitMQ 기반 Queue 처리
- DB Lock 또는 분산락을 통한 다중 인스턴스 처리 고도화
- Slack 또는 Email 기반 운영자 알림 연동
- 관리자 페이지 권한 처리
- 반복 실패 사용자 차단 정책 실제 구현
- Testcontainers 기반 테스트 환경 분리
- GitHub Actions 기반 CI 자동 테스트

---

## AI 활용 범위

ChatGPT를 다음 범위에서 활용했습니다.

- 구조 설계 아이디어
- 상태 관리 방향
- 운영 시나리오 아이디어
- PostgreSQL 전환 방향
- 재시도 정책
- 메시지 템플릿 설계 아이디어
- 테스트 코드 아이디어
- README 초안 작성 및 정리

최종 구현, 수정, 테스트 실행 및 검증은 직접 수행했습니다.