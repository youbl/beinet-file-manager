-- EMPTY DATABASE ONLY. Then execute migration/V001, V002, V003 in order.
-- Do not run this file on an existing installation. Migrations are manual.
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    userPassword VARCHAR(255),
    userEmail VARCHAR(255),
    lastLoginDate DATETIME,
    status INT NOT NULL DEFAULT 1,
    isBoss INT NOT NULL DEFAULT 0,
    isAdmin INT NOT NULL DEFAULT 0,
    tenantId BIGINT,
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    delflag INT NOT NULL DEFAULT 0,
    inviteUserId BIGINT,
    inviteToken VARCHAR(64),
    mfaKey VARCHAR(100),
    picture VARCHAR(255),
    isTest INT DEFAULT 0,
    memo VARCHAR(10),
    userIp VARCHAR(15),
    userLoc VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
