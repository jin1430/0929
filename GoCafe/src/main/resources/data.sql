-- FK 잠시 비활성화
SET REFERENTIAL_INTEGRITY FALSE;

-- 깔끔 초기화
DELETE FROM cafe_tag;
DELETE FROM cafe;
DELETE FROM member;

-- 아이디 시퀀스 리셋 (없으면 무시됨)
ALTER TABLE cafe      ALTER COLUMN cafe_id RESTART WITH 1;
ALTER TABLE cafe_tag  ALTER COLUMN cafe_tag_id RESTART WITH 1;
ALTER TABLE member    ALTER COLUMN member_id RESTART WITH 1;

-- ===== Cafe 더미 =====
INSERT INTO cafe
  (cafe_id, cafe_owner_id, cafe_name, cafe_address, cafe_lat, cafe_lon, cafe_number, cafe_date, cafe_views, cafe_photo, cafe_code, status)
VALUES
  (1, 1, '카페무드 홍대점','서울 마포구 양화로 45', 37.5569,126.9238,'000000000000', CURRENT_TIMESTAMP, 432, 'img/c1.jpg','CAFE000001','APPROVED'),
  (2, 2, '브런치랩 합정', '서울 마포구 독막로 15', 37.5498,126.9136,'000000000001', CURRENT_TIMESTAMP, 210, 'img/c2.jpg','CAFE000002','APPROVED'),
  (3, 3, '라떼가좋아', '서울 마포구 잔다리로 20', 37.5531,126.9204,'000000000002', CURRENT_TIMESTAMP, 987, 'img/c3.jpg','CAFE000003','APPROVED'),
  (4, 4, '루프탑뷰', '서울 마포구 와우산로 80', 37.5539,126.9352,'000000000003', CURRENT_TIMESTAMP, 120, 'img/c4.jpg','CAFE000004','APPROVED'),
  (5, 5, '스페셜티랩', '서울 마포구 잔다리로 5', 37.5522,126.9190,'000000000004', CURRENT_TIMESTAMP, 75, 'img/c5.jpg','CAFE000005','APPROVED');


-- ===== CafeTag 더미 =====
-- tag_category_code 예: MOOD / MENU / FEATURE ...
INSERT INTO cafe_tag (cafe_id, tag_category_code, tag_code) VALUES
(1,'MOOD','감성'), (1,'MENU','브런치'), (1,'FEATURE','콘센트많음'),
(2,'MENU','브런치'), (2,'MOOD','데이트'), (2,'FEATURE','애견동반'),
(3,'MENU','라떼'), (3,'MOOD','조용한'), (3,'FEATURE','와이파이빠름'),
(4,'MOOD','루프탑'), (4,'VIEW','야경좋음'),
(5,'BEAN','스페셜티'), (5,'BREW','핸드드립');

-- ===== Member 더미(로그인 테스트) =====
-- 비밀번호는 1234 (AuthController가 dev 편의상 평문 비교 1회 허용)
INSERT INTO member
(member_id, member_email, member_password, member_nickname, member_age, member_gender, member_role, member_date, member_photo, token_version)
VALUES
(1,'test@cafego.dev','11111111','테스터',25,'M','ROLE_USER',CURRENT_TIMESTAMP,NULL,0);

-- FK 재활성화
SET REFERENTIAL_INTEGRITY TRUE;
