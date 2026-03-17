
-- =====================================================================
-- FINAL HOTEL 통합 DDL (Oracle)
-- =====================================================================


-- ===================================================================
-- 0) refresh token 저장 테이블
-- ===================================================================
/*
   [왜 필요한가?]
   - access token 은 만료시간이 짧고,
     refresh token 은 access token 재발급용으로 더 길게 사용한다.
   - refresh token 을 DB에 저장해두면
     (1) 로그아웃 시 서버에서 무효화
     (2) refresh 요청 시 DB 저장값과 비교
     (3) 추후 탈취/재사용 대응 확장
     이 가능해진다.

   [이번 통합본 기준]
   - 요청사항에 따라 tbl_refreshtoken 은 "예전 원본 기준"으로 유지한다.
   - 따라서 principal_type 은 MEMBER / ADMIN 만 허용한다.
*/
CREATE TABLE tbl_refreshtoken (
    refresh_token_id NUMBER PRIMARY KEY,
    principal_type   VARCHAR2(20)   NOT NULL,
    principal_no     NUMBER         NOT NULL,
    login_id         VARCHAR2(100),
    token_value      VARCHAR2(1000) NOT NULL,
    expires_at       DATE           NOT NULL,
    revoked_yn       CHAR(1) DEFAULT 'N' NOT NULL,
    created_at       DATE DEFAULT SYSDATE NOT NULL,
    updated_at       DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT ck_tbl_refreshtoken_type
        CHECK (principal_type IN ('MEMBER', 'ADMIN')),

    CONSTRAINT ck_tbl_refreshtoken_revoked
        CHECK (revoked_yn IN ('Y', 'N'))
);

CREATE SEQUENCE seq_tbl_refreshtoken
START WITH 1
INCREMENT BY 1
NOCACHE;

-- 사용자 1명당 refresh token 1개 운용
CREATE UNIQUE INDEX uq_tbl_refreshtoken_principal
ON tbl_refreshtoken (principal_type, principal_no);

-- refresh token 문자열 조회 성능용
CREATE INDEX idx_tbl_refreshtoken_token_value
ON tbl_refreshtoken (token_value);


-- ===================================================================
-- 1) 호텔(지점) 마스터
-- ===================================================================
/*
   [왜 필요한가?]
   - 호텔/지점의 최상위 기준 테이블이다.
   - 대부분의 도메인(객실/다이닝/셔틀/공지/프로모션)이 fk_hotel_id 로 참조한다.

   [최신 반영 사항]
   - 실제 DB에는 admin_no 컬럼이 추가되어 있다.
   - 현재 덤프상 별도 FK 제약은 보이지 않으므로 컬럼만 유지한다.
*/
CREATE TABLE tbl_hotel (
  hotel_id        NUMBER PRIMARY KEY,
  hotel_name      VARCHAR2(50) NOT NULL,
  address         VARCHAR2(200),
  latitude        NUMBER(10,7),
  longitude       NUMBER(10,7),
  contact         VARCHAR2(50),
  hotel_desc      VARCHAR2(1000),
  approve_status  VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
  reject_reason   VARCHAR2(500),
  active_yn       CHAR(1) DEFAULT 'Y' NOT NULL,
  created_by      VARCHAR2(50),
  created_at      DATE DEFAULT SYSDATE NOT NULL,
  admin_no        NUMBER,

  CONSTRAINT CK_tbl_hotel_active_yn
    CHECK (active_yn IN ('Y','N')),
  CONSTRAINT CK_tbl_hotel_approve_status
    CHECK (approve_status IN ('PENDING','APPROVED','REJECTED'))
);

-- 샘플 데이터
INSERT INTO tbl_hotel (hotel_id, hotel_name) VALUES (1, '호텔 시엘');
INSERT INTO tbl_hotel (hotel_id, hotel_name) VALUES (2, '르시엘');


-- ===================================================================
-- 2) 회원 등급 마스터 + 정책
-- ===================================================================
/*
   [왜 필요한가?]
   - 등급 마스터(코드/명/정렬) + 등급별 혜택/적립율 정책(1:1)
   - 회원(tbl_member_security)의 grade_code 가 이 테이블을 참조한다.
*/
CREATE TABLE tbl_member_grade (
  grade_code   VARCHAR2(20) PRIMARY KEY,
  grade_name   VARCHAR2(20) NOT NULL,
  sort_order   NUMBER NOT NULL
);

CREATE TABLE tbl_member_grade_policy (
  grade_code                  VARCHAR2(20) PRIMARY KEY,
  annual_stay_nights_min      NUMBER NULL,
  valid_points_min            NUMBER NULL,
  room_point_rate_pct         NUMBER(5,2) NOT NULL,
  rooftop_lounge_pool_free_yn CHAR(1) DEFAULT 'N' NOT NULL,
  breakfast_voucher_per_night NUMBER DEFAULT 0 NOT NULL,

  CONSTRAINT FK_grade_policy_grade
    FOREIGN KEY (grade_code) REFERENCES tbl_member_grade(grade_code),

  CONSTRAINT CK_grade_policy_nonneg CHECK (
    (annual_stay_nights_min IS NULL OR annual_stay_nights_min >= 0)
    AND (valid_points_min IS NULL OR valid_points_min >= 0)
    AND room_point_rate_pct >= 0
    AND breakfast_voucher_per_night >= 0
  ),
  CONSTRAINT CK_grade_policy_yn CHECK (rooftop_lounge_pool_free_yn IN ('Y','N'))
);

-- 샘플 데이터
INSERT INTO tbl_member_grade VALUES ('CLASSIC','클래식',1);
INSERT INTO tbl_member_grade VALUES ('SILVER','실버',2);
INSERT INTO tbl_member_grade VALUES ('GOLD','골드',3);
INSERT INTO tbl_member_grade VALUES ('PLATINUM','플래티넘',4);

INSERT INTO tbl_member_grade_policy
(grade_code, annual_stay_nights_min, valid_points_min, room_point_rate_pct, rooftop_lounge_pool_free_yn, breakfast_voucher_per_night)
VALUES ('CLASSIC', NULL, NULL, 3.00, 'N', 0);

INSERT INTO tbl_member_grade_policy VALUES ('SILVER', 5, 1500, 5.00, 'N', 0);
INSERT INTO tbl_member_grade_policy VALUES ('GOLD', 25, 20000, 7.00, 'Y', 0);
INSERT INTO tbl_member_grade_policy VALUES ('PLATINUM', 50, 70000, 10.00, 'Y', 1);


