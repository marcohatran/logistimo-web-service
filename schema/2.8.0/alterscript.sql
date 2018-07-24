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
