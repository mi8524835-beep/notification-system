# Notification System

프로덕트 엔지니어 채용 과제 중 **BE-C. 알림 발송 시스템**을 구현한 Spring Boot 기반 백엔드 프로젝트입니다.

이 프로젝트는 수강 신청 완료, 결제 확정, 강의 시작 D-1, 취소 처리 등 다양한 이벤트가 발생했을 때 사용자에게 이메일 또는 인앱 알림을 발송하는 상황을 가정합니다.

알림 요청 API는 실제 발송을 즉시 수행하지 않고 요청을 DB에 저장한 뒤, 별도 처리 로직이 저장된 알림을 조회하여 비동기적으로 발송 상태를 관리합니다.

---

# 프로젝트 개요

본 시스템은 다음 요구사항을 중심으로 설계했습니다.

- 알림 발송 요청 등록
- 알림 상태 조회
- 사용자별 알림 목록 조회
- 읽음 처리
- 중복 발송 방지
- 실패 시 재시도
- 실패 사유 기록
- 서버 재시작 후 미처리 알림 재처리
- 관리자 페이지를 통한 상태 확인 및 수동 재시도
- JWT 기반 관리자 기능 보호

API 요청 스레드는 알림 요청을 `REQUESTED` 상태로 저장한 뒤 즉시 응답합니다.

실제 발송 처리는 DB에 저장된 알림을 별도 Processor가 조회하여 처리합니다.

---

# 기술 스택

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- H2
- Thymeleaf
- Swagger / Springdoc OpenAPI
- Gradle
- JUnit5

---

# 실행 방법

## 1. PostgreSQL DB 생성

```sql
CREATE DATABASE notification_db;
```

## 2. application.properties 설정

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/notification_db
spring.datasource.username=본인계정
spring.datasource.password=비밀번호

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

## 3. 실행

```bash
./gradlew bootRun
```

또는 IntelliJ에서 실행

## 4. Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

---

# 요구사항 해석 및 가정

과제 요구사항을 다음과 같이 해석했습니다.

- 알림 요청 API는 발송 성공 여부를 기다리지 않고 요청 접수만 수행해야 한다.
- 실패한 알림은 단순 무시하지 않고 재시도 가능하도록 저장해야 한다.
- 서버 재시작 이후에도 미처리 알림이 복구 가능해야 한다.
- 동일 이벤트는 중복 저장되지 않아야 한다.
- 실제 메시지 브로커 없이도 향후 Kafka / RabbitMQ 구조로 확장 가능해야 한다.
- 인증은 과제 핵심 요구사항은 아니지만 관리자 기능 보호를 위해 JWT를 적용했다.

---

# 설계 결정과 이유

## 1. DB 기반 비동기 처리 선택

실제 메시지 브로커 없이 구현해야 하는 과제 제약을 고려하여 DB Polling 방식을 선택했습니다.

요청 저장과 발송 처리를 분리하여:

- API 응답 속도 보장
- 서버 장애 시 재처리 가능
- 메시지 브로커 구조로 확장 가능

하도록 설계했습니다.

---

## 2. 상태 기반 처리

알림 상태를 다음처럼 정의했습니다.

```text
REQUESTED
PROCESSING
SENT
FAILED
```

상태 전이를 통해:

- 처리 가능 여부
- 재시도 여부
- 장애 복구 여부

를 판단합니다.

상태 흐름:

```text
REQUESTED
 ↓
PROCESSING
 ↓ 성공
SENT

PROCESSING
 ↓ 실패
FAILED
 ↓ 재시도
PROCESSING
```

---

## 3. 실패 정보 저장

실패 시 단순히 예외를 무시하지 않고 저장합니다.

저장 정보:

- failureReason
- retryCount
- nextRetryAt

---

## 4. 중복 발송 방지

현재 중복 기준:

```text
receiverId + eventId
```

이미 존재하면 저장하지 않습니다.

예:

같은 사용자에게

```text
PAYMENT_SUCCESS
```

알림이 이미 있으면 재요청 시 무시됩니다.

---

## 5. JWT 기반 관리자 인증

관리자 기능 보호를 위해 JWT 인증을 적용했습니다.

JWT 발급:

```http
POST /auth/token
```

요청:

```json
{
  "username":"admin",
  "password":"admin1234"
}
```

JWT에는 사용자 권한이 포함되며:

```text
ROLE_ADMIN
```

필터에서 권한을 복원하여:

```text
/admin/**
```

접근을 제한합니다.

---

# 비동기 처리 구조

```text
Client
 ↓

Notification API
 ↓

DB 저장
REQUESTED
 ↓

Notification Processor
 ↓

PROCESSING
 ↓

Mock Sender
 ↓

SENT / FAILED
```

API는 요청 저장만 수행하고 실제 발송은 별도 처리됩니다.

---

# 재시도 정책

실패 시:

```text
retryCount 증가
failureReason 저장
nextRetryAt 저장
```

재시도 가능 시점 이후 다시 처리 대상이 됩니다.

최대 재시도 초과 시 실패 상태로 유지됩니다.

운영자는 수동 재시도를 수행할 수 있습니다.

---

# 운영 시나리오 대응

## 서버 재시작

DB에 저장된:

```text
REQUESTED
FAILED
PROCESSING
```

상태를 다시 조회하여 재처리할 수 있습니다.

---

## 장애 복구

PROCESSING 상태가 일정 시간 이상 지속되면 장애로 판단하고 재처리 대상에 포함합니다.

---

## 실패 이력 보존

예외를 무시하지 않고:

```text
failureReason
retryCount
```

를 저장합니다.

---

## 수동 재시도

관리자는 실패 알림을 REQUESTED 상태로 변경하여 재처리 가능합니다.

---

# API 목록 및 예시

## 알림 요청

```http
POST /notifications
```

요청:

```json
{
 "receiverId":"user1",
 "eventId":"PAYMENT_SUCCESS",
 "channel":"EMAIL"
}
```

응답:

```text
알림 요청 접수
```

---

## 알림 상태 조회

```http
GET /notifications/{id}
```

---

## 사용자 알림 조회

```http
GET /notifications/users/{receiverId}
```

---

## 읽음 처리

```http
PATCH /notifications/{id}/read
```

---

## 관리자 페이지

```http
GET /admin
Authorization: Bearer {token}
```

---

# 데이터 모델 설명

Notification Entity 주요 필드

| 필드 | 설명 |
|------|------|
| id | 알림 ID |
| receiverId | 수신자 |
| eventId | 이벤트 |
| channel | EMAIL / IN_APP |
| status | 상태 |
| retryCount | 재시도 횟수 |
| failureReason | 실패 이유 |
| nextRetryAt | 재시도 예정 |
| readStatus | 읽음 여부 |
| version | 낙관적 락 |

---

# 테스트 실행 방법

실행:

```bash
./gradlew test
```

현재 테스트 검증 항목:

- REQUESTED 저장
- 중복 방지
- 읽음 처리
- 상태 변경
- 재시도 로직

---

# 미구현 / 제약사항

현재 구현하지 않은 부분:

- 실제 이메일 서버 연동
- Kafka / RabbitMQ
- Redis Lock
- Refresh Token
- 다중 인스턴스 완전 경쟁 제어

향후:

```text
SELECT FOR UPDATE
SKIP LOCKED
Consumer Group
```

등으로 확장 가능

---

# AI 활용 범위

AI를 다음 영역에 활용했습니다.

- 요구사항 해석
- README 구성
- JWT 구조 검토
- 상태 전이 정책 검토
- 테스트 보강 방향 검토

최종 구현 및 검증은 직접 수행했습니다.