-- phpMyAdmin SQL Dump
-- version 4.0.10deb1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Sep 18, 2017 at 02:24 PM
-- Server version: 5.5.55-0ubuntu0.14.04.1
-- PHP Version: 5.5.9-1ubuntu4.21

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `portal`
--

-- --------------------------------------------------------

--
-- Table structure for table `Access_control`
--

CREATE TABLE IF NOT EXISTS `Access_control` (
  `ACL_ID` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `Src_attr` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Src_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Dst_IP` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `Dst_port` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Protocol` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Permission` tinyint(1) NOT NULL,
  `Priority` int(11) NOT NULL,
  PRIMARY KEY (`ACL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `Area_flow`
--

CREATE TABLE IF NOT EXISTS `Area_flow` (
  `ID` int(11) NOT NULL,
  `Week` varchar(3) COLLATE utf8_unicode_ci NOT NULL,
  `Time_period` int(11) NOT NULL,
  `Building` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Kbps` double NOT NULL,
  `Percentage` int(11) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `Area_flow`
--

INSERT INTO `Area_flow` (`ID`, `Week`, `Time_period`, `Building`, `Kbps`, `Percentage`) VALUES
(0, 'Fri', 9, 'Building2', 0.0126236979166667, 0),
(1, 'Fri', 15, 'Building1', 25.3749153645833, 6),
(2, 'Thu', 10, 'Building1', 428.477140694282, 94),
(3, 'Thu', 12, 'Building1', 0.0232345920138889, 0),
(4, 'Thu', 12, 'Building2', 0.00958116319444445, 0),
(5, 'Tue', 13, 'Building2', 0.01205078125, 0);

-- --------------------------------------------------------

--
-- Table structure for table `Flow_classification`
--

CREATE TABLE IF NOT EXISTS `Flow_classification` (
  `ID` int(11) NOT NULL,
  `User_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Week` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Time_period` int(11) NOT NULL,
  `Building` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Room` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Kbps` double NOT NULL,
  `Day_counts` int(11) NOT NULL,
  `Bwd_req` varchar(5) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `Flow_classification`
--

INSERT INTO `Flow_classification` (`ID`, `User_ID`, `Week`, `Time_period`, `Building`, `Room`, `Kbps`, `Day_counts`, `Bwd_req`) VALUES
(0, 'A', 'Fri', 15, 'Building1', 'Room1', 0.0149153645833333, 13, 'Mid'),
(1, 'A', 'Thu', 10, 'Building1', 'Room1', 428.466405017198, 213, 'High'),
(2, 'A', 'Thu', 10, 'Building1', 'Room2', 0.0107356770833333, 13, 'High'),
(3, 'A', 'Thu', 12, 'Building1', 'Room1', 0.0124989149305556, 26, 'High'),
(4, 'B', 'Fri', 15, 'Building1', 'Room1', 25.36, 400, 'High'),
(5, 'B', 'Thu', 12, 'Building1', 'Room1', 0.0107356770833333, 13, 'Low'),
(6, 'F', 'Fri', 9, 'Building2', 'Room2', 0.0126236979166667, 13, 'High'),
(7, 'F', 'Thu', 12, 'Building2', 'Room2', 0.00958116319444445, 13, 'High'),
(8, 'F', 'Tue', 13, 'Building2', 'Room2', 0.01205078125, 13, 'High');

-- --------------------------------------------------------

--
-- Table structure for table `Group`
--

CREATE TABLE IF NOT EXISTS `Group` (
  `Group_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`Group_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `Group`
--

INSERT INTO `Group` (`Group_ID`, `Name`) VALUES
('Guest', 'Guest'),
('Staff', 'Staff'),
('Studnet', 'Student'),
('Teacher', 'Teacher');

-- --------------------------------------------------------

--
-- Table structure for table `IP_MAC`
--

CREATE TABLE IF NOT EXISTS `IP_MAC` (
  `IP` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `MAC` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `Time` datetime NOT NULL,
  PRIMARY KEY (`IP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `IP_MAC`
--

INSERT INTO `IP_MAC` (`IP`, `MAC`, `Time`) VALUES
('192.168.44.101', 'EA:E9:78:FB:FD:00', '2017-09-15 09:01:54'),
('192.168.44.166', 'EA:E9:78:FB:FD:01', '2017-09-14 10:40:46'),
('192.168.44.167', 'EA:E9:78:FB:FD:02', '2017-09-14 12:26:43'),
('192.168.44.168', 'EA:E9:78:FB:FD:03', '2017-09-14 10:31:40'),
('192.168.44.169', 'EA:E9:78:FB:FD:04', '2017-09-05 14:55:21'),
('192.168.44.170', 'EA:E9:78:FB:FD:05', '2017-09-05 15:31:16'),
('192.168.44.171', 'EA:E9:78:FB:FD:06', '2017-09-05 15:21:54'),
('192.168.44.172', 'EA:E9:78:FB:FD:07', '2017-09-15 09:01:54'),
('192.168.44.173', 'EA:E9:78:FB:FD:08', '2017-09-05 15:09:01');

-- --------------------------------------------------------

--
-- Table structure for table `Registered_MAC`
--

CREATE TABLE IF NOT EXISTS `Registered_MAC` (
  `MAC` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `User_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Group_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Enable` tinyint(1) NOT NULL,
  PRIMARY KEY (`MAC`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `Registered_MAC`
--

INSERT INTO `Registered_MAC` (`MAC`, `User_ID`, `Group_ID`, `Enable`) VALUES
('EA:E9:78:FB:FD:00', '', '', 1),
('EA:E9:78:FB:FD:01', 'A', 'Teacher', 1),
('EA:E9:78:FB:FD:02', 'B', 'Teacher', 1),
('EA:E9:78:FB:FD:03', 'A', 'Teacher', 1),
('EA:E9:78:FB:FD:07', 'F', 'Guest', 1);

-- --------------------------------------------------------

--
-- Table structure for table `Switch`
--

CREATE TABLE IF NOT EXISTS `Switch` (
  `Switch_ID` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `Building` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Room` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`Switch_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `Switch`
--

INSERT INTO `Switch` (`Switch_ID`, `Building`, `Room`) VALUES
('of:0000000000000003', 'Building1', ''),
('of:0000000000000004', 'Building2', ''),
('of:0000000000000005', 'Building1', 'Room1'),
('of:0000000000000006', 'Building1', 'Room2'),
('of:0000000000000007', 'Building2', 'Room1'),
('of:0000000000000008', 'Building2', 'Room2');

-- --------------------------------------------------------

--
-- Table structure for table `User`
--

CREATE TABLE IF NOT EXISTS `User` (
  `User_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Name` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `Group_ID` varchar(10) COLLATE utf8_unicode_ci NOT NULL,
  `Account` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  `Password` varchar(20) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`User_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Dumping data for table `User`
--

INSERT INTO `User` (`User_ID`, `Name`, `Group_ID`, `Account`, `Password`) VALUES
('A', 'A', 'Teacher', '1', '1'),
('B', 'B', 'Teacher', '2', '2'),
('C', 'C', 'Staff', '3', '3'),
('D', 'D', 'Student', '4', '4'),
('E', 'E', 'Student', '5', '5'),
('F', 'F', 'Guest', '6', '6');

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
