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
- 운영 보호 정책

---

# 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Gradle
- JUnit5
- AssertJ
- Thymeleaf

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

```http
http://localhost:8080/admin/dashboard
```

---

# 핵심 설계 방향

## 1. 요청 저장과 실제 처리 분리

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

## 2. 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용:

- Service 중복 체크
- DB Unique Constraint
- 예외 처리
- 테스트 검증

---

## 3. 실패 재시도 정책

실패 시:

```text
retryCount 증가
failureReason 저장
nextRetryAt 계산
```

재시도:

```text
1회 실패 → 5초
2회 실패 → 30초
3회 실패 → 5분
```

목적:

- 일시 장애 대응
- 서버 부하 감소

---

## 4. 장기 PROCESSING 복구

```text
PROCESSING
+
30분 이상 지속
↓
복구 대상
```

서버 장애 상황 대응 목적.

---

## 5. 동시성 대응

Notification:

```java
@Version
private Long version;
```

적용:

- 읽음 처리 충돌 감지
- 동일 알림 동시 수정 방지

낙관적 락 테스트 포함.

---

## 6. 운영 보호 정책

반복 실패 사용자 보호:

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

현재는 설계 정책으로 정의했으며
운영 환경에서는 차단 정책으로 확장 가능합니다.

---

# API 명세

## 알림 생성

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

## 전체 조회

```http
GET /notifications
```

---

## 단건 조회

```http
GET /notifications/{id}
```

---

## 사용자별 조회

```http
GET /users/{receiverId}/notifications
```

---

## 읽음 필터

```http
GET /users/{receiverId}/notifications?readStatus=true
```

---

## 읽음 처리

```http
PATCH /notifications/{id}/read
```

---

## 수동 재시도

```http
PATCH /notifications/{id}/retry
```

---

## 관리자 실패 대시보드

```http
GET /admin/dashboard
```

목적:

```text
FAILED 상태 모니터링
운영 확인
```

---

# 관리자 화면

제공 기능:

- FAILED 알림 조회
- 실패 원인 확인
- 빈 상태 메시지 표시
- 상태 통계 카드(확장 가능)

예시:

```text
현재 실패 알림이 없습니다.
```

---

# 상태 관리

상태:

```text
REQUESTED
PROCESSING
SENT
FAILED
```

흐름:

```text
REQUESTED
↓
PROCESSING
↓
SENT
```

실패:

```text
PROCESSING
↓
FAILED
↓
재시도
```

---

# DB 모델

Notification:

| 컬럼 | 설명 |
|------|------|
| id | PK |
| receiverId | 사용자 |
| eventId | 이벤트 |
| status | 상태 |
| retryCount | 재시도 |
| failureReason | 실패 사유 |
| nextRetryAt | 다음 재시도 |
| scheduledAt | 예약 |
| processingStartedAt | 처리 시작 |
| readStatus | 읽음 |
| version | 낙관적 락 |

---

# 테스트

실행:

```bash
./gradlew test
```

검증:

- shouldSaveNotification
- shouldPreventDuplicateNotification
- shouldIncreaseRetryCountWhenFailed
- shouldMarkNotificationAsRead
- shouldApplyOptimisticLockWhenSameNotificationIsUpdatedConcurrently
- shouldDetectLongProcessingStatusAsRecoveryTarget

결과:

```text
BUILD SUCCESSFUL
```

---

# 테스트 네이밍 컨벤션 개선

초기:

```text
실패시재시도횟수증가()
```

변경:

```text
shouldIncreaseRetryCountWhenFailed()
```

이유:

- 협업 가독성
- Java 관례 반영
- 검색 편의성

---

# 구현 완료

완료:

- [x] 알림 생성
- [x] 전체 조회
- [x] 단건 조회
- [x] 사용자 조회
- [x] 읽음 처리
- [x] 예약 발송
- [x] 재시도
- [x] 지수 백오프
- [x] 중복 방지
- [x] PostgreSQL
- [x] 비동기 처리
- [x] 템플릿
- [x] 관리자 대시보드
- [x] 동시성 대응
- [x] 장기 PROCESSING 복구
- [x] 테스트
- [x] README
- [x] 운영 정책 고려

---

# 개선 가능 사항

- Kafka/RabbitMQ 기반 Queue 처리
- 분산락
- Slack 운영 알림
- 관리자 권한 처리
- Testcontainers
- 장애 차단 정책 고도화

---

# AI 활용 범위

활용:

- 구조 설계 아이디어
- 테스트 아이디어
- README 정리
- 운영 시나리오

최종 구현, 수정, 테스트,
실행 및 검증은 직접 수행했습니다.