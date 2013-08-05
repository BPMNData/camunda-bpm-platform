DROP TABLE IF EXISTS `Offer`;
DROP TABLE IF EXISTS `Request`;

CREATE TABLE IF NOT EXISTS `Request` (
  `requestID` varchar(60) NOT NULL,
  `messageRequestID` varchar(60),
  `departure` varchar(255),
  `destination` varchar(255),
  `start_date` varchar(255),
  `return_date` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`requestID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `Offer` (
  `offerID` varchar(60) NOT NULL,
  `requestID` varchar(60),
  `inboundFlightNumber` varchar(255),
  `outboundFlightNumber` varchar(255),
  `price` DOUBLE,
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`offerID`),
  FOREIGN KEY (`requestID`) REFERENCES Request(`requestID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;