-- ===================================================================
-- 3) 회원 테이블 (MEMBER + GUEST + SOCIAL)
-- ===================================================================
/*
   [왜 필요한가?]
   - 로그인 계정(아이디/비번/활성) + 회원 프로필/포인트 + 등급 정보를 관리한다.
   - MEMBER / GUEST 를 하나의 테이블에서 함께 관리한다.
   - 실제 DB는 social_provider / provider_user_id 를 포함하여
     로컬회원 + 소셜회원 + 게스트를 모두 수용한다.

   [핵심 제약]
   - member_type = MEMBER / GUEST
   - social_provider 와 provider_user_id 는 둘 다 NULL 이거나 둘 다 NOT NULL 이어야 한다.
   - 로컬 회원 : memberid/passwd/email/birthday/grade_code 필요
   - 소셜 회원 : social_provider/provider_user_id/memberid/passwd/grade_code 필요
   - 게스트 : memberid/passwd/email/birthday/grade_code 는 NULL, lookup_key 는 필수

   [email unique 관련 실제 DB 반영]
   - 단순 UNIQUE(email)이 아니라
     "로컬 회원(member_type='MEMBER' and social_provider is null)" 에 대해서만
     email 중복을 막는 함수 기반 unique index 를 사용한다.
*/
CREATE TABLE tbl_member_security(
   member_no              NUMBER NOT NULL,
   memberid               VARCHAR2(50),
   passwd                 VARCHAR2(200),
   enabled                CHAR(1) DEFAULT '1' NOT NULL,
   name                   NVARCHAR2(30) NOT NULL,
   birthday               VARCHAR2(20),
   email                  VARCHAR2(200),
   mobile                 VARCHAR2(200),
   postcode               VARCHAR2(10),
   address                VARCHAR2(200),
   detail_address         VARCHAR2(200),
   extra_address          VARCHAR2(200),
   point                  NUMBER DEFAULT 0 NOT NULL,
   point_earned_total     NUMBER DEFAULT 0 NOT NULL,
   registerday            DATE DEFAULT SYSDATE,
   passwd_modify_date     DATE DEFAULT SYSDATE,
   last_login_date        DATE DEFAULT SYSDATE,
   grade_code             VARCHAR2(20) DEFAULT 'CLASSIC',
   member_type            VARCHAR2(20) DEFAULT 'MEMBER' NOT NULL,
   lookup_key             VARCHAR2(64),
   converted_at           DATE,
   social_provider        VARCHAR2(30),
   provider_user_id       VARCHAR2(200),

   CONSTRAINT PK_tbl_member_security PRIMARY KEY(member_no),
   CONSTRAINT UQ_tbl_member_security_memberid UNIQUE(memberid),
   CONSTRAINT UQ_tbl_member_security_lookup_key UNIQUE(lookup_key),
   CONSTRAINT CK_tbl_member_security_enabled CHECK (enabled IN ('0','1')),
   CONSTRAINT CK_tbl_member_security_point_nonneg CHECK (point >= 0 AND point_earned_total >= 0),
   CONSTRAINT CK_tbl_member_security_type CHECK (member_type IN ('MEMBER','GUEST')),
   CONSTRAINT CK_tbl_member_security_social_pair CHECK (
      (social_provider IS NULL AND provider_user_id IS NULL)
      OR
      (social_provider IS NOT NULL AND provider_user_id IS NOT NULL)
   ),
   CONSTRAINT UQ_tbl_member_security_social UNIQUE (social_provider, provider_user_id),
   CONSTRAINT CK_tbl_member_security_fields CHECK (
     (
       member_type = 'MEMBER'
       AND social_provider IS NULL
       AND provider_user_id IS NULL
       AND memberid IS NOT NULL
       AND passwd   IS NOT NULL
       AND email    IS NOT NULL
       AND birthday IS NOT NULL
       AND grade_code IS NOT NULL
     )
     OR
     (
       member_type = 'MEMBER'
       AND social_provider IS NOT NULL
       AND provider_user_id IS NOT NULL
       AND memberid IS NOT NULL
       AND passwd   IS NOT NULL
       AND grade_code IS NOT NULL
     )
     OR
     (
       member_type = 'GUEST'
       AND social_provider IS NULL
       AND provider_user_id IS NULL
       AND memberid IS NULL
       AND passwd   IS NULL
       AND email    IS NULL
       AND birthday IS NULL
       AND grade_code IS NULL
       AND lookup_key IS NOT NULL
     )
   ),
   CONSTRAINT FK_member_security_grade
     FOREIGN KEY (grade_code) REFERENCES tbl_member_grade(grade_code)
);

CREATE SEQUENCE seq_tbl_member_security
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

-- 로컬 회원(local account) 에 대해서만 email 중복 방지
CREATE UNIQUE INDEX UX_tbl_member_security_local_email
ON tbl_member_security (
  CASE
    WHEN (member_type = 'MEMBER' AND social_provider IS NULL)
    THEN email
  END
);


-- ===================================================================
-- 4) 관리자 테이블
-- ===================================================================
/*
   [왜 필요한가?]
   - 관리자 로그인 계정과 권한 부여의 기준 테이블이다.
   - HQ(총괄) / BRANCH(지점) 관리자를 구분한다.
   - BRANCH 관리자는 반드시 담당 호텔(fk_hotel_id)을 가진다.
*/
CREATE TABLE tbl_admin_security(
   admin_no              NUMBER NOT NULL,
   adminid               VARCHAR2(50)   NOT NULL,
   passwd                VARCHAR2(200)  NOT NULL,
   enabled               CHAR(1) DEFAULT '1' NOT NULL,
   name                  NVARCHAR2(30) NOT NULL,
   email                 VARCHAR2(200) NOT NULL,
   mobile                VARCHAR2(200),
   admin_type            VARCHAR2(20) NOT NULL,
   fk_hotel_id           NUMBER,
   registerday           DATE DEFAULT SYSDATE,
   passwd_modify_date    DATE DEFAULT SYSDATE,
   last_login_date       DATE DEFAULT SYSDATE,

   CONSTRAINT PK_tbl_admin_security PRIMARY KEY(admin_no),
   CONSTRAINT UQ_tbl_admin_security_adminid UNIQUE(adminid),
   CONSTRAINT UQ_tbl_admin_security_email UNIQUE(email),
   CONSTRAINT CK_tbl_admin_security_enabled CHECK (enabled IN ('0','1')),
   CONSTRAINT CK_tbl_admin_security_type CHECK (admin_type IN ('HQ','BRANCH')),
   CONSTRAINT CK_tbl_admin_security_hotel_rule CHECK (
        (admin_type = 'HQ' AND fk_hotel_id IS NULL)
     OR (admin_type = 'BRANCH' AND fk_hotel_id IS NOT NULL)
   ),
   CONSTRAINT FK_tbl_admin_security_hotel
     FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE SEQUENCE seq_tbl_admin_security
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 5) 회원 권한
-- ===================================================================
CREATE TABLE tbl_member_authorities (
   member_auth_no  NUMBER NOT NULL,
   member_no       NUMBER NOT NULL,
   authority       VARCHAR2(50) NOT NULL,

   CONSTRAINT PK_tbl_member_authorities PRIMARY KEY(member_auth_no),
   CONSTRAINT UQ_tbl_member_authorities UNIQUE(member_no, authority),
   CONSTRAINT FK_tbl_member_authorities_member
     FOREIGN KEY(member_no) REFERENCES tbl_member_security(member_no) ON DELETE CASCADE,
   CONSTRAINT CK_tbl_member_authorities_prefix
     CHECK (authority LIKE 'ROLE\_%' ESCAPE '\')
);

CREATE SEQUENCE seq_tbl_member_authorities
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 6) 관리자 권한
-- ===================================================================
CREATE TABLE tbl_admin_authorities (
   admin_auth_no   NUMBER NOT NULL,
   admin_no        NUMBER NOT NULL,
   authority       VARCHAR2(50) NOT NULL,

   CONSTRAINT PK_tbl_admin_authorities PRIMARY KEY(admin_auth_no),
   CONSTRAINT UQ_tbl_admin_authorities UNIQUE(admin_no, authority),
   CONSTRAINT FK_tbl_admin_authorities_admin
     FOREIGN KEY(admin_no) REFERENCES tbl_admin_security(admin_no) ON DELETE CASCADE,
   CONSTRAINT CK_tbl_admin_authorities_prefix
     CHECK (authority LIKE 'ROLE\_%' ESCAPE '\')
);

CREATE SEQUENCE seq_tbl_admin_authorities
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 7) 로그인 히스토리(회원만)
-- ===================================================================
CREATE TABLE tbl_loginhistory (
  historyno   NUMBER NOT NULL,
  member_no   NUMBER NOT NULL,
  logindate   DATE DEFAULT SYSDATE NOT NULL,
  clientip    VARCHAR2(45) NOT NULL,

  CONSTRAINT PK_tbl_loginhistory PRIMARY KEY(historyno),
  CONSTRAINT FK_tbl_loginhistory_member
    FOREIGN KEY(member_no) REFERENCES tbl_member_security(member_no)
);

