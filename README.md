# Notification System

이 프로젝트는 이벤트 기반 알림 발송 시스템입니다.

결제 완료, 강의 시작, 실패 알림 등 다양한 이벤트가 발생했을 때
알림 요청을 저장하고 비동기 방식으로 처리합니다.

알림 발송 실패가 비즈니스 트랜잭션에 영향을 주지 않도록
요청 저장과 실제 발송 처리를 분리했습니다.

중복 발송 방지, 예약 발송, 읽음 처리, 메시지 템플릿,
재시도 정책, 지수 백오프, 최종 실패 기록,
운영자 모니터링 로그, 읽음 처리 동시성 대응,
장기 PROCESSING 상태 복구 로직을 포함합니다.

---

## 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- H2 Database
- Gradle
- JUnit 5
- AssertJ

---

## 실행 방법

프로젝트 실행:

```bash
./gradlew bootRun
```

테스트 실행:

```bash
./gradlew test
```

---

## 요구사항 해석 및 가정

알림 발송 요청 API는 실제 즉시 발송이 아니라
요청을 DB에 저장한 뒤 비동기 처리 대상 상태로 두는 방식으로 해석했습니다.

실제 이메일 발송은 과제 조건에 따라 수행하지 않고,
로그 출력으로 Mock 처리했습니다.

알림 발송 실패가 결제나 수강 신청 같은
비즈니스 트랜잭션에 영향을 주지 않도록
알림 요청 저장과 발송 처리를 분리했습니다.

`eventId`는 다음 두 가지 역할을 함께 수행합니다.

- 중복 발송 방지 기준
- 메시지 템플릿 선택 기준

---

## 설계 결정과 이유

### 1. 요청 저장과 발송 처리 분리

API 요청 시 바로 발송하지 않고
`REQUESTED` 상태로 DB에 저장합니다.

이후 `@Scheduled` 기반 Processor가 별도로 알림을 처리합니다.

```text
API 요청
↓
DB 저장
↓
REQUESTED
↓
Processor 처리
↓
SENT / FAILED
```

이 구조를 통해 알림 발송 실패가
비즈니스 요청 흐름에 직접 영향을 주지 않도록 했습니다.

---

### 2. PostgreSQL 적용

초기 개발 단계에서는 H2 Database를 사용했습니다.

하지만 서버 재시작 이후에도
미처리 알림과 실패 이력이 유지되어야 하므로
PostgreSQL로 전환했습니다.

효과:

- 알림 데이터 영속 저장
- 서버 재시작 후 미처리 알림 유지
- 운영 환경 확장 가능

---

### 3. 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용 방식:

- Service 레벨 중복 체크
- DB Unique Constraint
- 동시 중복 요청 시 DataIntegrityViolationException 처리

동시에 같은 요청이 들어와 Service 중복 체크를 통과하더라도,
DB Unique Constraint가 마지막 방어선 역할을 하도록 설계했습니다.

---

### 4. 메시지 템플릿

`eventId` 기준으로 메시지를 자동 생성합니다.

예시:

```text
PAYMENT_SUCCESS
↓
결제가 완료되었습니다.

PAYMENT_FAIL
↓
결제 실패

LECTURE_START
↓
강의가 시작됩니다.
```

효과:

- 메시지 관리 단순화
- 중복 문구 제거
- 유지보수 편의성 증가

---

### 5. 재시도 정책과 지수 백오프

발송 실패 시 최대 3회 재시도합니다.

실패 시 저장 정보:

```text
retryCount 증가
failureReason 저장
nextRetryAt 계산
```

재시도 간격:

```text
1차 실패 → 5초 후 재시도
2차 실패 → 30초 후 재시도
3차 실패 → 5분 후 재시도
```

목적:

- 일시적 장애 대응
- 서버 부하 감소
- 외부 시스템 복구 시간 확보

---

### 6. 읽음 처리 동시성 대응

여러 기기에서 동시에 읽음 처리 요청이 들어오는 상황을 고려하여
Notification 엔티티에 `@Version` 기반 낙관적 락을 적용했습니다.

또한 이미 읽음 상태인 알림에 다시 읽음 처리 요청이 들어와도
상태를 중복 변경하지 않도록 처리했습니다.

---

### 7. 장기 PROCESSING 상태 복구

알림 처리 중 서버 장애가 발생하면
`PROCESSING` 상태로 오래 남을 수 있습니다.

이를 고려하여 처리 시작 시간을 저장하는
`processingStartedAt` 필드를 추가했습니다.

```text
PROCESSING 상태
+
processingStartedAt 기준 30분 초과
↓
복구 대상
```

Processor는 오래 지속된 PROCESSING 상태를 다시 처리 대상으로 판단할 수 있습니다.

---

## API 명세 및 샘플 요청/응답

### 1. 알림 생성

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

Response:

```text
알림 요청 접수 완료
```

설명:

- 요청 즉시 발송하지 않습니다.
- DB에 `REQUESTED` 상태로 저장됩니다.
- Processor가 이후 비동기로 처리합니다.

