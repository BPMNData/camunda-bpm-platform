SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Datenbank: `testdb`
--

-- --------------------------------------------------------

DROP TABLE IF EXISTS `order`;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `order`
--

CREATE TABLE IF NOT EXISTS `order` (
  `oid` varchar(60) NOT NULL,
  `btoid` varchar(60) DEFAULT NULL,
  `state` text NOT NULL,
  PRIMARY KEY (`oid`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
