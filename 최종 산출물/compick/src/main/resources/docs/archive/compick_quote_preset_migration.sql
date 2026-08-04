-- ============================================================
-- COMPICK 견적구매 / 견적추천(PRESET) 기능 추가 마이그레이션
--
-- 이 스크립트는 이미 compick_schema_oracle.sql로 스키마가 만들어진
-- 기존 Oracle DB(예: 로컬 compick-oracle Docker 컨테이너)에
-- QUOTE.purpose_tag / QUOTE.summary_description 컬럼과
-- 프리셋 견적 시드 데이터를 추가하기 위한 "1회성 증분 마이그레이션"이다.
--
-- 신규로 DB를 만드는 경우에는 이 스크립트가 필요 없다 - 이미
-- compick_schema_oracle.sql 자체에 purpose_tag/summary_description
-- 컬럼이 반영돼 있다. 이 파일은 "기존에 이미 스키마를 적용한 DB"를
-- 최신 상태로 맞추기 위한 용도이며, 실행 후에는 docs/archive/로
-- 옮겨서 보관하는 것을 권장한다 (프로젝트 컨벤션).
--
-- 실행 예:
--   sqlplus COMPICK/<password>@//localhost:1521/XEPDB1 @compick_quote_preset_migration.sql
-- 또는 Docker 컨테이너 안에서:
--   docker exec -i compick-oracle sqlplus COMPICK/<password>@//localhost:1521/XEPDB1 < compick_quote_preset_migration.sql
-- ============================================================

SET SERVEROUTPUT ON

-- ============================================================
-- 1. QUOTE 테이블 컬럼 추가 (이미 있으면 건너뛰어야 하므로,
--    재실행 시 ORA-01430 이 나면 이미 적용된 것이므로 무시해도 된다)
-- ============================================================

ALTER TABLE QUOTE ADD (
    purpose_tag          VARCHAR2(20),
    summary_description  VARCHAR2(200)
);

ALTER TABLE QUOTE ADD CONSTRAINT ck_quote_purpose_tag
    CHECK (purpose_tag IS NULL OR purpose_tag IN ('OFFICE', 'GAMING', 'VIDEO_EDIT', 'BEGINNER'));


-- ============================================================
-- 2. 프리셋 전용 시스템 계정
-- 이 계정은 실제 로그인 용도가 아니라 PRESET 타입 QUOTE의
-- member_id NOT NULL 제약을 만족시키기 위한 소유자 역할만 한다.
-- password_hash는 아무도 모르는 무작위 값을 BCrypt로 해싱한
-- 값이라 실제로 로그인할 수 없다 (평문 비밀번호는 어디에도 남기지 않음).
-- ============================================================

MERGE INTO MEMBER target
USING (SELECT 'system_preset' AS login_id FROM DUAL) source
ON (target.login_id = source.login_id)
WHEN NOT MATCHED THEN
    INSERT (
        login_id, password_hash, member_name, email,
        nickname, phone, member_role, member_status
    ) VALUES (
        'system_preset',
        '$2a$10$5g3XNImbFFhEXfLrTctgcOplVDd80g5Q3e0CtxqmeUhuBW.os/F/m',
        'COMPICK 시스템',
        'system-preset@compick.internal',
        'COMPICK',
        '000-0000-0000',
        'ADMIN',
        'ACTIVE'
    );


-- ============================================================
-- 3. 프리셋 견적 4종 시드
-- CPU 소켓 / 메인보드 소켓·메모리 규격이 서로 맞물리도록
-- 실제 카탈로그(543개 상품)에서 조건에 맞는 상품을 골라 연결한다.
-- 이미 동일한 quote_name의 PRESET 견적이 있으면 건너뛴다(재실행 안전).
-- ============================================================

