USE `pattern_eval_b`;

DROP TABLE IF EXISTS `ResponseB`;
DROP TABLE IF EXISTS `RequestB`;

CREATE TABLE IF NOT EXISTS `RequestB` (
  `requestID` varchar(60) NOT NULL,
  `requestID_fromA` varchar(60) NOT NULL,
  `requestText` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`requestID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `ResponseB` (
  `responseID` varchar(60) NOT NULL,
  `requestID` varchar(60),
  `responseText` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`responseID`),
  FOREIGN KEY (`requestID`) REFERENCES RequestB(`requestID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