---

### 2. 예약 알림 생성

```http
POST /notifications
```

Request:

```json
{
  "receiverId":"민경",
  "eventId":"LECTURE_START",
  "channel":"EMAIL",
  "scheduledAt":"2026-05-24T16:00:00"
}
```

Response:

```text
알림 요청 접수 완료
```

설명:

- `scheduledAt` 이전에는 발송하지 않습니다.
- `scheduledAt` 이후 Processor가 발송 처리합니다.

---

### 3. 전체 알림 조회

```http
GET /notifications
```

Response 예시:

```json
[
  {
    "id":1,
    "receiverId":"민경",
    "eventId":"PAYMENT_SUCCESS",
    "message":"결제가 완료되었습니다.",
    "channel":"EMAIL",
    "status":"SENT",
    "retryCount":0,
    "readStatus":false,
    "scheduledAt":null,
    "failureReason":null,
    "nextRetryAt":null,
    "processingStartedAt":null,
    "version":0,
    "readyToSend":true,
    "readyToRetry":true
  }
]
```

---

### 4. 단건 알림 조회

```http
GET /notifications/{id}
```

Response 예시:

```json
{
  "id":1,
  "receiverId":"민경",
  "eventId":"PAYMENT_SUCCESS",
  "message":"결제가 완료되었습니다.",
  "channel":"EMAIL",
  "status":"SENT",
  "retryCount":0,
  "readStatus":false,
  "scheduledAt":null,
  "failureReason":null,
  "nextRetryAt":null,
  "processingStartedAt":null,
  "version":0
}
```

---

### 5. 사용자별 알림 조회

```http
GET /users/{receiverId}/notifications
```

Response 예시:

```json
[
  {
    "id":1,
    "receiverId":"민경",
    "eventId":"PAYMENT_SUCCESS",
    "message":"결제가 완료되었습니다.",
    "status":"SENT",
    "readStatus":false
  }
]
```

---

### 6. 읽음 여부 필터 조회

```http
GET /users/{receiverId}/notifications?readStatus=true
```

Response 예시:

```json
[
  {
    "id":1,
    "receiverId":"민경",
    "eventId":"PAYMENT_SUCCESS",
    "message":"결제가 완료되었습니다.",
    "status":"SENT",
    "readStatus":true
  }
]
```

---

### 7. 읽음 처리

```http
PATCH /notifications/{id}/read
```

Response:

```text
읽음 처리 완료
```

설명:

- `readStatus`를 true로 변경합니다.
- 이미 읽음 상태라면 추가 변경하지 않습니다.
- `@Version`을 통해 동시 수정 충돌을 감지할 수 있습니다.

---

### 8. 수동 재시도

```http
PATCH /notifications/{id}/retry
```

Response:

```text
수동 재시도 요청 완료
```

설명:

- 실패한 알림을 다시 `REQUESTED` 상태로 변경합니다.
- `retryCount`, `failureReason`, `nextRetryAt`을 초기화합니다.

---

## 상태 관리

알림 상태:

```text
REQUESTED
PROCESSING
SENT
FAILED
```

상태 흐름:

```text
REQUESTED
↓
PROCESSING
↓
SENT
```

실패 시:

```text
PROCESSING
↓
FAILED
↓
재시도 가능 시간 이후 재처리
```

장기 PROCESSING 복구:

```text
PROCESSING
↓
30분 이상 지속
↓
복구 대상
```

---

## 비동기 처리 구조 및 재시도 정책

현재 구현:

```text
알림 요청
↓
DB 저장
↓
REQUESTED
↓
@Scheduled Processor
↓
PROCESSING
↓
SENT / FAILED
```

발송 실패 시:

```text
FAILED
↓
retryCount 증가
↓
nextRetryAt 설정
↓
재시도 가능 시간 이후 재처리
```

실제 운영 환경에서는 다음 구조로 확장 가능합니다.

```text
Kafka / RabbitMQ
↓
Consumer
↓
알림 처리
```

메시지 브로커는 사용하지 않았지만,
요청 저장과 실제 처리 로직을 분리하여
운영 환경에서 Queue 기반 처리로 전환 가능하도록 설계했습니다.

---

## 운영자 모니터링 로그

최종 실패 시 사용자에게 다시 알림을 보내지 않고,
운영자가 확인할 수 있도록 로그를 출력합니다.

예시:

```text
최종 실패 처리 완료

[운영자 알림]
eventId: PAYMENT_FAIL
receiverId: 민경
failureReason: 이메일 서버 오류
```

운영 환경에서는 다음 방식으로 확장할 수 있습니다.

- Slack 알림
- 관리자 페이지
- Email 모니터링

---

## DB 스키마 / 데이터 모델 설명

