DROP TABLE IF EXISTS `Offer`;
DROP TABLE IF EXISTS `AirlineRequest`;
DROP TABLE IF EXISTS `TravelDetails`;
DROP TABLE IF EXISTS `Airline`;

CREATE TABLE IF NOT EXISTS `TravelDetails` (
  `travelID` varchar(60) NOT NULL,
  `cu_request_id` varchar(60),
  `customer` varchar(255),
  `departure` varchar(255),
  `destination` varchar(255),
  `start_date` varchar(255),
  `return_date` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`travelID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `Airline` (
  `airlineID` varchar(60) NOT NULL,
  `name` varchar(255),
  `country` varchar(255),
  `address` varchar(255),
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`airlineID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `AirlineRequest` (
  `requestID` varchar(60) NOT NULL,
  `travelID` varchar(60),
  `airlineID` varchar(60),
  `departure` varchar(255),
  `destination` varchar(255),
  `start_date` varchar(255),
  `return_date` varchar(255),
  `inboundFlightNumber` varchar(255),
  `outboundFlightNumber` varchar(255),
  `price` DOUBLE,
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`requestID`),
  FOREIGN KEY (`travelID`) REFERENCES TravelDetails(`travelID`) ON DELETE CASCADE,
  FOREIGN KEY (`airlineID`) REFERENCES Airline(`airlineID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `Offer` (
  `offerID` varchar(60) NOT NULL,
  `travelID` varchar(60),
  `airlineID` varchar(60),
  `inboundFlightNumber` varchar(255),
  `outboundFlightNumber` varchar(255),
  `price` DOUBLE,
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`offerID`),
  FOREIGN KEY (`travelID`) REFERENCES TravelDetails(`travelID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

INSERT INTO `Airline` (`airlineID`,  `name`, `country`, `address`, `state`) VALUES ('AB', 'Air Berlin', 'Germany', 'http://localhost:8082/bpmn-data-endpoint', 'active'), ('LH', 'Lufthansa', 'Germany', 'http://localhost:8083/bpmn-data-endpoint', 'active');