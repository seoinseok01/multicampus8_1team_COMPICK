-- ============================================================
-- COMPICK existing database migration (Oracle)
-- Updated: 2026-08-11
-- Existing rows are preserved. Run once with the COMPICK schema user.
-- ============================================================

DECLARE
    column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO column_count
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'QUOTE' AND COLUMN_NAME = 'IMAGE_URL';
    IF column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE QUOTE ADD (image_url VARCHAR2(1000))';
    END IF;
END;
/

DECLARE
    column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO column_count
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'ORDERS' AND COLUMN_NAME = 'RETURN_REQUESTED_AT';
    IF column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD (return_requested_at TIMESTAMP)';
    END IF;
END;
/

DECLARE
    column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO column_count
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'ORDERS' AND COLUMN_NAME = 'STOCK_DEDUCTED_AT';
    IF column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD (stock_deducted_at TIMESTAMP)';
    END IF;
END;
/

DECLARE
    column_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO column_count
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'ORDERS' AND COLUMN_NAME = 'STOCK_RESTORED_AT';
    IF column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD (stock_restored_at TIMESTAMP)';
    END IF;
END;
/

DECLARE
    constraint_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO constraint_count
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PRODUCT' AND CONSTRAINT_NAME = 'CK_PRODUCT_SALES_STATUS';
    IF constraint_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PRODUCT DROP CONSTRAINT CK_PRODUCT_SALES_STATUS';
    END IF;
    EXECUTE IMMEDIATE q'[
        ALTER TABLE PRODUCT ADD CONSTRAINT ck_product_sales_status
        CHECK (sales_status IN ('ON_SALE', 'OFF_SALE', 'SOLD_OUT', 'HIDDEN', 'DISCONTINUED'))
    ]';
END;
/

DECLARE
    PROCEDURE create_index_if_missing(index_name VARCHAR2, create_sql VARCHAR2) IS
        index_count NUMBER;
    BEGIN
        SELECT COUNT(*) INTO index_count
          FROM USER_INDEXES
         WHERE INDEX_NAME = UPPER(index_name);
        IF index_count = 0 THEN
            EXECUTE IMMEDIATE create_sql;
        END IF;
    END;
BEGIN
    create_index_if_missing(
        'IDX_ADDRESS_MEMBER_DEFAULT',
        'CREATE INDEX idx_address_member_default ON ADDRESS (member_id, is_default DESC, address_id)'
    );
    create_index_if_missing(
        'IDX_PRODUCT_CATALOG',
        'CREATE INDEX idx_product_catalog ON PRODUCT (category_id, sales_status, rating_count DESC, created_at DESC)'
    );
    create_index_if_missing(
        'IDX_QUOTE_MEMBER_TYPE_CREATED',
        'CREATE INDEX idx_quote_member_type_created ON QUOTE (member_id, quote_type, created_at DESC)'
    );
    create_index_if_missing(
        'IDX_QUOTE_TYPE_CREATED',
        'CREATE INDEX idx_quote_type_created ON QUOTE (quote_type, created_at DESC)'
    );
    create_index_if_missing(
        'IDX_ORDERS_MEMBER_STATUS_ORDERED',
        'CREATE INDEX idx_orders_member_status_ordered ON ORDERS (member_id, order_status, ordered_at DESC)'
    );
    create_index_if_missing(
        'IDX_ORDERS_ORDERED',
        'CREATE INDEX idx_orders_ordered ON ORDERS (ordered_at DESC)'
    );
    create_index_if_missing(
        'IDX_ORDERS_RETURN_REQUESTED',
        'CREATE INDEX idx_orders_return_requested ON ORDERS (return_requested_at)'
    );
END;
/

COMMIT;

-- Verification
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
  FROM USER_TAB_COLUMNS
 WHERE (TABLE_NAME = 'QUOTE' AND COLUMN_NAME = 'IMAGE_URL')
    OR (TABLE_NAME = 'ORDERS' AND COLUMN_NAME IN (
        'RETURN_REQUESTED_AT', 'STOCK_DEDUCTED_AT', 'STOCK_RESTORED_AT'
    ))
 ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT INDEX_NAME, TABLE_NAME
  FROM USER_INDEXES
 WHERE INDEX_NAME IN (
    'IDX_ADDRESS_MEMBER_DEFAULT',
    'IDX_PRODUCT_CATALOG',
    'IDX_QUOTE_MEMBER_TYPE_CREATED',
    'IDX_QUOTE_TYPE_CREATED',
    'IDX_ORDERS_MEMBER_STATUS_ORDERED',
    'IDX_ORDERS_ORDERED',
    'IDX_ORDERS_RETURN_REQUESTED'
 )
 ORDER BY TABLE_NAME, INDEX_NAME;
