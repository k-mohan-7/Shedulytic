<?php
// update_user_activity.php - Records user activity for streak tracking

// Include database connection
include_once 'database.php';

// Set content type to JSON
header('Content-Type: application/json');

// Check for required parameters
$required = ['user_id', 'date', 'activity_type'];
$missing = [];

foreach ($required as $field) {
    if (!isset($_POST[$field]) || empty($_POST[$field])) {
        $missing[] = $field;
    }
}

if (!empty($missing)) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Missing required fields: ' . implode(', ', $missing)
    ]);
    exit;
}

// Get parameters
$userId = $_POST['user_id'];
$date = $_POST['date'];
$activityType = $_POST['activity_type'];

// Validate date format (YYYY-MM-DD)
if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Invalid date format. Use YYYY-MM-DD.'
    ]);
    exit;
}

try {
    // Check if there's already a record for this date and user
    $checkSql = "SELECT id FROM user_activity_log WHERE user_id = ? AND date = ?";
    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->bind_param("ss", $userId, $date);
    $checkStmt->execute();
    $result = $checkStmt->get_result();
    
    if ($result->num_rows > 0) {
        // Update existing record
        $updateSql = "UPDATE user_activity_log SET activity_type = ?, last_updated = NOW() WHERE user_id = ? AND date = ?";
        $updateStmt = $conn->prepare($updateSql);
        $updateStmt->bind_param("sss", $activityType, $userId, $date);
        $updateStmt->execute();
    } else {
        // Insert new record
        $insertSql = "INSERT INTO user_activity_log (user_id, date, activity_type, created_at) VALUES (?, ?, ?, NOW())";
        $insertStmt = $conn->prepare($insertSql);
        $insertStmt->bind_param("sss", $userId, $date, $activityType);
        $insertStmt->execute();
    }
    
    // Calculate current streak
    $streakCount = calculateUserStreak($conn, $userId);
    
    // Return success
    echo json_encode([
        'status' => 'success',
        'message' => 'Activity logged successfully',
        'date' => $date,
        'user_id' => $userId,
        'activity_type' => $activityType,
        'streak_count' => $streakCount
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
    $activeDates = [];
    
    while ($row = $result->fetch_assoc()) {
        $date = $row['date'];
        $activeDates[] = $date;
        
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