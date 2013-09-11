/*!40101 SET NAMES binary*/;
/*!40014 SET FOREIGN_KEY_CHECKS=0*/;

CREATE TABLE `sketchy_items` (
  `id` int(11) unsigned NOT NULL,
  `kind` char(16) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`,`kind`)
) DEFAULT CHARSET=utf8;
