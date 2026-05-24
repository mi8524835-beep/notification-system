# Notification System

이 프로젝트는 이벤트 기반 알림 발송 시스템입니다.

결제 완료, 강의 시작, 실패 알림 등 이벤트 발생 시
알림 요청을 저장하고 비동기 처리 방식으로 발송합니다.

실패 시 재시도 정책을 적용하며,
중복 발송 방지, 예약 발송, 읽음 처리,
최종 실패 기록 및 운영자 모니터링 기능을 제공합니다.

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

초기 개발:

```text
H2 Database
```

운영 고려:

```text
PostgreSQL 적용
```

---

# 실행 방법

프로젝트 실행:

```bash
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

---

# API 목록

### 알림 생성

POST

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

# 상태 관리

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

운영 확장 가능:

```text
Kafka
RabbitMQ
Queue
```

---

# 메시지 템플릿

eventId 기준으로 메시지를 자동 생성

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

# 재시도 정책

최대:

```text
3회 재시도
```

실패 시:

```text
retryCount 증가
failureReason 저장
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
- 장애 상황 재요청 방지
- 장애 복구 시간 확보

---

# 운영자 모니터링 로그

최종 실패 시:

```text
최종 실패 처리 완료

[운영자 알림]

eventId: PAYMENT_FAIL
receiverId: 민경
failureReason: 이메일 서버 오류
```

운영 확장 가능:

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
- DB Unique Constraint

동일 이벤트 재발송 차단

---

# 예약 발송

```text
scheduledAt 이전
↓
대기

scheduledAt 이후
↓
발송
```

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

- 데이터 영속 저장
- 서버 재시작 후 유지
- 운영 환경 확장 가능

---

# 운영 시나리오 고려

### 서버 재시작

PostgreSQL 적용으로
기존 알림 유지 가능

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

### 다중 인스턴스 환경

확장 가능:

- Queue
- 분산락
- 상태 선점 방식
- DB Lock

---

# DB 설계

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

# 구현 완료 기능

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
- [x] 운영 시나리오 고려

---

# 개선 가능 사항

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
- 운영 시나리오 아이디어
- PostgreSQL 전환 방향
- 재시도 정책
- README 초안
- 테스트 시나리오

최종 구현, 수정 및 검증은 직접 수행