CREATE SEQUENCE seq_historyno
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 8) 호텔 이미지
-- ===================================================================
CREATE TABLE HOTEL_IMAGE (
    image_id      NUMBER PRIMARY KEY,
    fk_hotel_id   NUMBER NOT NULL,
    image_url     VARCHAR2(500) NOT NULL,
    is_main       CHAR(1) DEFAULT 'N',
    sort_order    NUMBER DEFAULT 1,

    CONSTRAINT ck_hotel_image_main CHECK (is_main IN ('Y','N')),
    CONSTRAINT fk_img_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE SEQUENCE SEQ_HOTEL_IMAGE START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 9) 객실 도메인
-- ===================================================================
/*
   [최신 반영 사항]
   - ROOM_TYPE 에 approve_status 추가
   - ROOM_APPROVAL_HISTORY / ROOM_VIEW_HISTORY 테이블 반영
   - SEASON 에 priority 추가
*/
CREATE TABLE ROOM_TYPE (
    room_type_id    NUMBER PRIMARY KEY,
    fk_hotel_id     NUMBER NOT NULL,
    room_grade      VARCHAR2(100) NOT NULL,
    bed_type        VARCHAR2(50) NOT NULL,
    view_type       VARCHAR2(50) NOT NULL,
    room_name       VARCHAR2(200) NOT NULL,
    room_size       NUMBER,
    max_capacity    NUMBER NOT NULL,
    total_count     NUMBER NOT NULL,
    base_price      NUMBER NOT NULL,
    is_active       CHAR(1) DEFAULT 'Y',
    approve_status  VARCHAR2(20) DEFAULT 'PENDING',

    CONSTRAINT fk_roomtype_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),
    CONSTRAINT ck_roomtype_active CHECK (is_active IN ('Y','N'))
);

CREATE SEQUENCE SEQ_ROOM_TYPE START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE ROOM_OPTION (
    option_id      NUMBER PRIMARY KEY,
    room_type_id   NUMBER NOT NULL,
    option_name    VARCHAR2(100) NOT NULL,
    extra_price    NUMBER DEFAULT 0,
    price_type     VARCHAR2(20),

    CONSTRAINT fk_option_room_type FOREIGN KEY (room_type_id) REFERENCES ROOM_TYPE(room_type_id)
);

CREATE SEQUENCE SEQ_ROOM_OPTION START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE ROOM_IMAGE (
    image_id      NUMBER PRIMARY KEY,
    room_type_id  NUMBER NOT NULL,
    image_url     VARCHAR2(500) NOT NULL,
    is_main       CHAR(1) DEFAULT 'N',
    sort_order    NUMBER DEFAULT 1,

    CONSTRAINT ck_room_image_main CHECK (is_main IN ('Y','N')),
    CONSTRAINT fk_img_room FOREIGN KEY (room_type_id) REFERENCES ROOM_TYPE(room_type_id)
);

CREATE SEQUENCE SEQ_ROOM_IMAGE START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE SEASON (
    season_id   NUMBER PRIMARY KEY,
    season_name VARCHAR2(50) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    price_rate  NUMBER(4,2) NOT NULL,
    priority    NUMBER DEFAULT 1,

    CONSTRAINT ck_season_date CHECK (end_date >= start_date)
);

CREATE SEQUENCE SEQ_SEASON START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE WEEKDAY_RATE (
    weekday_id       NUMBER PRIMARY KEY,
    fk_hotel_id      NUMBER NOT NULL,
    day_of_week      NUMBER NOT NULL,
    rate_multiplier  NUMBER(4,2) NOT NULL,

    CONSTRAINT fk_weekday_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),
    CONSTRAINT ck_weekday_range CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT uk_weekday UNIQUE (fk_hotel_id, day_of_week)
);

CREATE SEQUENCE SEQ_WEEKDAY_RATE START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE ROOM_STOCK (
    stock_id        NUMBER PRIMARY KEY,
    room_type_id    NUMBER NOT NULL,
    stay_date       DATE NOT NULL,
    available_count NUMBER NOT NULL,
    price_override  NUMBER,
    is_closed       CHAR(1) DEFAULT 'N',
    min_stay        NUMBER DEFAULT 1,

    CONSTRAINT fk_stock_room_type FOREIGN KEY (room_type_id) REFERENCES ROOM_TYPE(room_type_id),
    CONSTRAINT uk_room_date UNIQUE (room_type_id, stay_date),
    CONSTRAINT ck_stock_non_negative CHECK (available_count >= 0),
    CONSTRAINT ck_stock_closed CHECK (is_closed IN ('Y','N'))
);

CREATE SEQUENCE SEQ_ROOM_STOCK START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE ROOM_APPROVAL_HISTORY (
    history_id       NUMBER PRIMARY KEY,
    fk_room_type_id  NUMBER NOT NULL,
    status           VARCHAR2(30) NOT NULL,
    reason           VARCHAR2(500),
    decided_by       NUMBER,
    decided_at       DATE DEFAULT SYSDATE,

    CONSTRAINT CK_room_history_status CHECK (status IN ('DRAFT','PENDING','NEED_REVISION','APPROVED','REJECTED')),
    CONSTRAINT FK_room_history_admin FOREIGN KEY (decided_by) REFERENCES tbl_admin_security(admin_no),
    CONSTRAINT FK_room_history_room FOREIGN KEY (fk_room_type_id) REFERENCES ROOM_TYPE(room_type_id)
);

CREATE SEQUENCE SEQ_ROOM_APPROVAL_HISTORY START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE ROOM_VIEW_HISTORY (
    history_id    NUMBER PRIMARY KEY,
    member_no     NUMBER NOT NULL,
    room_type_id  NUMBER NOT NULL,
    viewed_at     DATE DEFAULT SYSDATE,

    CONSTRAINT FK_room_view_member FOREIGN KEY (member_no) REFERENCES tbl_member_security(member_no)
);

CREATE SEQUENCE SEQ_ROOM_VIEW_HISTORY START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 10) 숙박 결제/예약
-- ===================================================================
CREATE TABLE PAYMENT (
    payment_id       NUMBER PRIMARY KEY,
    member_no        NUMBER NOT NULL,
    payment_amount   NUMBER NOT NULL,
    payment_method   VARCHAR2(50),
    payment_status   VARCHAR2(30) DEFAULT 'READY',
    imp_uid          VARCHAR2(200),
    paid_at          DATE,
    created_at       DATE DEFAULT SYSDATE,
    refunded_amount  NUMBER DEFAULT 0,

    CONSTRAINT ck_payment_status CHECK (payment_status IN ('READY','PAID','FAILED','CANCELLED','PARTIAL_CANCEL')),
    CONSTRAINT fk_payment_member FOREIGN KEY (member_no) REFERENCES tbl_member_security(member_no)
);

CREATE SEQUENCE SEQ_PAYMENT START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE RESERVATION (
    reservation_id     NUMBER PRIMARY KEY,
    member_no          NUMBER NOT NULL,
    room_type_id       NUMBER NOT NULL,
    payment_id         NUMBER,
    checkin_date       DATE NOT NULL,
    checkout_date      DATE NOT NULL,
    guest_count        NUMBER NOT NULL,
    reservation_code   VARCHAR2(50) UNIQUE,
    reservation_status VARCHAR2(30) DEFAULT 'PENDING',
    hold_expires_at    DATE,
    total_price        NUMBER NOT NULL,
    cancel_deadline    DATE,
    refund_amount      NUMBER,
    created_at         DATE DEFAULT SYSDATE,

    CONSTRAINT ck_reservation_status CHECK (reservation_status IN ('PENDING','CONFIRMED','CANCELLED','EXPIRED','CHECKED_IN','CHECKED_OUT','NO_SHOW')),
    CONSTRAINT ck_guest_count CHECK (guest_count >= 1),
    CONSTRAINT ck_date CHECK (checkout_date > checkin_date),
    CONSTRAINT fk_res_member FOREIGN KEY (member_no) REFERENCES tbl_member_security(member_no),
    CONSTRAINT fk_res_room FOREIGN KEY (room_type_id) REFERENCES ROOM_TYPE(room_type_id),
    CONSTRAINT fk_res_payment FOREIGN KEY (payment_id) REFERENCES PAYMENT(payment_id)
);

