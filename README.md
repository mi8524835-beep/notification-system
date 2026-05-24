# Notification System

이 프로젝트는 이벤트 기반 알림 발송 시스템입니다.

결제 완료, 수강 신청, 강의 시작 등 이벤트 발생 시
사용자에게 알림 요청을 저장하고 비동기 처리 방식으로 발송합니다.

실패 시 재시도 정책을 적용하며,
중복 발송 방지, 읽음 처리, 예약 발송,
최종 실패 보관 및 수동 재시도 기능을 제공합니다.

또한 실제 운영 환경을 고려하여
DB 영속성, 상태 관리, 장애 대응 시나리오까지 함께 설계했습니다.

---

## 기술 스택

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Gradle

초기 개발:

- H2 Database

운영 고려:

- PostgreSQL 전환

---

## 실행 방법

프로젝트 실행:

```bash
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

---

## API 목록

### 알림 생성

POST `/notifications`

예시:

```json
{
  "receiverId":"민경",
  "message":"결제 완료",
  "eventId":"PAYMENT_001",
  "channel":"EMAIL"
}
```

---

### 예약 발송

```json
{
  "receiverId":"민경",
  "message":"예약 발송",
  "eventId":"SCHEDULE_001",
  "channel":"EMAIL",
  "scheduledAt":"2026-05-24T12:00:00"
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

읽음 필터:

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

# 상태 관리

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
최종 실패
```

---

# 비동기 처리 구조

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

실제 운영 환경:

```text
Kafka
RabbitMQ
Queue
```

전환 가능하도록 설계

---

# 재시도 정책

현재:

최대 3회 재시도

실패 시:

```text
FAILED 유지
failureReason 저장
retryCount 증가
```

---

## 지수 백오프 적용

재시도 간격을 점진적으로 증가하도록 구현

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
- 장애 상황에서 과도한 재요청 방지
- 장애 복구 시간 확보

---

## 운영자 모니터링 로그

최종 실패 발생 시
사용자에게 즉시 재알림하지 않고
운영자가 확인할 수 있도록 로그 출력

예시:

```text
최종 실패 처리 완료

[운영자 알림]

eventId: PAYMENT_FAIL_002
receiverId: 민경
failureReason: 이메일 서버 오류
```

운영 환경 확장 가능:

- Slack 알림
- 관리자 페이지
- Email 모니터링

---

# 중복 발송 방지

중복 기준:

```text
receiverId + eventId
```

적용:

- Service 중복 체크
- DB Unique 제약

동일 이벤트 재발송 차단

예시 로그:

```text
중복 알림 요청 - 저장하지 않음
```

---

# 예약 발송

구현:

```text
scheduledAt 이전
↓
대기

scheduledAt 이후
↓
발송 가능
```

---

# 수동 재시도

최종 실패 알림:

```http
PATCH /notifications/{id}/retry
```

관리자가 재처리 가능

---

# PostgreSQL 적용

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

- 서버 재시작 후 데이터 유지
- 알림 이력 영속 저장
- 유실 없는 재처리 대응 가능
- 운영 환경 확장 가능

---

# 운영 시나리오 고려

### 서버 재시작

PostgreSQL 적용으로
기존 알림 유지 가능

---

### 다중 인스턴스 환경

현재:

단일 서버 기준

개선 가능:

- DB Lock
- 분산 락
- Queue
- 상태 선점 방식

---

### 장기 PROCESSING 상태

개선 가능:

```text
PROCESSING 지속
↓
FAILED 전환
↓
재시도
```

---

# DB 설계

Notification

| 컬럼 | 설명 |
|------|------|
| id | PK |
| receiverId | 수신자 |
| message | 내용 |
| eventId | 이벤트 |
| status | 상태 |
| retryCount | 재시도 횟수 |
| nextRetryAt | 다음 재시도 시간 |
| readStatus | 읽음 여부 |
| scheduledAt | 예약 시간 |
| failureReason | 실패 이유 |
| channel | 발송 채널 |

---

# 구현 완료 기능

완료:

- [x] 알림 생성
- [x] 전체 조회
- [x] 단건 조회
- [x] 사용자별 조회
- [x] 읽음 처리
- [x] 읽음 필터
- [x] 실패 기록
- [x] 중복 방지
- [x] 예약 발송
- [x] 비동기 처리
- [x] PostgreSQL 영속 저장
- [x] 수동 재시도
- [x] 지수 백오프 재시도
- [x] 운영자 모니터링 로그
- [x] 운영 시나리오 고려

---

# 개선 가능 사항

- [ ] 메시지 템플릿
- [ ] Slack 연동
- [ ] Queue 적용
- [ ] 관리자 대시보드
- [ ] 동시성 처리
- [ ] 분산락
- [ ] 다중 인스턴스 대응

---

# AI 활용 범위

ChatGPT 활용:

- 구조 설계 아이디어
- 상태 관리 방향
- README 초안
- 운영 시나리오 아이디어
- 재시도 정책
- PostgreSQL 전환 방향
- 테스트 코드 아이디어

최종 구현, 수정 및 검증은 직접 수행