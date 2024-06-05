SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS `photosdbmanager` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `photosdbmanager`;

DROP TABLE IF EXISTS `tagged`;
DROP TABLE IF EXISTS `images`;
DROP TABLE IF EXISTS `cameras`;
DROP TABLE IF EXISTS `tags`;

CREATE TABLE `cameras` (
  `cameraname` varchar(255) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `cameras` (`cameraname`) VALUES
('IPhone 8'),
('IPhone X'),
('Nikon COOLPIX L120');

CREATE TABLE `tags` (
  `tagname` varchar(255) NOT NULL,
  PRIMARY KEY (`tagname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tags` (`tagname`) VALUES ('Animal'),('Artwork'),('B & W'),('Buildings'),('Cloudy'),('Color'),('Daytime'),('Evening'),
('Landscape '),('Mural'),('Nature'),('Night'),('Object(s)'),('People'),('Portrait'),('Rain'),('Rural'),('Sculpture'),('Snow'),('Stars'),
('Suburban'),('Sunny'),('Sunset'),('Urban'),('Water');

CREATE TABLE `images` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `abspath` varchar(255) DEFAULT NULL,
  `relpath` varchar(255) DEFAULT NULL,
  `dateandtime` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'YYYY-MM-DD HH:MM:SS',
  `cameraname` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL COMMENT 'City, State, (Country)',
  `caption` varchar(255) DEFAULT NULL,
  PRIMARY KEY(`id`),
  FOREIGN KEY (`cameraname`) REFERENCES `cameras` (`cameraname`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `tagged` (
  `id` int(11) NOT NULL,
  `tagname` varchar(255) NOT NULL,
  PRIMARY KEY(`id`,`tagname`),
  FOREIGN KEY (`id`) REFERENCES `images` (`id`),
  FOREIGN KEY (`tagname`) REFERENCES `tags` (`tagname`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
