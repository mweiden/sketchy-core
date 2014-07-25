/*!40101 SET NAMES binary*/;
/*!40014 SET FOREIGN_KEY_CHECKS=0*/;

CREATE TABLE `trusted_users` (
  `user_id` bigint unsigned NOT NULL,
  `reason` varchar(255),
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) DEFAULT CHARSET=utf8;
