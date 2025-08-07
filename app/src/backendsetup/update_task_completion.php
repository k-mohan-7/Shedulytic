<?php
header("Content-Type: application/json");
require_once 'db_connect.php';

function sendResponse($status, $message, $data = null) {
    $response = array(
        'status' => $status,
        'message' => $message
    );
    if ($data !== null) {
        $response['data'] = $data;
    }
    echo json_encode($response);
    exit;
}

// Get POST data
$input = json_decode(file_get_contents('php://input'), true);

if (!$input) {
    sendResponse('error', 'Invalid input data');
}

// Validate required fields
if (!isset($input['task_id']) || !isset($input['user_id']) || !isset($input['completion_date'])) {
    sendResponse('error', 'Missing required fields');
}

$taskId = $input['task_id'];
$userId = $input['user_id'];
$completionDate = $input['completion_date'];
$action = isset($input['action']) ? $input['action'] : 'complete'; // complete or uncomplete

try {
    // Start transaction
    $conn->begin_transaction();

    // First, verify this is a habit task
    $taskStmt = $conn->prepare("SELECT task_type, parent_task_id FROM tasks WHERE task_id = ? AND user_id = ?");
    $taskStmt->bind_param("is", $taskId, $userId);
    $taskStmt->execute();
    $taskResult = $taskStmt->get_result();
    
    if ($taskResult->num_rows === 0) {
        $conn->rollback();
        sendResponse('error', 'Task not found');
    }

    $taskData = $taskResult->fetch_assoc();
    if ($taskData['task_type'] !== 'habit') {
        $conn->rollback();
        sendResponse('error', 'This operation is only valid for habit tasks');
    }

    // Get the root habit task ID (for streak calculation)
    $rootTaskId = $taskData['parent_task_id'] ?? $taskId;

    if ($action === 'complete') {
        // Add completion record
        $completionStmt = $conn->prepare("INSERT INTO task_completions (task_id, user_id, completion_date) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE completion_time = CURRENT_TIMESTAMP");
        $completionStmt->bind_param("iss", $taskId, $userId, $completionDate);
        
        if (!$completionStmt->execute()) {
            $conn->rollback();
            sendResponse('error', 'Failed to record completion');
        }
    } else {
        // Remove completion record
        $uncompletionStmt = $conn->prepare("DELETE FROM task_completions WHERE task_id = ? AND user_id = ? AND completion_date = ?");
        $uncompletionStmt->bind_param("iss", $taskId, $userId, $completionDate);
        
        if (!$uncompletionStmt->execute()) {
            $conn->rollback();
            sendResponse('error', 'Failed to remove completion');
        }
    }

    // Calculate current streak
    $streakStmt = $conn->prepare("
        WITH RECURSIVE dates AS (
            SELECT completion_date, @row := @row + 1 as row_num
            FROM task_completions
            CROSS JOIN (SELECT @row := 0) r
            WHERE (task_id = ? OR task_id IN (SELECT task_id FROM tasks WHERE parent_task_id = ?))
            AND completion_date <= ?
            ORDER BY completion_date DESC
        )
        SELECT COUNT(*) as streak_count
        FROM dates d1
        WHERE NOT EXISTS (
            SELECT 1 FROM dates d2
            WHERE d2.row_num = d1.row_num + 1
            AND DATEDIFF(d1.completion_date, d2.completion_date) > 1
        )
    ");

    $streakStmt->bind_param("iis", $rootTaskId, $rootTaskId, $completionDate);
    $streakStmt->execute();
    $streakResult = $streakStmt->get_result();
    $streakData = $streakResult->fetch_assoc();
    $currentStreak = $streakData['streak_count'];

    // Commit transaction
    $conn->commit();

    sendResponse('success', 'Habit status updated', array(
        'task_id' => $taskId,
        'current_streak' => $currentStreak,
        'completion_date' => $completionDate,
        'action' => $action
    ));

} catch (Exception $e) {
    $conn->rollback();
    sendResponse('error', 'Database error: ' . $e->getMessage());
} finally {
    if (isset($taskStmt)) $taskStmt->close();
    if (isset($completionStmt)) $completionStmt->close();
    if (isset($uncompletionStmt)) $uncompletionStmt->close();
    if (isset($streakStmt)) $streakStmt->close();
    if (isset($conn)) $conn->close();
}
?>