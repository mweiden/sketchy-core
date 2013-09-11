CREATE TABLE sketchy_scores (
  user_id int(11) unsigned NOT NULL,
  kind char(16) NOT NULL,
  signals int(11) unsigned NOT NULL DEFAULT '0',
  state int(11) unsigned NOT NULL DEFAULT '0',
  score float NOT NULL DEFAULT '0.0',
  probability float NOT NULL DEFAULT '0.0',
  last_signaled_at datetime NOT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY (user_id, kind)
);

CREATE TABLE `sketchy_items` (
  `id` int(11) unsigned NOT NULL,
  `kind` char(16) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`,`kind`)
);

CREATE TABLE `trusted_users` (
  `user_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `reason` varchar(255),
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`)
);

INSERT INTO `sketchy_scores` (user_id, kind, signals, state, score, probability, last_signaled_at, created_at) VALUES
  (1, 'Test', 2, 3, 4.0, 1.0, '2013-07-21 08:00:00', '2013-07-21 07:00:00');

INSERT INTO `trusted_users` (user_id, reason, created_at) VALUES
  (1, 'Test', '2013-07-21 08:00:00');

INSERT INTO `sketchy_items` (id, kind, created_at) VALUES
  (1, 'Test', '2013-07-21 08:00:00');