CREATE SEQUENCE SEQ_RESERVATION START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE RESERVATION_OPTION (
    reservation_option_id NUMBER PRIMARY KEY,
    reservation_id        NUMBER NOT NULL,
    option_id             NUMBER NOT NULL,
    option_count          NUMBER DEFAULT 1,

    CONSTRAINT fk_res_opt_res FOREIGN KEY (reservation_id) REFERENCES RESERVATION(reservation_id),
    CONSTRAINT fk_res_opt_opt FOREIGN KEY (option_id) REFERENCES ROOM_OPTION(option_id)
);

CREATE SEQUENCE SEQ_RESERVATION_OPTION START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE PAYMENT_REFUND (
    refund_id        NUMBER PRIMARY KEY,
    payment_id       NUMBER NOT NULL,
    reservation_id   NUMBER NOT NULL,
    refund_amount    NUMBER NOT NULL,
    refund_type      VARCHAR2(20),
    refunded_at      DATE DEFAULT SYSDATE,

    CONSTRAINT ck_refund_type CHECK (refund_type IN ('FULL','PARTIAL')),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES PAYMENT(payment_id),
    CONSTRAINT fk_refund_res FOREIGN KEY (reservation_id) REFERENCES RESERVATION(reservation_id)
);

CREATE SEQUENCE SEQ_PAYMENT_REFUND START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 11) SHUTTLE
-- ===================================================================

/*
   [구조 설명]
   - place        : 출발지/도착지 마스터
   - route        : 호텔별 셔틀 구간(예: 서울역 -> 호텔시엘, 호텔시엘 -> 서울역)
   - timetable    : route 별 시간표
   - block        : 특정 노선/특정 시간표의 특정 기간 운행 차단
   - slot_stock   : 날짜별 좌석 재고
   - booking      : 예약 단위 헤더
   - booking_leg  : 실제 탑승 레그(TO/FROM 다중 가능)

   [이번 반영 사항]
   - HQ 운영자가 호텔별 셔틀 구간(route)을 직접 추가/비활성화 가능
   - 특정 일자/기간 동안 route 전체 또는 특정 timetable 차단 가능
   - 미래 slot_stock 기간 연장 가능
   - 1달 이전 과거 셔틀 데이터 삭제 가능
*/


-- ================================================================
-- 11-1. 셔틀 장소 마스터
-- ================================================================
CREATE TABLE tbl_shuttle_place (
  place_code   VARCHAR2(30) PRIMARY KEY,
  place_name   NVARCHAR2(50) NOT NULL,
  active_yn    CHAR(1) DEFAULT 'Y' NOT NULL,

  CONSTRAINT CK_shuttle_place_active_yn
    CHECK (active_yn IN ('Y','N'))
);

-- 샘플 장소
INSERT INTO tbl_shuttle_place (place_code, place_name, active_yn) VALUES ('SEOUL_STATION', '서울역', 'Y');
INSERT INTO tbl_shuttle_place (place_code, place_name, active_yn) VALUES ('GIMPO', '김포공항', 'Y');
INSERT INTO tbl_shuttle_place (place_code, place_name, active_yn) VALUES ('INCHEON', '인천공항', 'Y');


-- ================================================================
-- 11-2. 셔틀 구간(route)
-- ================================================================
CREATE TABLE tbl_shuttle_route (
  route_id           NUMBER PRIMARY KEY,
  fk_hotel_id        NUMBER NOT NULL,
  route_type         VARCHAR2(20) NOT NULL,
  start_place_code   VARCHAR2(30) NOT NULL,
  end_place_code     VARCHAR2(30) NOT NULL,
  route_name         VARCHAR2(100) NOT NULL,
  active_yn          CHAR(1) DEFAULT 'Y' NOT NULL,
  created_at         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

  CONSTRAINT FK_shuttle_route_hotel
    FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),

  CONSTRAINT FK_shuttle_route_start_place
    FOREIGN KEY (start_place_code) REFERENCES tbl_shuttle_place(place_code),

  CONSTRAINT FK_shuttle_route_end_place
    FOREIGN KEY (end_place_code) REFERENCES tbl_shuttle_place(place_code),

  CONSTRAINT CK_shuttle_route_type
    CHECK (route_type IN ('TO_HOTEL','FROM_HOTEL')),

  CONSTRAINT CK_shuttle_route_active_yn
    CHECK (active_yn IN ('Y','N')),

  CONSTRAINT CK_shuttle_route_diff_place
    CHECK (start_place_code <> end_place_code)
);

CREATE SEQUENCE seq_tbl_shuttle_route
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE UNIQUE INDEX UQ_shuttle_route_key
ON tbl_shuttle_route (fk_hotel_id, route_type, start_place_code, end_place_code);

CREATE INDEX IX_shuttle_route_hotel
ON tbl_shuttle_route (fk_hotel_id, route_type, active_yn);


-- ================================================================
-- 11-3. 기존 호텔을 종점/기점 place 로도 관리
--       route 구조에서는 호텔도 place 개념이 필요하므로 별도 코드 부여
-- ================================================================
MERGE INTO tbl_shuttle_place p
USING (
  SELECT 'HOTEL_' || hotel_id AS place_code,
         hotel_name || ' 셔틀승하차장' AS place_name
  FROM tbl_hotel
) src
ON (p.place_code = src.place_code)
WHEN NOT MATCHED THEN
  INSERT (place_code, place_name, active_yn)
  VALUES (src.place_code, src.place_name, 'Y');


-- ================================================================
-- 11-4. route 샘플 시드
--       호텔 1: 시엘 / 호텔 2: 르시엘 기준
-- ================================================================

-- 호텔 1 (시엘)
INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 1, 'TO_HOTEL',   'SEOUL_STATION', 'HOTEL_1', '서울역 -> 호텔 시엘', 'Y');

INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 1, 'TO_HOTEL',   'GIMPO',         'HOTEL_1', '김포공항 -> 호텔 시엘', 'Y');

INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 1, 'FROM_HOTEL', 'HOTEL_1',       'SEOUL_STATION', '호텔 시엘 -> 서울역', 'Y');

INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 1, 'FROM_HOTEL', 'HOTEL_1',       'INCHEON', '호텔 시엘 -> 인천공항', 'Y');

-- 호텔 2 (르시엘)
INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 2, 'TO_HOTEL',   'SEOUL_STATION', 'HOTEL_2', '서울역 -> 르시엘', 'Y');

INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 2, 'TO_HOTEL',   'GIMPO',         'HOTEL_2', '김포공항 -> 르시엘', 'Y');

INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 2, 'FROM_HOTEL', 'HOTEL_2',       'SEOUL_STATION', '르시엘 -> 서울역', 'Y');

INSERT INTO tbl_shuttle_route
(route_id, fk_hotel_id, route_type, start_place_code, end_place_code, route_name, active_yn)
VALUES (seq_tbl_shuttle_route.NEXTVAL, 2, 'FROM_HOTEL', 'HOTEL_2',       'INCHEON', '르시엘 -> 인천공항', 'Y');


