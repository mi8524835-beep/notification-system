# Notification System

이 프로젝트는 이벤트 기반 알림 발송 시스템입니다.

결제 완료, 강의 시작, 실패 알림 등 다양한 이벤트 발생 시
알림 요청을 저장하고 비동기 처리 방식으로 발송합니다.

실패 시 재시도 정책을 적용하며,
중복 발송 방지, 예약 발송, 읽음 처리,
메시지 템플릿, 최종 실패 기록, 수동 재시도,
운영자 모니터링 로그 기능을 제공합니다.

또한 실제 운영 환경을 고려하여
DB 영속성, 장애 대응, 재시도 정책,
운영 시나리오까지 함께 설계했습니다.

---

# 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- H2 Database
- Gradle
- JUnit5
- AssertJ

---

# 실행 방법

프로젝트 실행:

```bash
./gradlew bootRun
```

테스트 실행:

```bash
./gradlew test
```

---

# 요구사항 해석 및 가정

알림 발송 요청 API는 실제 즉시 발송이 아니라
요청을 DB에 저장한 뒤 비동기 처리 대상 상태로 두는 방식으로 해석했습니다.

실제 이메일 발송은 수행하지 않고
로그 출력(Mock) 방식으로 처리했습니다.

알림 발송 실패가
결제나 수강 신청 같은 비즈니스 로직에 영향을 주지 않도록
요청 저장과 발송 처리를 분리했습니다.

eventId는:

- 중복 발송 방지 기준
- 메시지 템플릿 선택 기준

역할을 함께 수행합니다.

---

# 설계 결정과 이유

## 요청 저장과 발송 처리 분리

현재 구조:

```text
API 요청
↓
REQUESTED 저장
↓
Processor 처리
↓
SENT / FAILED
```

목적:

- 요청 지연 최소화
- 발송 실패가 비즈니스 트랜잭션에 영향 주지 않도록 분리

---

## PostgreSQL 적용

초기:

```text
H2 Database
```

문제:

```text
서버 재시작 시 데이터 유실 가능
```

개선:

```text
PostgreSQL 적용
```

효과:

- 영속 저장
- 재시작 후 데이터 유지
- 운영 환경 확장 가능

---

## 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용:

- Service 중복 체크
- DB Unique Constraint

---

## 메시지 템플릿 적용

eventId 기반:

예시:

```text
PAYMENT_SUCCESS
↓
결제가 완료되었습니다.

LECTURE_START
↓
강의가 시작됩니다.
```

효과:

- 유지보수 편의성
- 중복 제거

---

## 재시도 정책 + 지수 백오프

최대:

```text
3회 재시도
```

간격:

```text
1차 → 5초

2차 → 30초

3차 → 5분
```

목적:

- 서버 부하 감소
- 장애 복구 시간 확보

---

## 읽음 처리 동시성 대응

Notification 엔티티에:

```java
@Version
private Long version;
```

적용.

목적:

- 여러 기기에서 동시에 읽음 처리 시 충돌 감지
- 낙관적 락 적용

---

# API 명세 및 샘플 요청/응답

## 알림 생성

POST

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

---

## 예약 알림

Request:

```json
{
  "receiverId":"민경",
  "eventId":"LECTURE_START",
  "channel":"EMAIL",
  "scheduledAt":"2026-05-24T16:00:00"
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

또는

FAILED
↓
retry
↓
최종 FAILED
```

---

# 비동기 처리 구조

현재:

```text
API
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

운영 확장:

```text
Kafka
RabbitMQ
Queue
```

---

# 운영자 모니터링 로그

최종 실패:

예시:

```text
최종 실패 처리 완료

[운영자 알림]

eventId:
receiverId:
failureReason:
```

운영 확장:

- Slack
- 관리자 페이지
- Email 모니터링

---

# DB 스키마 / 데이터 모델 설명

## Notification

| 컬럼 | 설명 |
|------|------|
| id | PK |
| receiverId | 수신자 |
| eventId | 이벤트 |
| message | 템플릿 메시지 |
| status | 상태 |
| retryCount | 재시도 |
| nextRetryAt | 다음 재시도 |
| readStatus | 읽음 여부 |
| scheduledAt | 예약 시간 |
| failureReason | 실패 이유 |
| channel | 발송 채널 |
| version | 낙관적 락 |

Unique:

```text
receiverId + eventId
```

---

# ERD 설명

```text
Notification
─────────────────────
id (PK)

receiverId

eventId

message

status

retryCount

nextRetryAt

readStatus

scheduledAt

failureReason

channel

version
─────────────────────
```

설계 목적:

- 상태 관리
- 재시도 관리
- 예약 발송
- 동시성 대응

---

# 테스트 실행 방법

```bash
./gradlew test
```

작성 테스트:

- 알림 저장
- 중복 방지
- 읽음 처리
- 실패 처리
- 상태 변경
- 템플릿 테스트
- nextRetryAt 테스트

결과:

```text
BUILD SUCCESSFUL
```

---

# 구현 완료 기능

완료:

- [x] 알림 생성
- [x] 조회
- [x] 읽음 처리
- [x] 예약 발송
- [x] 실패 기록
- [x] 수동 재시도
- [x] PostgreSQL
- [x] 비동기 처리
- [x] 템플릿
- [x] 지수 백오프
- [x] 운영자 로그
- [x] 동시성 대응
- [x] 테스트 코드

---

# 미구현 / 제약사항

- 실제 이메일 발송 없음
- Kafka/RabbitMQ 미적용
- 분산락 미구현
- PROCESSING 자동 복구 미구현
- 관리자 대시보드 없음

---

# 개선 가능 사항

- Queue 적용
- Slack 연동
- 관리자 페이지
- PROCESSING 복구
- 분산락
- 동시성 테스트 보강

---

# 요구사항 개선 의견

현재 구현은 Scheduler 기반입니다.

운영 환경 개선 방향:

### 메시지 브로커

현재:

```text
DB
↓
Scheduler
```

확장:

```text
Kafka
RabbitMQ
Consumer
```

---

### 장기 PROCESSING 복구

```text
PROCESSING 오래 지속
↓
FAILED
↓
retry
```

---

### 동시성 테스트 보강

현재:

```text
@Version 적용
```

향후:

```text
실제 충돌 테스트 추가
```

---

# AI 활용 범위

ChatGPT 활용:

- 구조 설계 아이디어
- README 정리
- 테스트 아이디어
- 운영 시나리오
- 재시도 정책
- PostgreSQL 전환 방향

최종 구현 및 검증은 직접 수행했습니다.