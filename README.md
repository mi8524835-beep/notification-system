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

## API 목록 및 예시

### 알림 생성

```http
POST /notifications
```

예시:

```json
{
  "receiverId":"민경",
  "eventId":"PAYMENT_SUCCESS",
  "channel":"EMAIL"
}
```

---

### 예약 발송

```json
{
  "receiverId":"민경",
  "eventId":"LECTURE_START",
  "channel":"EMAIL",
  "scheduledAt":"2026-05-24T16:00:00"
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

읽음 여부 필터:

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

## 요구사항 해석 및 가정

알림 발송 요청 API는 실제 즉시 발송이 아니라
요청을 DB에 저장한 뒤 비동기 처리 대상 상태로 두는 방식으로 해석했습니다.

실제 이메일 발송은 과제 조건에 따라 수행하지 않고,
로그 출력으로 Mock 처리했습니다.

알림 발송 실패가 결제나 수강 신청 같은
비즈니스 트랜잭션에 영향을 주지 않도록
알림 요청 저장과 발송 처리를 분리했습니다.

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

또는

FAILED
↓
재시도
↓
최종 FAILED
```

---

## 비동기 처리 구조

현재 구현:

```text
알림 요청
↓
DB 저장
↓
REQUESTED
↓
@Scheduled 처리
↓
PROCESSING
↓
SENT / FAILED
```

실제 운영 환경에서는 다음 구조로 확장 가능합니다.

```text
Kafka
RabbitMQ
Queue
```

메시지 브로커는 사용하지 않았지만,
요청 저장과 실제 처리 로직을 분리하여
운영 환경에서 Queue 기반 처리로 전환 가능하도록 설계했습니다.

---

## 메시지 템플릿

eventId 기준으로 메시지를 자동 생성합니다.

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
강의 시작
```

효과:

- 메시지 관리 단순화
- 중복 문구 제거
- 유지보수 편의성 증가

---

## 재시도 정책

최대 3회 재시도합니다.

실패 시:

```text
retryCount 증가
failureReason 저장
nextRetryAt 계산
```

---

### 지수 백오프 적용

재시도 간격을 점진적으로 증가하도록 구현했습니다.

```text
1차 실패
↓
5초 후 재시도

2차 실패
↓
30초 후 재시도

3차 실패
↓
5분 후 재시도
```

목적:

- 서버 부하 감소
- 장애 상황 재요청 방지
- 장애 복구 시간 확보

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

운영 환경에서는 다음 방식으로 확장 가능합니다.

- Slack 알림
- 관리자 페이지
- Email 모니터링

---

## 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용:

- Service 중복 체크
- DB Unique Constraint

동일 이벤트에 대한 중복 알림 저장을 방지합니다.

---

## 예약 발송

```text
scheduledAt 이전
↓
대기

scheduledAt 이후
↓
발송
```

특정 시각 이후에만 발송되도록 처리했습니다.

---

## PostgreSQL 적용

초기 개발 단계에서는 H2 Database를 사용했습니다.

문제:

```text
서버 재시작 시 데이터 유실 가능
```

개선:

```text
PostgreSQL 적용
```

효과:

- 데이터 영속 저장
- 서버 재시작 후 알림 유지
- 운영 환경 확장 가능

---

## 운영 시나리오 고려

### 서버 재시작

PostgreSQL 적용으로 기존 알림 데이터가 유지됩니다.

---

### 장기 PROCESSING 상태

현재는 PROCESSING 상태가 장시간 유지되는 경우를
자동 복구하지 않았습니다.

개선 방향:

```text
PROCESSING 지속
↓
일정 시간 초과 감지
↓
FAILED 전환
↓
재시도
```

---

### 다중 인스턴스 환경

현재 구현은 단일 서버 기준입니다.

다중 인스턴스 환경에서는 동일 알림을
여러 서버가 동시에 처리할 수 있으므로
다음 방식으로 확장할 수 있습니다.

- Queue
- 분산락
- 상태 선점 방식
- DB Lock

---

## 데이터 모델 설명

Notification

| 컬럼 | 설명 |
|------|------|
| id | PK |
| receiverId | 수신자 |
| eventId | 이벤트 |
| message | 템플릿 메시지 |
| status | 상태 |
| retryCount | 재시도 횟수 |
| nextRetryAt | 다음 재시도 시간 |
| readStatus | 읽음 여부 |
| scheduledAt | 예약 시간 |
| failureReason | 실패 이유 |
| channel | 발송 채널 |

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
- [x] PostgreSQL 영속 저장
- [x] 비동기 처리
- [x] 메시지 템플릿
- [x] 지수 백오프
- [x] 운영자 모니터링 로그
- [x] 테스트 코드 작성
- [x] 운영 시나리오 고려

---

## 미구현 / 제약사항

- 실제 이메일 발송은 구현하지 않고 로그 출력으로 대체했습니다.
- 실제 메시지 브로커(Kafka/RabbitMQ)는 사용하지 않았습니다.
- 다중 인스턴스 환경의 분산락은 설계 방향만 기술했습니다.
- 장기 PROCESSING 상태 자동 복구는 개선 방향으로 남겼습니다.
- 관리자 대시보드는 구현하지 않고 운영자 로그로 대체했습니다.

---

## 개선 가능 사항

- Queue 기반 비동기 처리
- Slack 연동
- 관리자 대시보드
- 장기 PROCESSING 자동 복구
- DB Lock / 분산락 기반 다중 인스턴스 대응
- 읽음 처리 동시성 보완

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