-- ================================================================
-- 11-5. 시간표(timetable)
-- ================================================================
CREATE TABLE tbl_shuttle_timetable (
  timetable_id   NUMBER PRIMARY KEY,
  fk_route_id    NUMBER NOT NULL,
  fk_hotel_id    NUMBER NOT NULL,
  leg_type       VARCHAR2(20) NOT NULL,
  place_code     VARCHAR2(30) NOT NULL,
  depart_time    VARCHAR2(5) NOT NULL,
  capacity       NUMBER NOT NULL,
  active_yn      CHAR(1) DEFAULT 'Y' NOT NULL,
  created_at     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

  CONSTRAINT FK_shuttle_timetable_route
    FOREIGN KEY (fk_route_id) REFERENCES tbl_shuttle_route(route_id),

  CONSTRAINT FK_shuttle_timetable_hotel
    FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),

  CONSTRAINT FK_shuttle_timetable_place
    FOREIGN KEY (place_code) REFERENCES tbl_shuttle_place(place_code),

  CONSTRAINT CK_shuttle_timetable_leg_type
    CHECK (leg_type IN ('TO_HOTEL','FROM_HOTEL')),

  CONSTRAINT CK_shuttle_timetable_depart_time
    CHECK (REGEXP_LIKE(depart_time, '^[0-2][0-9]:[0-5][0-9]$')),

  CONSTRAINT CK_shuttle_timetable_capacity
    CHECK (capacity > 0),

  CONSTRAINT CK_shuttle_timetable_active_yn
    CHECK (active_yn IN ('Y','N'))
);

CREATE SEQUENCE seq_tbl_shuttle_timetable
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE UNIQUE INDEX UQ_shuttle_timetable_key
ON tbl_shuttle_timetable (fk_route_id, depart_time);

CREATE INDEX IX_shuttle_timetable_hotel
ON tbl_shuttle_timetable (fk_hotel_id, leg_type);

CREATE INDEX IX_shuttle_timetable_route
ON tbl_shuttle_timetable (fk_route_id, active_yn, depart_time);


-- ================================================================
-- 11-6. 시간표 샘플 시드
-- ================================================================
-- 호텔 1(시엘)
INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, start_place_code, '12:00', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 1 AND route_type = 'TO_HOTEL' AND start_place_code = 'SEOUL_STATION';

INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, start_place_code, '13:00', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 1 AND route_type = 'TO_HOTEL' AND start_place_code = 'GIMPO';

INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, end_place_code, '13:00', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 1 AND route_type = 'FROM_HOTEL' AND end_place_code = 'SEOUL_STATION';

INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, end_place_code, '14:00', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 1 AND route_type = 'FROM_HOTEL' AND end_place_code = 'INCHEON';

-- 호텔 2(르시엘)
INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, start_place_code, '12:30', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 2 AND route_type = 'TO_HOTEL' AND start_place_code = 'SEOUL_STATION';

INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, start_place_code, '13:30', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 2 AND route_type = 'TO_HOTEL' AND start_place_code = 'GIMPO';

INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, end_place_code, '13:30', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 2 AND route_type = 'FROM_HOTEL' AND end_place_code = 'SEOUL_STATION';

INSERT INTO tbl_shuttle_timetable
(timetable_id, fk_route_id, fk_hotel_id, leg_type, place_code, depart_time, capacity, active_yn)
SELECT seq_tbl_shuttle_timetable.NEXTVAL, route_id, fk_hotel_id, route_type, end_place_code, '14:30', 20, 'Y'
FROM tbl_shuttle_route
WHERE fk_hotel_id = 2 AND route_type = 'FROM_HOTEL' AND end_place_code = 'INCHEON';


-- ================================================================
-- 11-7. 셔틀 차단(block)
-- ================================================================
CREATE TABLE tbl_shuttle_block (
  shuttle_block_id   NUMBER PRIMARY KEY,
  fk_hotel_id        NUMBER NOT NULL,
  fk_route_id        NUMBER NOT NULL,
  fk_timetable_id    NUMBER,
  block_start_date   DATE NOT NULL,
  block_end_date     DATE NOT NULL,
  reason             VARCHAR2(300),
  active_yn          CHAR(1) DEFAULT 'Y' NOT NULL,
  created_by_admin   NUMBER NOT NULL,
  created_at         TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

  CONSTRAINT FK_shuttle_block_hotel
    FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),

  CONSTRAINT FK_shuttle_block_route
    FOREIGN KEY (fk_route_id) REFERENCES tbl_shuttle_route(route_id),

  CONSTRAINT FK_shuttle_block_timetable
    FOREIGN KEY (fk_timetable_id) REFERENCES tbl_shuttle_timetable(timetable_id),

  CONSTRAINT FK_shuttle_block_admin
    FOREIGN KEY (created_by_admin) REFERENCES tbl_admin_security(admin_no),

  CONSTRAINT CK_shuttle_block_active_yn
    CHECK (active_yn IN ('Y','N')),

  CONSTRAINT CK_shuttle_block_date
    CHECK (block_end_date >= block_start_date)
);

CREATE SEQUENCE seq_tbl_shuttle_block
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE INDEX IX_shuttle_block_route_date
ON tbl_shuttle_block (fk_route_id, block_start_date, block_end_date, active_yn);

CREATE INDEX IX_shuttle_block_timetable_date
ON tbl_shuttle_block (fk_timetable_id, block_start_date, block_end_date, active_yn);


-- ================================================================
-- 11-8. 날짜별 재고(slot_stock)
-- ================================================================
CREATE TABLE tbl_shuttle_slot_stock (
  stock_id        NUMBER PRIMARY KEY,
  fk_timetable_id NUMBER NOT NULL,
  ride_date       DATE NOT NULL,
  capacity        NUMBER NOT NULL,
  booked_qty      NUMBER DEFAULT 0 NOT NULL,
  updated_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,

  CONSTRAINT FK_shuttle_stock_timetable
    FOREIGN KEY (fk_timetable_id) REFERENCES tbl_shuttle_timetable(timetable_id),

  CONSTRAINT CK_shuttle_stock_capacity
    CHECK (capacity > 0),

  CONSTRAINT CK_shuttle_stock_booked_qty
    CHECK (booked_qty >= 0 AND booked_qty <= capacity)
);

CREATE SEQUENCE seq_tbl_shuttle_slot_stock
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE UNIQUE INDEX UQ_shuttle_stock_key
ON tbl_shuttle_slot_stock(fk_timetable_id, ride_date);

CREATE INDEX IX_shuttle_stock_date
ON tbl_shuttle_slot_stock(ride_date);


-- ================================================================
-- 11-9. 초기 90일 재고 생성
-- ================================================================
INSERT INTO tbl_shuttle_slot_stock
(stock_id, fk_timetable_id, ride_date, capacity, booked_qty, updated_at)
SELECT
    seq_tbl_shuttle_slot_stock.NEXTVAL,
    t.timetable_id,
    TRUNC(SYSDATE) + d.lv - 1,
    t.capacity,
    0,
    SYSTIMESTAMP
FROM tbl_shuttle_timetable t
CROSS JOIN (
    SELECT LEVEL lv
    FROM dual
    CONNECT BY LEVEL <= 90
) d
WHERE t.active_yn = 'Y';