DECLARE
    v_system_member_id  MEMBER.member_id%TYPE;

    PROCEDURE create_preset(
        p_quote_name    IN VARCHAR2,
        p_purpose_tag   IN VARCHAR2,
        p_summary       IN VARCHAR2,
        p_gpu_chipset_like IN VARCHAR2  -- NULL이면 GPU 없이 구성(내장 그래픽 사무용)
    ) IS
        v_quote_id        QUOTE.quote_id%TYPE;
        v_exists          NUMBER;

        v_cpu_id          PRODUCT.product_id%TYPE;
        v_socket          PRODUCT.socket_type%TYPE;
        v_cpu_power       PRODUCT.power_consumption%TYPE;

        v_board_id        PRODUCT.product_id%TYPE;
        v_memory_type     PRODUCT.memory_type%TYPE;
        v_board_form      PRODUCT.form_factor%TYPE;

        v_ram_id          PRODUCT.product_id%TYPE;
        v_cooler_id       PRODUCT.product_id%TYPE;
        v_storage_id      PRODUCT.product_id%TYPE;

        v_gpu_id          PRODUCT.product_id%TYPE;
        v_gpu_length      PRODUCT.gpu_length_mm%TYPE := 0;
        v_gpu_power       PRODUCT.recommended_power%TYPE := 0;

        v_case_id         PRODUCT.product_id%TYPE;
        v_psu_id          PRODUCT.product_id%TYPE;
    BEGIN
        SELECT COUNT(*) INTO v_exists
          FROM QUOTE
         WHERE quote_type = 'PRESET' AND quote_name = p_quote_name;

        IF v_exists > 0 THEN
            RETURN; -- 이미 시드됨
        END IF;

        -- CPU: 예산대에 맞춰 GPU 유무로 저가/고가 라인을 나눈다.
        -- 소켓이 일치하는 메인보드가 실제로 있는 CPU만 후보로 삼는다.
        SELECT product_id, socket_type, power_consumption
          INTO v_cpu_id, v_socket, v_cpu_power
          FROM (
                SELECT p.product_id, p.socket_type, p.power_consumption
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'CPU'
                   AND p.sales_status = 'ON_SALE'
                   AND p.socket_type IS NOT NULL
                   AND EXISTS (
                       SELECT 1
                         FROM PRODUCT bp JOIN CATEGORY bc ON bp.category_id = bc.category_id
                        WHERE bc.category_name = 'MAINBOARD'
                          AND bp.sales_status = 'ON_SALE'
                          AND bp.socket_type = p.socket_type
                   )
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        IF p_gpu_chipset_like IS NOT NULL THEN
            -- 게이밍/크리에이터 라인은 예산에 맞는 CPU를 다시 고른다
            IF p_purpose_tag = 'GAMING' AND p_quote_name LIKE '%가성비%' THEN
                SELECT product_id, socket_type, power_consumption
                  INTO v_cpu_id, v_socket, v_cpu_power
                  FROM (
                        SELECT p.product_id, p.socket_type, p.power_consumption
                          FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                         WHERE c.category_name = 'CPU'
                           AND p.sales_status = 'ON_SALE'
                           AND p.brand = 'AMD'
                           AND p.socket_type = 'AM5'
                         ORDER BY p.price ASC
                       )
                 WHERE ROWNUM = 1;
            ELSIF p_quote_name LIKE '%고성능%' THEN
                SELECT product_id, socket_type, power_consumption
                  INTO v_cpu_id, v_socket, v_cpu_power
                  FROM (
                        SELECT p.product_id, p.socket_type, p.power_consumption
                          FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                         WHERE c.category_name = 'CPU'
                           AND p.sales_status = 'ON_SALE'
                           AND p.product_name LIKE '%7800X3D%'
                       )
                 WHERE ROWNUM = 1;
            ELSIF p_purpose_tag = 'VIDEO_EDIT' THEN
                SELECT product_id, socket_type, power_consumption
                  INTO v_cpu_id, v_socket, v_cpu_power
                  FROM (
                        SELECT p.product_id, p.socket_type, p.power_consumption
                          FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                         WHERE c.category_name = 'CPU'
                           AND p.sales_status = 'ON_SALE'
                           AND p.socket_type IS NOT NULL
                         ORDER BY p.price DESC
                       )
                 WHERE ROWNUM = 1;
            END IF;
        END IF;

        -- 메인보드: CPU 소켓과 일치하는 것 중 예산대에 맞춰 선택
        SELECT product_id, memory_type, form_factor
          INTO v_board_id, v_memory_type, v_board_form
          FROM (
                SELECT p.product_id, p.memory_type, p.form_factor
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'MAINBOARD'
                   AND p.sales_status = 'ON_SALE'
                   AND p.socket_type = v_socket
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        -- RAM: 메인보드 메모리 규격과 일치
        SELECT product_id INTO v_ram_id
          FROM (
                SELECT p.product_id
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'RAM'
                   AND p.sales_status = 'ON_SALE'
                   AND p.memory_type = v_memory_type
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        -- CPU 쿨러: 저렴한 것
        SELECT product_id INTO v_cooler_id
          FROM (
                SELECT p.product_id
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'CPU_COOLER'
                   AND p.sales_status = 'ON_SALE'
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        -- 저장장치: 중간 가격대 NVMe/SSD 우선
        SELECT product_id INTO v_storage_id
          FROM (
                SELECT p.product_id
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'STORAGE'
                   AND p.sales_status = 'ON_SALE'
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        -- GPU (선택): chipset 키워드로 매칭
        IF p_gpu_chipset_like IS NOT NULL THEN
            SELECT product_id, gpu_length_mm, recommended_power
              INTO v_gpu_id, v_gpu_length, v_gpu_power
              FROM (
                    SELECT p.product_id, p.gpu_length_mm, p.recommended_power
                      FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                     WHERE c.category_name = 'GPU'
                       AND p.sales_status = 'ON_SALE'
                       AND JSON_VALUE(p.spec_json, '$.Chipset') LIKE p_gpu_chipset_like
                     ORDER BY p.price ASC
                   )
             WHERE ROWNUM = 1;
        END IF;

        -- 케이스: GPU 장착 길이를 수용 가능한 것 중 저렴한 것
        SELECT product_id INTO v_case_id
          FROM (
                SELECT p.product_id
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'CASE'
                   AND p.sales_status = 'ON_SALE'
                   AND (p.max_gpu_length_mm IS NULL OR p.max_gpu_length_mm >= v_gpu_length)
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        -- 파워: CPU+GPU 소비전력 합의 1.2배 이상 용량 중 저렴한 것
        SELECT product_id INTO v_psu_id
          FROM (
                SELECT p.product_id
                  FROM PRODUCT p JOIN CATEGORY c ON p.category_id = c.category_id
                 WHERE c.category_name = 'POWER_SUPPLY'
                   AND p.sales_status = 'ON_SALE'
                   AND p.power_capacity_watt >= (NVL(v_cpu_power, 65) + NVL(v_gpu_power, 0)) * 1.2
                 ORDER BY p.price ASC
               )
         WHERE ROWNUM = 1;

        INSERT INTO QUOTE (
            member_id, quote_name, quote_type, assembly_type,
            purpose_tag, summary_description
        ) VALUES (
            v_system_member_id, p_quote_name, 'PRESET', 'SELF',
            p_purpose_tag, p_summary
        ) RETURNING quote_id INTO v_quote_id;

        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_cpu_id);
        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_cooler_id);
        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_board_id);
        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_ram_id);
        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_storage_id);
        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_case_id);
        INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_psu_id);
        IF v_gpu_id IS NOT NULL THEN
            INSERT INTO QUOTE_ITEM (quote_id, product_id) VALUES (v_quote_id, v_gpu_id);
        END IF;

        DBMS_OUTPUT.PUT_LINE('시드 완료: ' || p_quote_name);
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- 카탈로그에 조건에 맞는 부품이 없으면 이 프리셋만 건너뛰고
            -- 나머지 프리셋 시드는 계속 진행한다.
            ROLLBACK TO SAVEPOINT before_preset;
            DBMS_OUTPUT.PUT_LINE('시드 건너뜀(조건에 맞는 부품 없음): ' || p_quote_name);
    END create_preset;

BEGIN
    SELECT member_id INTO v_system_member_id FROM MEMBER WHERE login_id = 'system_preset';

    SAVEPOINT before_preset;
    create_preset(
        '사무용 기본 PC', 'OFFICE',
        '문서·인터넷·인강용, 내장 그래픽으로 구성한 합리적인 사무용 PC입니다.',
        NULL
    );

    SAVEPOINT before_preset;
    create_preset(
        '가성비 게이밍 PC', 'GAMING',
        'FHD 게임 입문용으로 가격 대비 성능에 집중한 구성입니다.',
        'GeForce RTX 40%'
    );

    SAVEPOINT before_preset;
    create_preset(
        '고성능 게이밍 PC', 'GAMING',
        'QHD 고주사율 게임을 위한 성능과 가격 균형을 고려한 구성입니다.',
        'GeForce RTX 4070 SUPER'
    );

    SAVEPOINT before_preset;
    create_preset(
        '크리에이터 PC', 'VIDEO_EDIT',
        '영상 편집·그래픽 작업을 위한 고성능 구성입니다.',
        'GeForce RTX 40%'
    );

    COMMIT;
END;
/

EXIT;
