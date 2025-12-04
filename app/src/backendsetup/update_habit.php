<?php
/**
 * Update Habit Completion Status and Award XP
 * 
 * Handles habit completion updates including:
 * - Updating habit completion status
 * - Recording completion in habit_completions table
 * - Awarding XP based on verification type
 * - Updating streak information
 */

require_once 'db_connection.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Only accept POST requests
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        'status' => 'error',
        'message' => 'Only POST method is allowed'
    ]);
    exit();
}

// Get JSON input
$input = file_get_contents('php://input');
$data = json_decode($input, true);

// Validate required fields
if (!isset($data['habit_id']) || !isset($data['user_id'])) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Missing required fields: habit_id and user_id are required'
    ]);
    exit();
}

$habitId = $data['habit_id'];
$userId = $data['user_id'];
$completed = isset($data['completed']) ? (int)$data['completed'] : 1;
$currentStreak = isset($data['current_streak']) ? (int)$data['current_streak'] : 0;
$totalCompletions = isset($data['total_completions']) ? (int)$data['total_completions'] : 0;
$verificationType = isset($data['verification_type']) ? $data['verification_type'] : 'checkbox';
$completionDate = isset($data['date']) ? $data['date'] : date('Y-m-d');

try {
    // Start transaction
    $conn->begin_transaction();
    
    // XP reward based on verification type
    $xpReward = 1.0; // Default for checkbox
    if ($verificationType === 'pomodoro') {
        $xpReward = 2.0;
    } else if ($verificationType === 'location' || $verificationType === 'map') {
        $xpReward = 1.5;
    }
    
    if ($completed) {
        // Record completion in habit_completions table
        $completionQuery = "INSERT INTO habit_completions (habit_id, user_id, completion_date) 
                           VALUES (?, ?, ?) 
                           ON DUPLICATE KEY UPDATE completion_date = VALUES(completion_date)";
        $stmt = $conn->prepare($completionQuery);
        if ($stmt) {
            $stmt->bind_param("sis", $habitId, $userId, $completionDate);
            $stmt->execute();
            $stmt->close();
        }
        
        // Update user XP
        $xpQuery = "UPDATE users SET xp_points = COALESCE(xp_points, 0) + ? WHERE user_id = ?";
        $stmt = $conn->prepare($xpQuery);
        if ($stmt) {
            $stmt->bind_param("di", $xpReward, $userId);
            $stmt->execute();
            $stmt->close();
        }
        
        // Update user activity for streak tracking
        $activityQuery = "INSERT INTO user_activity (user_id, activity_date, activity_type, activity_count)
                         VALUES (?, ?, 'habit_completion', 1)
                         ON DUPLICATE KEY UPDATE activity_count = activity_count + 1";
        $stmt = $conn->prepare($activityQuery);
        if ($stmt) {
            $stmt->bind_param("is", $userId, $completionDate);
            $stmt->execute();
            $stmt->close();
        }
    } else {
        // Remove completion record if unmarking
        $deleteQuery = "DELETE FROM habit_completions 
                       WHERE habit_id = ? AND user_id = ? AND completion_date = ?";
        $stmt = $conn->prepare($deleteQuery);
        if ($stmt) {
            $stmt->bind_param("sis", $habitId, $userId, $completionDate);
            $stmt->execute();
            $stmt->close();
        }
    }
    
    // Calculate streak from habit_completions
    $streakQuery = "SELECT COUNT(DISTINCT completion_date) as streak_days
                   FROM habit_completions 
                   WHERE habit_id = ? AND user_id = ? 
                   AND completion_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)";
    $stmt = $conn->prepare($streakQuery);
    $newStreak = 0;
    if ($stmt) {
        $stmt->bind_param("si", $habitId, $userId);
        $stmt->execute();
        $result = $stmt->get_result();
        if ($row = $result->fetch_assoc()) {
            $newStreak = $row['streak_days'];
        }
        $stmt->close();
    }
    
    // Update streak count in users table
    if ($completed) {
        $updateStreakQuery = "UPDATE users SET streak_count = GREATEST(COALESCE(streak_count, 0), ?) WHERE user_id = ?";
        $stmt = $conn->prepare($updateStreakQuery);
        if ($stmt) {
            $stmt->bind_param("ii", $newStreak, $userId);
            $stmt->execute();
            $stmt->close();
        }
    }
    
    // Commit transaction
    $conn->commit();
    
    // Get updated user XP
    $userQuery = "SELECT xp_points, streak_count FROM users WHERE user_id = ?";
    $stmt = $conn->prepare($userQuery);
    $totalXp = 0;
    $userStreak = 0;
    if ($stmt) {
        $stmt->bind_param("i", $userId);
        $stmt->execute();
        $result = $stmt->get_result();
        if ($row = $result->fetch_assoc()) {
            $totalXp = $row['xp_points'] ?? 0;
            $userStreak = $row['streak_count'] ?? 0;
        }
        $stmt->close();
    }
    
    echo json_encode([
        'status' => 'success',
        'message' => $completed ? 'Habit marked as complete' : 'Habit marked as incomplete',
        'data' => [
            'habit_id' => $habitId,
            'completed' => $completed,
            'xp_earned' => $completed ? $xpReward : 0,
            'total_xp' => $totalXp,
            'streak' => $newStreak,
            'user_streak' => $userStreak
        ]
    ]);
    
} catch (Exception $e) {
    $conn->rollback();
    echo json_encode([
        'status' => 'error',
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}

$conn->close();
?>