-- ================================================================
-- 11-10. 셔틀 예약 헤더
-- ================================================================
CREATE TABLE tbl_shuttle_booking (
  shuttle_booking_id  NUMBER PRIMARY KEY,
  fk_reservation_id   NUMBER NOT NULL,
  fk_hotel_id         NUMBER NOT NULL,
  fk_member_no        NUMBER NOT NULL,
  ride_date           DATE NOT NULL,
  status              VARCHAR2(20) DEFAULT 'BOOKED' NOT NULL,
  created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  canceled_at         TIMESTAMP NULL,

  CONSTRAINT FK_shuttle_booking_reservation
    FOREIGN KEY (fk_reservation_id) REFERENCES reservation(reservation_id),

  CONSTRAINT FK_shuttle_booking_member
    FOREIGN KEY (fk_member_no) REFERENCES tbl_member_security(member_no),

  CONSTRAINT FK_shuttle_booking_hotel
    FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),

  CONSTRAINT CK_shuttle_booking_status
    CHECK (status IN ('BOOKED','CANCELED'))
);

CREATE SEQUENCE seq_tbl_shuttle_booking
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE UNIQUE INDEX UQ_shuttle_booking_reservation
ON tbl_shuttle_booking(fk_reservation_id);

CREATE INDEX IX_shuttle_booking_member_date
ON tbl_shuttle_booking(fk_member_no, ride_date);


-- ================================================================
-- 11-11. 셔틀 예약 레그
-- ================================================================
CREATE TABLE tbl_shuttle_booking_leg (
  shuttle_leg_id        NUMBER PRIMARY KEY,
  fk_shuttle_booking_id NUMBER NOT NULL,
  fk_timetable_id       NUMBER NOT NULL,
  leg_type              VARCHAR2(20) NOT NULL,
  place_code            VARCHAR2(30) NOT NULL,
  depart_time           VARCHAR2(5) NOT NULL,
  ticket_qty            NUMBER NOT NULL,
  leg_status            VARCHAR2(20) DEFAULT 'BOOKED' NOT NULL,
  created_at            TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  canceled_at           TIMESTAMP NULL,
  ride_date             DATE NOT NULL,

  CONSTRAINT FK_shuttle_leg_booking
    FOREIGN KEY (fk_shuttle_booking_id)
    REFERENCES tbl_shuttle_booking(shuttle_booking_id)
    ON DELETE CASCADE,

  CONSTRAINT FK_shuttle_leg_timetable
    FOREIGN KEY (fk_timetable_id) REFERENCES tbl_shuttle_timetable(timetable_id),

  CONSTRAINT FK_shuttle_leg_place
    FOREIGN KEY (place_code) REFERENCES tbl_shuttle_place(place_code),

  CONSTRAINT CK_shuttle_leg_type
    CHECK (leg_type IN ('TO_HOTEL','FROM_HOTEL')),

  CONSTRAINT CK_shuttle_leg_depart_time
    CHECK (REGEXP_LIKE(depart_time, '^[0-2][0-9]:[0-5][0-9]$')),

  CONSTRAINT CK_shuttle_leg_ticket_qty
    CHECK (ticket_qty > 0),

  CONSTRAINT CK_shuttle_leg_status
    CHECK (leg_status IN ('BOOKED','CANCELED'))
);

CREATE SEQUENCE seq_tbl_shuttle_booking_leg
START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE UNIQUE INDEX UQ_shuttle_leg_per_timetable
ON tbl_shuttle_booking_leg(fk_shuttle_booking_id, fk_timetable_id, ride_date);

CREATE INDEX IX_shuttle_leg_timetable
ON tbl_shuttle_booking_leg(fk_timetable_id);

CREATE INDEX IX_shuttle_leg_ride_date
ON tbl_shuttle_booking_leg(ride_date);

CREATE INDEX IX_shuttle_leg_booking_status
ON tbl_shuttle_booking_leg(fk_shuttle_booking_id, leg_status);


-- ================================================================
-- 11-12. 사용자 확인 페이지 카드용 뷰
-- ================================================================
CREATE OR REPLACE VIEW vw_my_shuttle_reservation_card AS
SELECT
  b.shuttle_booking_id,
  b.fk_reservation_id,
  b.fk_hotel_id,
  b.fk_member_no,
  b.status AS booking_status,
  b.created_at,
  b.canceled_at,
  to_leg.to_ride_date,
  to_leg.to_summary,
  to_leg.to_total_qty,
  from_leg.from_ride_date,
  from_leg.from_summary,
  from_leg.from_total_qty
FROM tbl_shuttle_booking b
LEFT JOIN (
  SELECT
    fk_shuttle_booking_id,
    MIN(ride_date) AS to_ride_date,
    SUM(ticket_qty) AS to_total_qty,
    LISTAGG(place_code || ' ' || depart_time || '(' || ticket_qty || ')', ', ')
      WITHIN GROUP (ORDER BY depart_time) AS to_summary
  FROM tbl_shuttle_booking_leg
  WHERE leg_type = 'TO_HOTEL'
    AND leg_status = 'BOOKED'
  GROUP BY fk_shuttle_booking_id
) to_leg
ON to_leg.fk_shuttle_booking_id = b.shuttle_booking_id
LEFT JOIN (
  SELECT
    fk_shuttle_booking_id,
    MIN(ride_date) AS from_ride_date,
    SUM(ticket_qty) AS from_total_qty,
    LISTAGG(place_code || ' ' || depart_time || '(' || ticket_qty || ')', ', ')
      WITHIN GROUP (ORDER BY depart_time) AS from_summary
  FROM tbl_shuttle_booking_leg
  WHERE leg_type = 'FROM_HOTEL'
    AND leg_status = 'BOOKED'
  GROUP BY fk_shuttle_booking_id
) from_leg
ON from_leg.fk_shuttle_booking_id = b.shuttle_booking_id;


-- ================================================================
-- 11-13. reservation 취소 시 셔틀 자동 취소 + 재고 복구 트리거
-- ================================================================
CREATE OR REPLACE TRIGGER trg_reservation_cancel_shuttle
AFTER UPDATE OF reservation_status ON reservation
FOR EACH ROW
WHEN (NEW.reservation_status = 'CANCELLED' AND NVL(OLD.reservation_status,'_') <> 'CANCELLED')
DECLARE
  v_booking_id tbl_shuttle_booking.shuttle_booking_id%TYPE;
BEGIN
  BEGIN
    SELECT shuttle_booking_id
      INTO v_booking_id
      FROM tbl_shuttle_booking
     WHERE fk_reservation_id = :NEW.reservation_id;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RETURN;
  END;

  FOR r IN (
    SELECT shuttle_leg_id, fk_timetable_id, ride_date, ticket_qty
      FROM tbl_shuttle_booking_leg
     WHERE fk_shuttle_booking_id = v_booking_id
       AND leg_status = 'BOOKED'
  ) LOOP

    UPDATE tbl_shuttle_slot_stock s
       SET s.booked_qty = CASE
                             WHEN s.booked_qty - r.ticket_qty < 0 THEN 0
                             ELSE s.booked_qty - r.ticket_qty
                          END,
           s.updated_at = SYSTIMESTAMP
     WHERE s.fk_timetable_id = r.fk_timetable_id
       AND s.ride_date = TRUNC(r.ride_date);

    UPDATE tbl_shuttle_booking_leg
       SET leg_status  = 'CANCELED',
           canceled_at = SYSTIMESTAMP
     WHERE shuttle_leg_id = r.shuttle_leg_id
       AND leg_status = 'BOOKED';
  END LOOP;

  UPDATE tbl_shuttle_booking
     SET status      = 'CANCELED',
         canceled_at = SYSTIMESTAMP
   WHERE shuttle_booking_id = v_booking_id
     AND status <> 'CANCELED';
END;
/
SHOW ERRORS;


-- ================================================================
-- 11-14. HQ 관리용 route 상세 조회 뷰
-- ================================================================
CREATE OR REPLACE VIEW vw_shuttle_route_detail AS
SELECT
    r.route_id,
    r.fk_hotel_id,
    h.hotel_name,
    r.route_type,
    r.start_place_code,
    sp.place_name AS start_place_name,
    r.end_place_code,
    ep.place_name AS end_place_name,
    r.route_name,
    r.active_yn,
    r.created_at
