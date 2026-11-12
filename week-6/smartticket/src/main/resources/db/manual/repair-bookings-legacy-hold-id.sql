SET @schema_name = DATABASE();

SET @legacy_fk = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'hold_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);
SET @ddl = IF(@legacy_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `bookings` DROP FOREIGN KEY `', @legacy_fk, '`'));
PREPARE repair_stmt FROM @ddl;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @legacy_index = (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'hold_id'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);
SET @ddl = IF(@legacy_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `bookings` DROP INDEX `', @legacy_index, '`'));
PREPARE repair_stmt FROM @ddl;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;

SET @legacy_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'bookings'
      AND COLUMN_NAME = 'hold_id'
);
SET @ddl = IF(@legacy_column_exists = 0,
    'SELECT 1',
    'ALTER TABLE `bookings` DROP COLUMN `hold_id`');
PREPARE repair_stmt FROM @ddl;
EXECUTE repair_stmt;
DEALLOCATE PREPARE repair_stmt;
