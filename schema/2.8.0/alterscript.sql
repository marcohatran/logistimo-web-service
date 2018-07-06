CREATE TABLE `logistimo`.`USER_DEVICES` (
  `id` VARCHAR(255) NOT NULL,
  `user_id` VARCHAR(255) NOT NULL,
  `application_name` INT NOT NULL,
  `created_on` DATETIME NOT NULL,
  `expires_on` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
  ) ENGINE=INNODB DEFAULT CHARSET=utf8;