FROM tbl_shuttle_route r
JOIN tbl_hotel h
  ON h.hotel_id = r.fk_hotel_id
JOIN tbl_shuttle_place sp
  ON sp.place_code = r.start_place_code
JOIN tbl_shuttle_place ep
  ON ep.place_code = r.end_place_code;


COMMENT ON TABLE tbl_shuttle_route IS '호텔별 셔틀 구간(노선) 관리';
COMMENT ON COLUMN tbl_shuttle_route.route_type IS 'TO_HOTEL / FROM_HOTEL';
COMMENT ON COLUMN tbl_shuttle_route.start_place_code IS '출발지 place_code';
COMMENT ON COLUMN tbl_shuttle_route.end_place_code IS '도착지 place_code';

COMMENT ON TABLE tbl_shuttle_block IS '특정 일자/기간 셔틀 운행 차단';
COMMENT ON COLUMN tbl_shuttle_block.fk_route_id IS '차단 대상 route';
COMMENT ON COLUMN tbl_shuttle_block.fk_timetable_id IS '특정 시간표만 차단할 경우 사용, route 전체 차단이면 NULL 가능';


COMMIT;


-- ===================================================================
-- 12) DINING / OUTLET
-- ===================================================================
/*
   [최신 반영 사항]
   - 실제 DB에는 TBL_DINING, OUTLETS, DINING_TABLES, DINING_RESERVATIONS 가 함께 존재한다.
   - Dining_Reservations 는 DINING_ID / RES_PASSWORD / GUEST_EMAIL 을 포함한다.
*/
CREATE TABLE OUTLETS (
    outlet_id       NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    hotel_id        NUMBER NOT NULL,
    name            VARCHAR2(100) NOT NULL,
    outlet_type     VARCHAR2(20) NOT NULL,
    min_age_limit   NUMBER DEFAULT 0,
    is_adult_only   NUMBER(1,0) DEFAULT 0,
    description     CLOB,

    CONSTRAINT CHK_outlet_type CHECK (outlet_type IN ('RESTAURANT', 'BAR', 'LOUNGE')),
    CONSTRAINT CHK_adult_only CHECK (is_adult_only IN (0, 1)),
    CONSTRAINT FK_outlet_hotel FOREIGN KEY (hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE TABLE TBL_DINING (
    dining_id        NUMBER PRIMARY KEY,
    fk_hotel_id      NUMBER NOT NULL,
    name             VARCHAR2(100) NOT NULL,
    d_type           VARCHAR2(20),
    tel              VARCHAR2(20),
    floor            VARCHAR2(10),
    main_img         VARCHAR2(200) DEFAULT 'default_dining.jpg',
    description      VARCHAR2(1000),
    business_hours   VARCHAR2(100),
    introduction     CLOB,
    store_imgs       VARCHAR2(1000),
    food_imgs        VARCHAR2(1000),
    menu_pdf         VARCHAR2(200),
    extra_info       VARCHAR2(2000),
    open_time        VARCHAR2(5),
    close_time       VARCHAR2(5),
    available_times  VARCHAR2(1000)
);

CREATE TABLE Dining_Tables (
    table_id        NUMBER PRIMARY KEY,
    fk_hotel_id     NUMBER NOT NULL,
    table_number    VARCHAR2(20) NOT NULL,
    max_capacity    NUMBER NOT NULL,
    min_capacity    NUMBER DEFAULT 1,
    zone_name       VARCHAR2(50),
    is_specifiable  CHAR(1) DEFAULT 'Y',
    active_yn       CHAR(1) DEFAULT 'Y' NOT NULL,
    fk_dining_id    NUMBER,

    CONSTRAINT ck_dining_table_spec CHECK (is_specifiable IN ('Y','N')),
    CONSTRAINT ck_dining_table_active CHECK (active_yn IN ('Y','N')),
    CONSTRAINT ck_dining_table_capacity CHECK (max_capacity >= 1 AND min_capacity >= 1 AND max_capacity >= min_capacity),
    CONSTRAINT fk_dining_table_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),
    CONSTRAINT fk_dining_connection FOREIGN KEY (fk_dining_id) REFERENCES TBL_DINING(dining_id)
);

CREATE SEQUENCE SEQ_DINING_TABLES START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE Dining_Payments (
    payment_id         NUMBER PRIMARY KEY,
    amount             NUMBER NOT NULL,
    original_amount    NUMBER NOT NULL,
    cancellation_fee   NUMBER DEFAULT 0,
    payment_method     VARCHAR2(50),
    status             VARCHAR2(30) DEFAULT 'PAID',
    pg_tid             VARCHAR2(100),
    paid_at            TIMESTAMP DEFAULT SYSTIMESTAMP,
    refunded_at        TIMESTAMP NULL,
    dining_res_no      NUMBER,

    CONSTRAINT ck_dining_payment_status CHECK (status IN ('PAID','PARTIAL_REFUNDED','FULLY_REFUNDED','FAILED'))
);

CREATE SEQUENCE SEQ_DINING_PAYMENTS START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE Dining_Reservations (
    dining_reservation_id NUMBER PRIMARY KEY,
    fk_hotel_id           NUMBER NOT NULL,
    dining_id             NUMBER,
    table_id              NUMBER,
    fk_member_no          NUMBER,
    guest_name            VARCHAR2(50),
    guest_phone           VARCHAR2(20),
    adult_count           NUMBER DEFAULT 0,
    child_count           NUMBER DEFAULT 0,
    infant_count          NUMBER DEFAULT 0,
    res_date              DATE NOT NULL,
    res_time              VARCHAR2(5) NOT NULL,
    special_requests      CLOB,
    allergy_info          CLOB,
    status                VARCHAR2(30) DEFAULT 'WAITING_PAYMENT',
    payment_id            NUMBER,
    created_at            TIMESTAMP DEFAULT SYSTIMESTAMP,
    updated_at            TIMESTAMP DEFAULT SYSTIMESTAMP,
    res_password          VARCHAR2(100),
    guest_email           VARCHAR2(100),

    CONSTRAINT ck_dining_res_time CHECK (REGEXP_LIKE(res_time, '^[0-2][0-9]:[0-5][0-9]$')),
    CONSTRAINT ck_dining_res_status CHECK (status IN ('WAITING_PAYMENT','CONFIRMED','VISITED','CANCELLED','NOSHOW')),
    CONSTRAINT ck_dining_res_counts CHECK (adult_count >= 0 AND child_count >= 0 AND infant_count >= 0),
    CONSTRAINT fk_dining_res_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),
    CONSTRAINT fk_dining_res_table FOREIGN KEY (table_id) REFERENCES Dining_Tables(table_id),
    CONSTRAINT fk_dining_res_member FOREIGN KEY (fk_member_no) REFERENCES tbl_member_security(member_no),
    CONSTRAINT fk_dining_res_payment FOREIGN KEY (payment_id) REFERENCES Dining_Payments(payment_id)
);

CREATE SEQUENCE SEQ_DINING_RESERVATIONS START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;
CREATE INDEX idx_dining_res_date_time ON Dining_Reservations(res_date, res_time);
CREATE INDEX idx_dining_guest_phone ON Dining_Reservations(guest_phone);

CREATE TABLE Dining_Pricing_Policies (
    pricing_policy_id     NUMBER PRIMARY KEY,
    dining_reservation_id NUMBER NOT NULL,
    category              VARCHAR2(20) NOT NULL,
    price                 NUMBER DEFAULT 0,

    CONSTRAINT ck_dining_price_category CHECK (category IN ('ADULT','CHILD','INFANT')),
    CONSTRAINT ck_dining_price_nonneg CHECK (price >= 0),
    CONSTRAINT fk_dining_price_res FOREIGN KEY (dining_reservation_id) REFERENCES Dining_Reservations(dining_reservation_id)
);

