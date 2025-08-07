<?php
// Set content type header before any output to prevent HTML in response
header("Content-Type: application/json");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

// Disable error reporting in production to prevent HTML errors in JSON
error_reporting(0);
ini_set('display_errors', 0);

// Include database connection
include 'db.php';

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

    // Fetch streak data for the date range
    try {
        // Get completed tasks for the date range
        $taskStmt = $pdo->prepare(
            "SELECT DATE(due_date) as completion_date, COUNT(*) as count 
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
        
        // Get completed habits for the date range
        $habitStmt = $pdo->prepare(
            "SELECT c.date as completion_date, COUNT(*) as count 
             FROM completions c 
             JOIN habits h ON c.habit_id = h.habit_id 
             WHERE h.user_id = :user_id 
             AND c.progress = 'completed' 
             AND c.date BETWEEN :start_date AND :end_date 
             GROUP BY c.date"
        );
        $habitStmt->bindParam(':user_id', $user_id);
        $habitStmt->bindParam(':start_date', $start_date);
        $habitStmt->bindParam(':end_date', $end_date);
        $habitStmt->execute();
        $habitCompletions = $habitStmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Get user's daily activity log
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
        $activityDates = $activityStmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Calculate current streak
        $streakCount = 0;
        $today = new DateTime();
        $yesterday = new DateTime('-1 day');
        $wasActiveYesterday = false;
        $isActiveToday = false;
        
        foreach ($activityDates as $activity) {
            $activityDate = new DateTime($activity['activity_date']);
            if ($activityDate->format('Y-m-d') === $today->format('Y-m-d')) {
                $isActiveToday = true;
            }
            if ($activityDate->format('Y-m-d') === $yesterday->format('Y-m-d')) {
                $wasActiveYesterday = true;
            }
        }
        
        if ($isActiveToday) {
            $streakCount = 1;
            $checkDate = clone $yesterday;
            while ($wasActiveYesterday) {
                $streakCount++;
                $checkDate->modify('-1 day');
                $wasActiveYesterday = false;
                foreach ($activityDates as $activity) {
                    if ($checkDate->format('Y-m-d') === (new DateTime($activity['activity_date']))->format('Y-m-d')) {
                        $wasActiveYesterday = true;
                        break;
                    }
                }
            }
        }
        
        // Update user's streak count in database
        $updateStmt = $pdo->prepare("UPDATE users SET streak_count = :streak_count WHERE user_id = :user_id");
        $updateStmt->bindParam(':streak_count', $streakCount);
        $updateStmt->bindParam(':user_id', $user_id);
        $updateStmt->execute();
        
        // Combine the results
        $streakData = [];
        
        // Initialize all dates in the range with 0 completions
        $current = new DateTime($start_date);
        $end = new DateTime($end_date);
        $end->modify('+1 day'); // Include end date
        
        while ($current < $end) {
            $dateStr = $current->format('Y-m-d');
            $streakData[$dateStr] = ['date' => $dateStr, 'completions' => 0, 'has_activity' => false];
            $current->modify('+1 day');
        }
        
        // Add task completions
        foreach ($taskCompletions as $completion) {
            $date = $completion['completion_date'];
            if (isset($streakData[$date])) {
                $streakData[$date]['completions'] += (int)$completion['count'];
                $streakData[$date]['has_activity'] = true;
            }
        }
        
        // Add habit completions
        foreach ($habitCompletions as $completion) {
            $date = $completion['completion_date'];
            if (isset($streakData[$date])) {
                $streakData[$date]['completions'] += (int)$completion['count'];
                $streakData[$date]['has_activity'] = true;
            }
        }
        
        // Convert activity dates to the format needed by the app
        foreach ($activityDates as $activity) {
            $date = $activity['activity_date'];
            if (isset($streakData[$date])) {
                $streakData[$date]['has_activity'] = true;
            }
        }
        
        // Convert to indexed array for JSON response
        $result = array_values($streakData);
        
        // Ensure we have a valid JSON response
        echo json_encode([
            'status' => 'success', 
            'streak_data' => $result,
            'streak_count' => (int)$streakCount
        ]);
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method.']);
}
?>
