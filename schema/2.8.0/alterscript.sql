CREATE TABLE `logistimo`.`USER_DEVICES` (
  `id` VARCHAR(255) NOT NULL,
  `user_id` VARCHAR(255) NOT NULL,
  `application_name` INT NOT NULL,
  `created_on` DATETIME NOT NULL,
  `expires_on` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=INNODB DEFAULT CHARSET=utf8;

ALTER TABLE DEMANDITEM ADD CREATEDON DATETIME DEFAULT NULL AFTER `ST`, ADD CREATEDBY varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL AFTER `TX`;

CREATE TABLE `FEEDBACK` (
  `ID` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `USERID` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `DOMAINID` bigint(20) DEFAULT NULL,
  `APP` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `APPVERSION` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `CREATEDATE` datetime DEFAULT NULL,
  `TITLE` varchar(255) DEFAULT NULL,
  `TEXT` varchar(2048) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE USERACCOUNT MODIFY COLUMN `BIRTHDATE` DATE, MODIFY COLUMN AGE int(11) DEFAULT 0;

CREATE TABLE `RETURNS_TRACKING_DETAILS` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `returns_id` BIGINT(20) NOT NULL,
  `tracking_id` varchar(255) DEFAULT NULL,
  `transporter` varchar(255) DEFAULT NULL,
  `expected_on` DATE DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_by` varchar(255) DEFAULT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

ALTER TABLE `logistimo`.`DEMANDITEM` DROP RQ;

ALTER TABLE `USERACCOUNT` ADD COLUMN `SALT` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL, ADD COLUMN `PASSWORD` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

ALTER TABLE `USERLOGINHISTORY` ADD COLUMN `REFERER` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL, ADD COLUMN `STATUS` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

UPDATE USERLOGINHISTORY SET STATUS = 'SUCCESS';

ALTER TABLE DASHBOARD MODIFY `CONF` VARCHAR(24576);