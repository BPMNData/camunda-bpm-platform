DROP TABLE IF EXISTS `Flights`;
DROP TABLE IF EXISTS `TravelDetails`;

CREATE TABLE IF NOT EXISTS `TravelDetails` (
  `travelID` varchar(60) NOT NULL,
  `departure` varchar(255),
  `destination` varchar(255),
  `start_date` varchar(255),
  `return_date` varchar(255),
  `hotel` varchar(255),
  `price` DOUBLE,
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`travelID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `Flights` (
  `flightID` varchar(60) NOT NULL,
  `travelID` varchar(60),
  `airline` varchar(255),
  `inboundFlightNumber` varchar(255),
  `outboundFlightNumber` varchar(255),
  `price` DOUBLE,
  `state` varchar(255) NOT NULL,
  PRIMARY KEY (`flightID`),
  FOREIGN KEY (`travelID`) REFERENCES TravelDetails(`travelID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;