CREATE SEQUENCE SEQ_DINING_PRICING_POLICIES START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE Dining_Refund_Policies (
    refund_policy_id      NUMBER PRIMARY KEY,
    dining_reservation_id NUMBER NOT NULL,
    days_before           NUMBER,
    refund_rate           NUMBER,

    CONSTRAINT ck_dining_refund_rate CHECK (refund_rate BETWEEN 0 AND 100),
    CONSTRAINT fk_dining_refund_res FOREIGN KEY (dining_reservation_id) REFERENCES Dining_Reservations(dining_reservation_id)
);

CREATE SEQUENCE SEQ_DINING_REFUND_POLICIES START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 13) 게시판/운영
-- ===================================================================
CREATE TABLE NOTICES (
    notice_id   NUMBER PRIMARY KEY,
    admin_no    NUMBER,
    fk_hotel_id NUMBER NOT NULL,
    title       VARCHAR2(200) NOT NULL,
    content     CLOB NOT NULL,
    is_top      CHAR(1) DEFAULT 'N',
    created_at  DATE DEFAULT SYSDATE,

    CONSTRAINT ck_notice_top CHECK (is_top IN ('Y', 'N')),
    CONSTRAINT fk_notice_admin FOREIGN KEY (admin_no) REFERENCES tbl_admin_security(admin_no),
    CONSTRAINT fk_notice_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE SEQUENCE SEQ_NOTICE_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE FAQS (
    faq_id      NUMBER PRIMARY KEY,
    fk_hotel_id NUMBER NOT NULL,
    category    VARCHAR2(50),
    title       VARCHAR2(200) NOT NULL,
    admin_no    NUMBER,
    view_count  NUMBER DEFAULT 0,
    created_at  DATE DEFAULT SYSDATE,
    content     VARCHAR2(4000),

    CONSTRAINT fk_faq_admin FOREIGN KEY (admin_no) REFERENCES tbl_admin_security(admin_no),
    CONSTRAINT fk_faq_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE SEQUENCE SEQ_FAQ_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE QUESTIONS (
    qna_id      NUMBER PRIMARY KEY,
    fk_hotel_id NUMBER NOT NULL,
    writer_name VARCHAR2(50) NOT NULL,
    title       VARCHAR2(200) NOT NULL,
    status      VARCHAR2(20) DEFAULT 'WAITING',
    is_secret   CHAR(1) DEFAULT 'N',
    created_at  DATE DEFAULT SYSDATE,
    content     VARCHAR2(4000),

    CONSTRAINT ck_qna_status CHECK (status IN ('WAITING', 'ANSWERED')),
    CONSTRAINT ck_qna_secret CHECK (is_secret IN ('Y', 'N')),
    CONSTRAINT fk_qna_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE SEQUENCE SEQ_QNA_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE ANSWERS (
    answer_id   NUMBER PRIMARY KEY,
    qna_id      NUMBER NOT NULL,
    admin_no    NUMBER NOT NULL,
    created_at  DATE DEFAULT SYSDATE,
    content     VARCHAR2(4000),

    CONSTRAINT fk_answer_qna FOREIGN KEY (qna_id) REFERENCES QUESTIONS(qna_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_admin FOREIGN KEY (admin_no) REFERENCES tbl_admin_security(admin_no)
);

CREATE SEQUENCE SEQ_ANSWER_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;


-- ===================================================================
-- 14) 프로모션 / 통계
-- ===================================================================
/*
   [최신 반영 사항]
   - 실제 DB 기준으로 PROMOTION_MASTER / PROMOTION_BANNER 구조 반영
   - 예전 초안의 promotion_type 중심 구조가 아니라
     실제 운영 중인 banner_type / subtitle / sort_order 구조를 사용한다.
*/
CREATE TABLE PROMOTION_MASTER (
    promotion_id    NUMBER PRIMARY KEY,
    fk_hotel_id     NUMBER NOT NULL,
    title           VARCHAR2(200) NOT NULL,
    price           NUMBER,
    discount_rate   NUMBER(5,2) DEFAULT 0,
    discount_amount NUMBER DEFAULT 0,
    start_date      DATE,
    end_date        DATE,
    is_active       NUMBER(1,0) DEFAULT 1,
    created_at      DATE DEFAULT SYSDATE,

    CONSTRAINT fk_promotion_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE SEQUENCE SEQ_PROMOTION_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE PROMOTION_BANNER (
    banner_id        NUMBER PRIMARY KEY,
    fk_promotion_id  NUMBER,
    fk_hotel_id      NUMBER NOT NULL,
    banner_type      VARCHAR2(20) NOT NULL,
    title            VARCHAR2(200) NOT NULL,
    subtitle         VARCHAR2(500),
    benefits         VARCHAR2(1000),
    image_url        VARCHAR2(500),
    sort_order       NUMBER DEFAULT 1,
    active_yn        CHAR(1) DEFAULT 'Y',

    CONSTRAINT ck_banner_active CHECK (active_yn IN ('Y','N')),
    CONSTRAINT fk_banner_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),
    CONSTRAINT fk_banner_master FOREIGN KEY (fk_promotion_id) REFERENCES PROMOTION_MASTER(promotion_id) ON DELETE CASCADE
);

CREATE SEQUENCE SEQ_PROMO_BANNER_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

CREATE TABLE DAILY_REVENUE_STATS (
    stat_date       DATE,
    fk_hotel_id     NUMBER,
    total_revenue   NUMBER DEFAULT 0,
    sold_rooms      NUMBER DEFAULT 0,
    total_rooms     NUMBER DEFAULT 100,
    adr             NUMBER DEFAULT 0,
    revpar          NUMBER DEFAULT 0,
    occupancy_rate  NUMBER(5,2) DEFAULT 0,
    PRIMARY KEY (stat_date, fk_hotel_id),

    CONSTRAINT fk_daily_rev_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id)
);

CREATE TABLE DAILY_PROMOTION_STATS (
    stat_date       DATE,
    fk_hotel_id     NUMBER,
    promotion_id    NUMBER,
    promo_revenue   NUMBER DEFAULT 0,
    promo_count     NUMBER DEFAULT 0,
    PRIMARY KEY (stat_date, fk_hotel_id, promotion_id),

    CONSTRAINT fk_daily_promo_hotel FOREIGN KEY (fk_hotel_id) REFERENCES tbl_hotel(hotel_id),
    CONSTRAINT fk_daily_promo_promotion FOREIGN KEY (promotion_id) REFERENCES PROMOTION_MASTER(promotion_id) ON DELETE CASCADE
);

CREATE TABLE RESERVATION_PROMOTION_MAPPING (
    mapping_id            NUMBER PRIMARY KEY,
    reservation_id        NUMBER NOT NULL,
    promotion_id          NUMBER NOT NULL,
    applied_price_at_time NUMBER,
    benefit_delivered     VARCHAR2(500),
    applied_at            DATE DEFAULT SYSDATE,

    CONSTRAINT fk_map_reservation FOREIGN KEY (reservation_id) REFERENCES RESERVATION(reservation_id),
    CONSTRAINT fk_map_promotion FOREIGN KEY (promotion_id) REFERENCES PROMOTION_MASTER(promotion_id) ON DELETE CASCADE
);

CREATE SEQUENCE SEQ_MAPPING_ID START WITH 1 INCREMENT BY 1 NOMAXVALUE NOMINVALUE NOCYCLE NOCACHE;

COMMIT;
