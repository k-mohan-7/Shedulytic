<?php
// get_user_streak.php - Retrieves user streak information for the app

// Include database connection
include_once 'database.php';

// Set content type to JSON
header('Content-Type: application/json');

// Check for required parameters
if (!isset($_GET['user_id']) || empty($_GET['user_id'])) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Missing required parameter: user_id'
    ]);
    exit;
}

// Get parameters
$userId = $_GET['user_id'];
$startDate = isset($_GET['start_date']) ? $_GET['start_date'] : date('Y-m-d', strtotime('-30 days'));
$endDate = isset($_GET['end_date']) ? $_GET['end_date'] : date('Y-m-d');

// Validate date formats
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $startDate) || !preg_match('/^\d{4}-\d{2}-\d{2}$/', $endDate)) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Invalid date format. Use YYYY-MM-DD.'
    ]);
    exit;
}

try {
    // Get user's current streak count
    $streakSql = "SELECT streak_count FROM users WHERE id = ?";
    $streakStmt = $conn->prepare($streakSql);
    $streakStmt->bind_param("s", $userId);
    $streakStmt->execute();
    $streakResult = $streakStmt->get_result();
    
    $streakCount = 0;
    if ($streakResult->num_rows > 0) {
        $row = $streakResult->fetch_assoc();
        $streakCount = intval($row['streak_count']);
    }
    
    // Get activity log for the date range
    $activitySql = "SELECT date, activity_type FROM user_activity_log 
                   WHERE user_id = ? AND date BETWEEN ? AND ? 
                   ORDER BY date DESC";
    $activityStmt = $conn->prepare($activitySql);
    $activityStmt->bind_param("sss", $userId, $startDate, $endDate);
    $activityStmt->execute();
    $activityResult = $activityStmt->get_result();
    
    // Format data for the app
    $activeDays = [];
    $activityData = [];
    
    while ($row = $activityResult->fetch_assoc()) {
        $date = $row['date'];
        $activityType = $row['activity_type'];
        
        $activeDays[] = $date;
        $activityData[] = [
            'date' => $date,
            'has_activity' => true,
            'activity_type' => $activityType
        ];
    }
    
    // If we have recent activity but no streak count, recalculate it
    if (count($activeDays) > 0 && $streakCount === 0) {
        $streakCount = calculateUserStreak($conn, $userId);
    }
    
    // Return the data
    echo json_encode([
        'status' => 'success',
        'user_id' => $userId,
        'streak_count' => $streakCount,
        'active_days' => $activeDays,
        'streak_data' => $activityData,
        'start_date' => $startDate,
        'end_date' => $endDate
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}

/**
 * Calculate the current streak for a user
 * A streak is defined as consecutive days with activity
 */
function calculateUserStreak($conn, $userId) {
    // Get today's date
    $today = date('Y-m-d');
    
    // Query to get the user's activity log, ordered by date descending
    $sql = "SELECT date FROM user_activity_log WHERE user_id = ? ORDER BY date DESC";
    $stmt = $conn->prepare($sql);
    $stmt->bind_param("s", $userId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    if ($result->num_rows === 0) {
        // No activity records
        return 0;
    }
    
    // Calculate the streak
    $streak = 0;
    $lastDate = null;
    
    while ($row = $result->fetch_assoc()) {
        $date = $row['date'];
        
        if ($lastDate === null) {
            // First iteration
            $lastDate = $date;
            $streak = 1;
            continue;
        }
        
        // Calculate the difference between the last date and the current date
        $lastDateTime = new DateTime($lastDate);
        $currentDateTime = new DateTime($date);
        $diff = $lastDateTime->diff($currentDateTime);
        
        if ($diff->days === 1) {
            // Consecutive day
            $streak++;
            $lastDate = $date;
        } else {
            // Streak broken, stop counting
            break;
        }
    }
    
    // Update user's streak in the users table if it exists
    $updateSql = "UPDATE users SET streak_count = ? WHERE id = ?";
    $updateStmt = $conn->prepare($updateSql);
    $updateStmt->bind_param("is", $streak, $userId);
    $updateStmt->execute();
    
    return $streak;
}
?> 