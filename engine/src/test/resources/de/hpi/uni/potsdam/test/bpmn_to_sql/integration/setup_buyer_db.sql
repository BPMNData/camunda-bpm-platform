DROP TABLE IF EXISTS `request`;
DROP TABLE IF EXISTS `response`;

CREATE TABLE IF NOT EXISTS `request` (
  `request_id` varchar(60) NOT NULL,
  `item` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `response` (
  `response_id` varchar(60) NOT NULL,
  `request_id` varchar(60) NOT NULL,
  `item` varchar(255),
  `price` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`response_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


-- INSERT INTO `request` (`request_id`,  `item`, `state`) VALUES
-- (42, 'soccer ball', 'created'), (23, 'spaghetti', 'created');