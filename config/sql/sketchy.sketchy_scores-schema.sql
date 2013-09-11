/*!40101 SET NAMES binary*/;
/*!40014 SET FOREIGN_KEY_CHECKS=0*/;

CREATE TABLE `sketchy_scores` (
  `user_id` int(11) unsigned NOT NULL,
  `kind` char(16) NOT NULL,
  `signals` int(11) unsigned NOT NULL DEFAULT '0',
  `state` int(11) unsigned NOT NULL DEFAULT '0',
  `score` float NOT NULL DEFAULT '0.0',
  `probability` float NOT NULL DEFAULT '0.0',
  `last_signaled_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (user_id, kind)
) DEFAULT CHARSET=utf8;
