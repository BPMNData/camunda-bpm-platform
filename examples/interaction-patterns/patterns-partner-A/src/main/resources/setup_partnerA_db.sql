USE `pattern_eval_a`;

DROP TABLE IF EXISTS `ResponseA`;
DROP TABLE IF EXISTS `RequestA`;

CREATE TABLE IF NOT EXISTS `RequestA` (
  `requestID` varchar(60) NOT NULL,
  `requestText` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`requestID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `ResponseA` (
  `responseID` varchar(60) NOT NULL,
  `requestID` varchar(60),
  `responseText` varchar(255),
  `conversationNumber` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`responseID`),
  FOREIGN KEY (`requestID`) REFERENCES RequestA(`requestID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


DROP TABLE IF EXISTS `DataObjectA`;
DROP TABLE IF EXISTS `DocumentA`;

CREATE TABLE IF NOT EXISTS `DocumentA` (
  `documentID` varchar(60) NOT NULL,
  `text` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`documentID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `DataObjectA` (
  `objectID` varchar(60) NOT NULL,
  `documentID` varchar(60),
  `text` varchar(255),
  `conversationNumber` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`objectID`),
  FOREIGN KEY (`documentID`) REFERENCES DocumentA(`documentID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
