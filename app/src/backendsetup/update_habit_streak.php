<?php
/**
 * Update Habit Streak
 * 
 * Handles updating streak information for habits
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
$completionDate = isset($data['date']) ? $data['date'] : date('Y-m-d');

try {
    // Calculate consecutive streak
    $streakQuery = "
        SELECT COUNT(*) as streak_count
        FROM (
            SELECT completion_date,
                   DATE_SUB(completion_date, INTERVAL ROW_NUMBER() OVER (ORDER BY completion_date DESC) DAY) as grp
            FROM habit_completions 
            WHERE habit_id = ? AND user_id = ?
            ORDER BY completion_date DESC
        ) t
        WHERE grp = (
            SELECT DATE_SUB(MAX(completion_date), INTERVAL 1 DAY)
            FROM habit_completions
            WHERE habit_id = ? AND user_id = ?
        )
        OR completion_date = CURDATE()
    ";
    
    // Simplified streak calculation - count recent consecutive days
    $simpleStreakQuery = "
        SELECT COUNT(DISTINCT completion_date) as streak
        FROM habit_completions 
        WHERE habit_id = ? AND user_id = ?
        AND completion_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
    ";
    
    $stmt = $conn->prepare($simpleStreakQuery);
    $streak = 0;
    if ($stmt) {
        $stmt->bind_param("si", $habitId, $userId);
        $stmt->execute();
        $result = $stmt->get_result();
        if ($row = $result->fetch_assoc()) {
            $streak = $row['streak'];
        }
        $stmt->close();
    }
    
    // Update user streak if this is higher
    $updateUserStreak = "UPDATE users SET streak_count = GREATEST(COALESCE(streak_count, 0), ?) WHERE user_id = ?";
    $stmt = $conn->prepare($updateUserStreak);
    if ($stmt) {
        $stmt->bind_param("ii", $streak, $userId);
        $stmt->execute();
        $stmt->close();
    }
    
    echo json_encode([
        'status' => 'success',
        'message' => 'Streak updated successfully',
        'data' => [
            'habit_id' => $habitId,
            'streak' => $streak,
            'completed' => $completed
        ]
    ]);
    
} catch (Exception $e) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Database error: ' . $e->getMessage()
    ]);
}

$conn->close();
?>
