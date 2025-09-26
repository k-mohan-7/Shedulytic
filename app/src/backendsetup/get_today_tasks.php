<?php
header("Content-Type: application/json");
require_once 'db_connect.php';

function sendError($message) {
    echo json_encode([
        'status' => 'error',
        'message' => $message
    ]);
    exit;
}

// Get and validate parameters
$userId = isset($_GET['user_id']) ? $_GET['user_id'] : '';
$date = isset($_GET['date']) ? $_GET['date'] : date('Y-m-d');

if (empty($userId)) {
    sendError('User ID is required');
}

try {
    // Get all tasks for today including habits
    $stmt = $conn->prepare("
        SELECT 
            t.id as task_id,
            t.title,
            t.description,
            t.start_time,
            t.end_time,
            t.task_type,
            t.status,
            t.repeat_frequency,
            t.parent_task_id,
            CASE 
                WHEN t.task_type = 'habit' AND t.repeat_frequency != 'none' 
                THEN (SELECT COUNT(*) FROM task_completions tc WHERE tc.task_id = t.id AND tc.completion_date = ?)
                ELSE (t.status = 'completed')
            END as is_completed
        FROM tasks t
        WHERE t.user_id = ? 
        AND DATE(t.due_date) = ?
        ORDER BY t.start_time ASC
    ");

    if (!$stmt) {
        sendError('Failed to prepare statement: ' . $conn->error);
    }

    $stmt->bind_param("sss", $date, $userId, $date);
    
    if (!$stmt->execute()) {
        sendError('Failed to execute query: ' . $stmt->error);
    }

    $result = $stmt->get_result();
    $tasks = array();
    
    while ($row = $result->fetch_assoc()) {
        // Format the task data
        $task = array(
            'task_id' => $row['task_id'],
            'title' => $row['title'],
            'description' => $row['description'],
            'start_time' => $row['start_time'],
            'end_time' => $row['end_time'],
            'task_type' => $row['task_type'],
            'status' => $row['status'],
            'is_completed' => (bool)$row['is_completed'],
            'repeat_frequency' => $row['repeat_frequency']
        );
        
        // Add parent task ID only if it exists
        if ($row['parent_task_id']) {
            $task['parent_task_id'] = $row['parent_task_id'];
        }
        
        $tasks[] = $task;
    }

    // Get streak information for habits
    if (!empty($tasks)) {
        foreach ($tasks as &$task) {
            if ($task['task_type'] === 'habit' && $task['repeat_frequency'] !== 'none') {
                $streakStmt = $conn->prepare("
                    SELECT COUNT(*) as current_streak
                    FROM task_completions tc
                    WHERE tc.task_id = ? OR tc.task_id IN (
                        SELECT id FROM tasks 
                        WHERE parent_task_id = (
                            SELECT COALESCE(parent_task_id, id) 
                            FROM tasks 
                            WHERE id = ?
                        )
                    )
                    AND tc.completion_date <= ?
                    AND tc.completion_date > DATE_SUB(?, INTERVAL 30 DAY)
                    ORDER BY tc.completion_date DESC
                ");
                
                $streakStmt->bind_param("iiss", $task['task_id'], $task['task_id'], $date, $date);
                $streakStmt->execute();
                $streakResult = $streakStmt->get_result();
                $streakData = $streakResult->fetch_assoc();
                
                $task['current_streak'] = (int)$streakData['current_streak'];
                $streakStmt->close();
            }
        }
    }

    echo json_encode([
        'status' => 'success',
        'tasks' => $tasks
    ]);

} catch (Exception $e) {
    sendError('Database error: ' . $e->getMessage());
} finally {
    if (isset($stmt)) {
        $stmt->close();
    }
    if (isset($conn)) {
        $conn->close();
    }
}
?>