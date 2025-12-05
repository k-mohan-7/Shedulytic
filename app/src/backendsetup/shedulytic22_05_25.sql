-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 22, 2025 at 06:49 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `shedulytic`
--

-- --------------------------------------------------------

--
-- Table structure for table `completions`
--

CREATE TABLE `completions` (
  `completion_id` int(11) NOT NULL,
  `habit_id` int(11) NOT NULL,
  `date` varchar(10) NOT NULL,
  `progress` enum('not_started','in_progress','completed','not_completed') NOT NULL DEFAULT 'not_started',
  `is_trusted` tinyint(4) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `completions`
--

INSERT INTO `completions` (`completion_id`, `habit_id`, `date`, `progress`, `is_trusted`) VALUES
(1, 1, '2025-02-22', 'completed', 1);

-- --------------------------------------------------------

--
-- Table structure for table `habits`
--

CREATE TABLE `habits` (
  `habit_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `reminder_date` varchar(10) DEFAULT NULL,
  `reminder_time` varchar(5) DEFAULT NULL,
  `frequency` enum('none','daily','weekly','monthly') NOT NULL DEFAULT 'none',
  `trust_type` enum('checkbox','map','pomodoro') NOT NULL DEFAULT 'checkbox',
  `xp_reward` int(11) DEFAULT 0,
  `xp_penalty` int(11) DEFAULT 0,
  `trust_weightage` int(11) DEFAULT 0,
  `map_lat` float DEFAULT NULL,
  `map_lon` float DEFAULT NULL,
  `pomodoro_duration` int(11) DEFAULT NULL,
  `created_at` varchar(16) NOT NULL,
  `updated_at` varchar(16) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `habits`
--

INSERT INTO `habits` (`habit_id`, `user_id`, `title`, `reminder_date`, `reminder_time`, `frequency`, `trust_type`, `xp_reward`, `xp_penalty`, `trust_weightage`, `map_lat`, `map_lon`, `pomodoro_duration`, `created_at`, `updated_at`) VALUES
(1, 1, 'Go to Gym', '2025-02-21', '17:00', 'none', 'map', 0, 0, 0, 37.7749, -122.419, NULL, '2025-02-22 10:48', NULL),
(2, 2, 'Go to Gym', '2025-02-21', '17:00', 'none', 'map', 0, 0, 0, 37.7749, -122.419, NULL, '2025-02-22 11:00', NULL),
(3, 6, 'Read 10 pages of a book', '2025-05-16', '08:00', 'daily', 'pomodoro', 0, 0, 0, 13.0827, 80.2707, 25, '2025-05-16 07:26', NULL),
(4, 6, 'cart', NULL, NULL, 'daily', 'checkbox', 0, 0, 0, NULL, NULL, NULL, '2025-05-19 16:49', NULL),
(5, 6, 'gokarna', NULL, NULL, 'daily', 'checkbox', 0, 0, 0, NULL, NULL, NULL, '2025-05-19 16:49', NULL),
(6, 6, 'checj2', NULL, NULL, 'daily', 'checkbox', 0, 0, 0, NULL, NULL, NULL, '2025-05-19 16:50', NULL),
(7, 6, 'chechbox check1', NULL, NULL, 'daily', 'checkbox', 0, 0, 0, NULL, NULL, NULL, '2025-05-19 16:56', NULL),
(8, 6, 'habit check 1', NULL, NULL, 'daily', 'checkbox', 0, 0, 0, NULL, NULL, NULL, '2025-05-19 17:34', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `rewards`
--

CREATE TABLE `rewards` (
  `reward_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `activity_type` enum('reminder','workflow','habit') NOT NULL,
  `activity_id` int(11) NOT NULL,
  `xp_reward` int(11) DEFAULT 0,
  `xp_penalty` int(11) DEFAULT 0,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `rewards`
--

INSERT INTO `rewards` (`reward_id`, `user_id`, `activity_type`, `activity_id`, `xp_reward`, `xp_penalty`, `reason`, `created_at`) VALUES
(1, 1, 'reminder', 1, 23, 17, 'Completed On Time', '2025-04-25 11:56:44');

-- --------------------------------------------------------

--
-- Table structure for table `tasks`
--

CREATE TABLE `tasks` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `task_type` enum('reminder','workflow','habit') NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `reminder_xp_reward` int(11) DEFAULT 0,
  `reminder_xp_penalty` int(11) DEFAULT 0,
  `workflow_xp_reward` int(11) DEFAULT 0,
  `workflow_xp_penalty` int(11) DEFAULT 0,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `due_date` datetime DEFAULT NULL,
  `status` enum('pending','completed','skipped') DEFAULT 'pending',
  `repeat_frequency` enum('none','daily','weekly','monthly') DEFAULT 'none',
  `priority` enum('low','medium','high') DEFAULT 'medium',
  `extended_time` int(11) DEFAULT 0,
  `is_extended` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `current_streak` int(11) DEFAULT 0,
  `parent_task_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tasks`
--

INSERT INTO `tasks` (`id`, `user_id`, `task_type`, `title`, `description`, `reminder_xp_reward`, `reminder_xp_penalty`, `workflow_xp_reward`, `workflow_xp_penalty`, `start_time`, `end_time`, `due_date`, `status`, `repeat_frequency`, `priority`, `extended_time`, `is_extended`, `created_at`, `updated_at`, `current_streak`, `parent_task_id`) VALUES
(25, 6, '', 'help', '', 0, 0, 0, 0, '2025-05-13 00:00:00', '2025-05-13 00:00:00', '2025-05-13 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-13 18:33:49', '2025-05-14 09:18:02', 0, NULL),
(26, 6, 'workflow', 'ho', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-13 18:34:29', '2025-05-14 15:04:45', 0, NULL),
(27, 6, 'workflow', 'go for a walk', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-13 18:36:20', '2025-05-14 18:06:37', 0, NULL),
(28, 6, '', 'how', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-13 18:36:27', '2025-05-14 13:58:23', 0, NULL),
(29, 6, '', 'can we', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-13 18:36:59', '2025-05-14 14:37:01', 0, NULL),
(30, 6, '', 'calculator', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-14 03:36:11', '2025-05-14 14:37:02', 0, NULL),
(31, 6, 'workflow', 'walkthere', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-14 03:40:26', '2025-05-14 15:04:40', 0, NULL),
(32, 6, '', 'udhdnk', '', 0, 0, 0, 0, '2025-05-14 00:00:00', '2025-05-14 00:00:00', '2025-05-14 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-14 03:46:51', '2025-05-14 15:04:37', 0, NULL),
(33, 6, 'workflow', 'testfromdb', '', 0, 0, 0, 0, '2025-05-15 00:00:00', '2025-05-15 00:00:00', '2025-05-15 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-14 03:49:31', '2025-05-15 17:27:56', 0, NULL),
(34, 6, '', 'Complete project documentation', 'Finish writing the technical documentation for the Shedulytic project', 0, 0, 0, 0, '2025-05-15 16:12:27', '2025-05-15 16:12:34', '2025-05-15 00:00:00', 'pending', 'none', 'high', 0, 0, '2025-05-15 03:43:01', '2025-05-15 17:28:06', 0, NULL),
(35, 6, '', 'Complete project documentation', 'Finish writing the technical documentation for the Shedulytic project', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-20 00:00:00', 'pending', 'none', 'high', 0, 0, '2025-05-15 11:01:00', '2025-05-15 11:01:00', 0, NULL),
(36, 6, '', 'check1', '', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-15 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-15 17:02:24', '2025-05-15 17:28:03', 0, NULL),
(37, 6, 'workflow', 'hellothere', '', 0, 0, 0, 0, '2025-05-15 20:48:51', '2025-05-15 22:48:36', '2025-05-15 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-15 17:15:11', '2025-05-15 17:28:49', 0, NULL),
(38, 6, 'workflow', 'check2', '', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-15 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-15 17:28:33', '2025-05-15 17:28:48', 0, NULL),
(39, 6, '', 'check3 for Tommorow', '', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-16 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-15 17:29:14', '2025-05-16 08:34:05', 0, NULL),
(40, 6, 'workflow', 'check4', '', 0, 0, 0, 0, '2025-05-16 00:00:00', '2025-05-16 00:00:00', '2025-05-16 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-15 19:38:46', '2025-05-16 08:34:02', 0, NULL),
(41, 6, 'workflow', 'check5fortest', '', 0, 0, 0, 0, '2025-05-16 00:00:00', '2025-05-16 00:00:00', '2025-05-16 00:00:00', 'completed', 'none', 'medium', 0, 0, '2025-05-16 07:52:37', '2025-05-16 08:34:03', 0, NULL),
(42, 6, '', 'chek 6', '', 0, 0, 0, 0, '2025-05-16 00:00:00', '2025-05-16 00:00:00', '2025-05-16 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-16 08:34:18', '2025-05-16 08:34:18', 0, NULL),
(43, 6, '', 'ga at', '', 0, 0, 0, 0, '2025-05-19 00:00:00', '2025-05-19 00:00:00', '2025-05-19 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-19 12:11:12', '2025-05-19 15:33:18', 0, NULL),
(44, 6, 'workflow', 'thahaua', '', 0, 0, 0, 0, '2025-05-19 00:00:00', '2025-05-19 00:00:00', '2025-05-19 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-19 12:20:39', '2025-05-19 15:33:20', 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `tasks_backup`
--

CREATE TABLE `tasks_backup` (
  `id` int(11) NOT NULL DEFAULT 0,
  `user_id` int(11) NOT NULL,
  `task_type` enum('reminder','workflow','habit') NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `reminder_xp_reward` int(11) DEFAULT 0,
  `reminder_xp_penalty` int(11) DEFAULT 0,
  `workflow_xp_reward` int(11) DEFAULT 0,
  `workflow_xp_penalty` int(11) DEFAULT 0,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `due_date` datetime DEFAULT NULL,
  `status` enum('pending','completed','skipped') DEFAULT 'pending',
  `repeat_frequency` enum('none','daily','weekly','monthly') DEFAULT 'none',
  `priority` enum('low','medium','high') DEFAULT 'medium',
  `extended_time` int(11) DEFAULT 0,
  `is_extended` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `current_streak` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tasks_backup`
--

INSERT INTO `tasks_backup` (`id`, `user_id`, `task_type`, `title`, `description`, `reminder_xp_reward`, `reminder_xp_penalty`, `workflow_xp_reward`, `workflow_xp_penalty`, `start_time`, `end_time`, `due_date`, `status`, `repeat_frequency`, `priority`, `extended_time`, `is_extended`, `created_at`, `updated_at`, `current_streak`) VALUES
(1, 2, 'habit', 'go for gym', 'Brigng food on the way.', 0, 0, 0, 0, '2025-02-21 06:00:00', '2025-02-21 07:00:00', '2025-02-21 11:00:00', 'completed', 'none', 'high', 0, 0, '2025-04-25 11:56:44', '2025-04-25 11:56:44', 0),
(11, 6, '', 'lazy', '', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-06 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-06 13:44:55', '2025-05-06 13:44:55', 0),
(12, 6, '', 'commonwealth', '', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-07 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-07 08:20:11', '2025-05-07 08:20:11', 0),
(13, 6, 'workflow', 'right', '', 0, 0, 0, 0, '0000-00-00 00:00:00', '0000-00-00 00:00:00', '2025-05-07 00:00:00', 'pending', 'none', 'medium', 0, 0, '2025-05-07 14:38:56', '2025-05-07 14:38:56', 0);

-- --------------------------------------------------------

--
-- Table structure for table `task_completions`
--

CREATE TABLE `task_completions` (
  `completion_id` int(11) NOT NULL,
  `task_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `completion_date` date NOT NULL,
  `completion_time` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `total_xp_reward` int(11) DEFAULT 0,
  `total_xp_penalty` int(11) DEFAULT 0,
  `net_xp_points` int(11) DEFAULT 0,
  `current_level` int(11) DEFAULT 1,
  `progress_to_next_level` int(11) DEFAULT 0,
  `badges_earned` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`badges_earned`)),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `avatar_url` varchar(255) DEFAULT NULL,
  `xp_points` int(11) DEFAULT 0,
  `level` int(11) DEFAULT 1,
  `streak_count` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `name`, `username`, `email`, `password`, `total_xp_reward`, `total_xp_penalty`, `net_xp_points`, `current_level`, `progress_to_next_level`, `badges_earned`, `created_at`, `updated_at`, `avatar_url`, `xp_points`, `level`, `streak_count`) VALUES
(1, 'John Doe', 'johndoe123', 'johndoe@example.com', 'hashed_pass1', 0, 0, 0, 1, 0, NULL, '2025-04-25 11:56:44', '2025-04-25 11:56:44', NULL, 0, 1, 0),
(2, 'ramesh', 'ramesh123', 'rameshkumar@gmail.com', 'hashed_pass2', 0, 0, 0, 1, 0, NULL, '2025-04-25 11:56:44', '2025-04-25 11:56:44', NULL, 0, 1, 0),
(4, 'mohan', 'testuser', 'testuser@example.com', '$2y$10$FT6/bUJLSQBeHY.7G7q2Bure2kHURw6dEGeQ4ejTtQ/wn8jMP/5ha', 0, 0, 0, 1, 0, NULL, '2025-05-04 16:09:18', '2025-05-04 16:09:18', NULL, 0, 1, 0),
(5, 'mo', 'mo', 'k.mohan701394@gmail.com', '$2y$10$r/dSn6zfLnopxpJ52PsqWO75eUxljgOW/B5Voqw4JKakk3r3ITjq2', 0, 0, 0, 1, 0, NULL, '2025-05-05 10:20:20', '2025-05-05 10:20:20', NULL, 0, 1, 0),
(6, 'Hari', 'Mohan', 'mohank1151.sse@saveetha.com', '$2y$10$JVS7ghczf3KeSzjPVk7jGeDt14Fny2krrbvN0..rQz2JKZkIUdIL6', 122, -5, 130, 1, 25, NULL, '2025-05-05 10:23:45', '2025-05-15 07:22:20', NULL, 0, 1, 4),
(7, 'suresh', 'suresh', 'suresh@gmail.com', '$2y$10$VvzcIUi79uX6xSDSrDZw3OTkh.msdwsQoCJ4rR7cxy6SGB3splOci', 0, 0, 0, 1, 0, NULL, '2025-05-12 10:16:08', '2025-05-12 10:16:08', NULL, 0, 1, 0);

-- --------------------------------------------------------

--
-- Table structure for table `user_activity_log`
--

CREATE TABLE `user_activity_log` (
  `log_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `activity_type` enum('login','task_completed','habit_completed') NOT NULL DEFAULT 'login',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_activity_log`
--

INSERT INTO `user_activity_log` (`log_id`, `user_id`, `activity_type`, `created_at`) VALUES
(1, 6, 'login', '2025-05-12 07:22:11'),
(2, 6, 'login', '2025-05-13 03:48:57'),
(3, 6, 'login', '2025-05-13 18:32:35'),
(4, 6, 'login', '2025-05-14 18:44:51'),
(5, 6, 'login', '2025-05-15 06:54:16'),
(6, 6, 'login', '2025-05-15 19:36:49'),
(7, 6, 'login', '2025-05-19 12:09:11');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `completions`
--
ALTER TABLE `completions`
  ADD PRIMARY KEY (`completion_id`),
  ADD KEY `habit_id` (`habit_id`);

--
-- Indexes for table `habits`
--
ALTER TABLE `habits`
  ADD PRIMARY KEY (`habit_id`);

--
-- Indexes for table `rewards`
--
ALTER TABLE `rewards`
  ADD PRIMARY KEY (`reward_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `tasks`
--
ALTER TABLE `tasks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_tasks_type` (`task_type`),
  ADD KEY `idx_tasks_parent` (`parent_task_id`);

--
-- Indexes for table `task_completions`
--
ALTER TABLE `task_completions`
  ADD PRIMARY KEY (`completion_id`),
  ADD UNIQUE KEY `unique_completion` (`task_id`,`completion_date`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_task_completions_date` (`task_id`,`completion_date`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `user_activity_log`
--
ALTER TABLE `user_activity_log`
  ADD PRIMARY KEY (`log_id`),
  ADD KEY `user_id` (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `completions`
--
ALTER TABLE `completions`
  MODIFY `completion_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `habits`
--
ALTER TABLE `habits`
  MODIFY `habit_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `rewards`
--
ALTER TABLE `rewards`
  MODIFY `reward_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `tasks`
--
ALTER TABLE `tasks`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;

--
-- AUTO_INCREMENT for table `task_completions`
--
ALTER TABLE `task_completions`
  MODIFY `completion_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `user_activity_log`
--
ALTER TABLE `user_activity_log`
  MODIFY `log_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `completions`
--
ALTER TABLE `completions`
  ADD CONSTRAINT `completions_ibfk_1` FOREIGN KEY (`habit_id`) REFERENCES `habits` (`habit_id`) ON DELETE CASCADE;

--
-- Constraints for table `rewards`
--
ALTER TABLE `rewards`
  ADD CONSTRAINT `rewards_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `tasks`
--
ALTER TABLE `tasks`
  ADD CONSTRAINT `tasks_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `tasks_ibfk_2` FOREIGN KEY (`parent_task_id`) REFERENCES `tasks` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `task_completions`
--
ALTER TABLE `task_completions`
  ADD CONSTRAINT `task_completions_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `task_completions_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `user_activity_log`
--
ALTER TABLE `user_activity_log`
  ADD CONSTRAINT `user_activity_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