### Notification

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | Long | PK |
| receiverId | String | 수신자 ID |
| eventId | String | 이벤트 ID 및 알림 타입 |
| message | String | 템플릿 기반 메시지 |
| status | Enum | REQUESTED / PROCESSING / SENT / FAILED |
| retryCount | Integer | 재시도 횟수 |
| nextRetryAt | LocalDateTime | 다음 재시도 가능 시간 |
| processingStartedAt | LocalDateTime | PROCESSING 시작 시간 |
| readStatus | Boolean | 읽음 여부 |
| scheduledAt | LocalDateTime | 예약 발송 시간 |
| failureReason | String | 실패 사유 |
| channel | Enum | EMAIL / IN_APP |
| version | Long | 낙관적 락 버전 |

Unique Constraint:

```text
receiverId + eventId
```

목적:

- 동일 사용자에게 동일 이벤트 알림 중복 저장 방지
- 중복 발송 방지

---

## ERD 설명

현재 시스템은 Notification 단일 엔티티 기반으로 구성했습니다.

```text
Notification
──────────────────────────────
id (PK)
receiverId
eventId
message
status
retryCount
nextRetryAt
processingStartedAt
readStatus
scheduledAt
failureReason
channel
version (@Version)
──────────────────────────────
```

설계 목적:

- 단일 알림 단위 상태 관리
- 재시도 이력 저장
- 예약 발송 대응
- 읽음 처리 상태 관리
- PROCESSING 장기 지속 복구
- 낙관적 락을 통한 동시성 대응

---

## 테스트 실행 방법

```bash
./gradlew test
```

작성 테스트:

- 알림 저장 테스트
- 중복 방지 테스트
- 읽음 처리 테스트
- 실패 시 재시도 횟수 증가 테스트
- 상태 변경 테스트
- 템플릿 메시지 테스트
- 실패 시 다음 재시도 시간 설정 테스트
- 이미 읽음 상태에서 중복 읽음 처리 테스트
- 낙관적 락 기반 동시 수정 테스트
- 장기 PROCESSING 상태 복구 대상 판단 테스트

테스트 실행 결과:

```text
BUILD SUCCESSFUL
```

---

## 구현 완료 기능

완료:

- [x] 알림 생성
- [x] 전체 조회
- [x] 단건 조회
- [x] 사용자별 조회
- [x] 읽음 필터
- [x] 읽음 처리
- [x] 예약 발송
- [x] 실패 기록
- [x] 수동 재시도
- [x] 중복 방지
- [x] 동시 중복 요청 예외 처리
- [x] PostgreSQL 영속 저장
- [x] 비동기 처리
- [x] 메시지 템플릿
- [x] 지수 백오프
- [x] 운영자 모니터링 로그
- [x] 읽음 처리 동시성 대응
- [x] 장기 PROCESSING 상태 복구 로직
- [x] 테스트 코드 작성
- [x] 운영 시나리오 고려

---

## 미구현 / 제약사항

- 실제 이메일 발송은 구현하지 않고 로그 출력으로 대체했습니다.
- 실제 메시지 브로커(Kafka/RabbitMQ)는 사용하지 않았습니다.
- 다중 인스턴스 환경의 분산락은 설계 방향만 기술했습니다.
- 관리자 대시보드는 구현하지 않고 운영자 로그로 대체했습니다.

---

## 개선 가능 사항

- Queue 기반 비동기 처리
- Slack 연동
- 관리자 대시보드
- DB Lock / 분산락 기반 다중 인스턴스 대응
- 복구 시간 정책 고도화
- 테스트 환경 분리

---

## 요구사항 개선 의견

현재 구현은 Scheduler 기반 비동기 처리 방식을 사용했습니다.

실제 운영 환경에서는 다음과 같은 개선이 필요하다고 판단했습니다.

### 1. 메시지 브로커 도입

현재:

```text
DB
↓
Scheduler
↓
Processor
```

확장 가능:

```text
Kafka / RabbitMQ
↓
Consumer
↓
알림 처리
```

목적:

- 처리량 증가
- 장애 격리
- 다중 인스턴스 대응

---

### 2. 다중 인스턴스 중복 처리 방지

현재는 단일 서버 기준으로 구현했습니다.

운영 환경에서 여러 서버가 동시에 같은 알림을 처리할 경우
중복 처리가 발생할 수 있으므로 다음 방식이 필요합니다.

- DB Lock
- 분산락
- Queue 기반 단일 Consumer 처리
- 상태 선점 방식

---

### 3. 테스트 환경 분리

현재 테스트는 Spring Boot Context와 실제 PostgreSQL 연결을 사용합니다.

운영 DB와 테스트 DB를 분리하거나,
Testcontainers 등을 활용하면 더 안정적인 테스트 환경을 구성할 수 있습니다.

---

## AI 활용 범위

ChatGPT 활용:

- 구조 설계 아이디어
- 상태 관리 방향
- 운영 시나리오 아이디어
- PostgreSQL 전환 방향
- 재시도 정책
- 메시지 템플릿 설계 아이디어
- 테스트 코드 아이디어
- README 초안 작성 및 정리

최종 구현, 수정, 테스트 실행 및 검증은 직접 수행했습니다.