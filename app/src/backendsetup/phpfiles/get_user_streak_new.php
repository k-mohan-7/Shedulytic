<?php
include 'db.php';

// Set headers for cross-origin requests and JSON response
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle OPTIONS requests for CORS preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Get query parameters
    $user_id = $_GET['user_id'] ?? null;
    $start_date = $_GET['start_date'] ?? null;
    $end_date = $_GET['end_date'] ?? null;

    // Validate input
    if (empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID is required.']);
        exit;
    }

    if (empty($start_date) || empty($end_date)) {
        echo json_encode(['status' => 'error', 'message' => 'Start and end dates are required.']);
        exit;
    }

    try {
        // Create user_activity_log table if it doesn't exist
        $pdo->exec("CREATE TABLE IF NOT EXISTS `user_activity_log` (
            `log_id` int(11) NOT NULL AUTO_INCREMENT,
            `user_id` int(11) NOT NULL,
            `activity_type` enum('login','task_completed','habit_completed') NOT NULL DEFAULT 'login',
            `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
            PRIMARY KEY (`log_id`),
            KEY `user_id` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

        // Log today's activity if not already logged
        $today = date('Y-m-d');
        $checkTodayStmt = $pdo->prepare(
            "SELECT COUNT(*) as count 
             FROM user_activity_log 
             WHERE user_id = :user_id 
             AND DATE(created_at) = :today"
        );
        $checkTodayStmt->bindParam(':user_id', $user_id);
        $checkTodayStmt->bindParam(':today', $today);
        $checkTodayStmt->execute();
        $todayResult = $checkTodayStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($todayResult['count'] == 0) {
            // Log today's activity
            $logStmt = $pdo->prepare(
                "INSERT INTO user_activity_log (user_id, activity_type) 
                 VALUES (:user_id, 'login')"
            );
            $logStmt->bindParam(':user_id', $user_id);
            $logStmt->execute();
            error_log("Logged new activity for user $user_id on $today");
        }

        // Create a date range array for all days between start and end date
        $streakData = [];
        $current = new DateTime($start_date);
        $end = new DateTime($end_date);
        $end->modify('+1 day'); // Include end date
        
        while ($current < $end) {
            $dateStr = $current->format('Y-m-d');
            $streakData[$dateStr] = [
                'date' => $dateStr,
                'has_activity' => false
            ];
            $current->modify('+1 day');
        }
        
        // Get user activity logs for the date range
        $activityStmt = $pdo->prepare(
            "SELECT DATE(created_at) as activity_date 
             FROM user_activity_log 
             WHERE user_id = :user_id 
             AND DATE(created_at) BETWEEN :start_date AND :end_date 
             GROUP BY DATE(created_at)"
        );
        $activityStmt->bindParam(':user_id', $user_id);
        $activityStmt->bindParam(':start_date', $start_date);
        $activityStmt->bindParam(':end_date', $end_date);
        $activityStmt->execute();
        $activityLogs = $activityStmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Also get completed tasks for the date range as additional activity indicators
        $taskStmt = $pdo->prepare(
            "SELECT DATE(due_date) as completion_date 
             FROM tasks 
             WHERE user_id = :user_id 
             AND status = 'completed' 
             AND DATE(due_date) BETWEEN :start_date AND :end_date 
             GROUP BY DATE(due_date)"
        );
        $taskStmt->bindParam(':user_id', $user_id);
        $taskStmt->bindParam(':start_date', $start_date);
        $taskStmt->bindParam(':end_date', $end_date);
        $taskStmt->execute();
        $taskCompletions = $taskStmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Mark days with activity
        foreach ($activityLogs as $activity) {
            $date = $activity['activity_date'];
            if (isset($streakData[$date])) {
                $streakData[$date]['has_activity'] = true;
            }
        }
        
        foreach ($taskCompletions as $completion) {
            $date = $completion['completion_date'];
            if (isset($streakData[$date])) {
                $streakData[$date]['has_activity'] = true;
            }
        }
        
        // Calculate streak count
        $streakCount = 0;
        
        // Get all dates with activity in descending order
        $activityDatesStmt = $pdo->prepare(
            "SELECT DATE(created_at) as activity_date 
             FROM user_activity_log 
             WHERE user_id = :user_id 
             ORDER BY activity_date DESC"
        );
        $activityDatesStmt->bindParam(':user_id', $user_id);
        $activityDatesStmt->execute();
        $activityDates = $activityDatesStmt->fetchAll(PDO::FETCH_ASSOC);
        
        if (count($activityDates) > 0) {
            // Check if today has activity
            $todayHasActivity = false;
            $today = new DateTime(date('Y-m-d'));
            
            foreach ($activityDates as $activity) {
                $activityDate = new DateTime($activity['activity_date']);
                if ($activityDate->format('Y-m-d') === $today->format('Y-m-d')) {
                    $todayHasActivity = true;
                    break;
                }
            }
            
            if ($todayHasActivity) {
                $streakCount = 1; // Start with 1 for today
                
                // Check consecutive days backward from today
                $checkDate = clone $today;
                $checkDate->modify('-1 day');
                
                while (true) {
                    $hasActivity = false;
                    $checkDateStr = $checkDate->format('Y-m-d');
                    
                    foreach ($activityDates as $activity) {
                        if ($activity['activity_date'] === $checkDateStr) {
                            $hasActivity = true;
                            break;
                        }
                    }
                    
                    if ($hasActivity) {
                        $streakCount++;
                        $checkDate->modify('-1 day');
                    } else {
                        break;
                    }
                }
            }
        }
        
        // Convert to indexed array for JSON response
        $result = [];
        foreach ($streakData as $date => $data) {
            $result[] = [
                'date' => $date,
                'has_activity' => $data['has_activity']
            ];
        }
        
        // Return the response
        echo json_encode([
            'status' => 'success', 
            'streak_data' => $result,
            'streak_count' => $streakCount
        ]);
        
    } catch (PDOException $e) {
        error_log("Error in get_user_streak.php: " . $e->getMessage());
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>
