<?php
/**
 * Complete Task with Reward/Penalty System
 * 
 * This script handles:
 * - Task completion status update
 * - XP reward (+2.0) for on-time completion
 * - XP penalty (-0.5) for skipped tasks
 * - Logging completion time and status
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Database connection
require_once 'db_connect.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['status' => 'error', 'message' => 'Only POST method allowed']);
    exit;
}

// Get JSON input
$input = file_get_contents('php://input');
$data = json_decode($input, true);

// Validate required fields
$task_id = $data['task_id'] ?? null;
$user_id = $data['user_id'] ?? null;
$status = $data['status'] ?? null;
$xp_change = $data['xp_change'] ?? 0;
$completed_at = $data['completed_at'] ?? date('Y-m-d H:i:s');

if (empty($task_id)) {
    echo json_encode(['status' => 'error', 'message' => 'Task ID is required']);
    exit;
}

if (empty($user_id)) {
    echo json_encode(['status' => 'error', 'message' => 'User ID is required']);
    exit;
}

if (empty($status)) {
    echo json_encode(['status' => 'error', 'message' => 'Status is required']);
    exit;
}

// Valid statuses
$valid_statuses = ['pending', 'completed', 'skipped', 'cant_complete', 'in_progress'];
if (!in_array($status, $valid_statuses)) {
    echo json_encode(['status' => 'error', 'message' => 'Invalid status value']);
    exit;
}

try {
    // Start transaction
    $pdo->beginTransaction();
    
    // 1. Update task status
    $update_task_sql = "UPDATE tasks SET 
                        status = :status, 
                        completed_at = :completed_at,
                        updated_at = NOW()
                        WHERE id = :task_id";
    
    $stmt = $pdo->prepare($update_task_sql);
    $stmt->execute([
        ':status' => $status,
        ':completed_at' => ($status === 'completed') ? $completed_at : null,
        ':task_id' => $task_id
    ]);
    
    // Check if task was updated
    if ($stmt->rowCount() === 0) {
        $pdo->rollBack();
        echo json_encode(['status' => 'error', 'message' => 'Task not found or already updated']);
        exit;
    }
    
    // 2. Update user XP points
    $current_xp = 0;
    $new_xp = 0;
    
    // Get current XP
    $get_xp_sql = "SELECT xp_points FROM users WHERE id = :user_id";
    $xp_stmt = $pdo->prepare($get_xp_sql);
    $xp_stmt->execute([':user_id' => $user_id]);
    $user_row = $xp_stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($user_row) {
        $current_xp = floatval($user_row['xp_points']);
        $new_xp = max(0, $current_xp + floatval($xp_change)); // Prevent negative XP
        
        // Update XP
        $update_xp_sql = "UPDATE users SET xp_points = :xp_points WHERE id = :user_id";
        $update_xp_stmt = $pdo->prepare($update_xp_sql);
        $update_xp_stmt->execute([
            ':xp_points' => $new_xp,
            ':user_id' => $user_id
        ]);
    }
    
    // 3. Log the completion in task_completions table (create if not exists)
    $create_log_table = "CREATE TABLE IF NOT EXISTS task_completions (
        id INT AUTO_INCREMENT PRIMARY KEY,
        task_id INT NOT NULL,
        user_id INT NOT NULL,
        status VARCHAR(50) NOT NULL,
        xp_change DECIMAL(5,2) DEFAULT 0,
        completed_at DATETIME,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_user_id (user_id),
        INDEX idx_task_id (task_id),
        INDEX idx_completed_at (completed_at)
    )";
    $pdo->exec($create_log_table);
    
    // Insert completion log
    $log_sql = "INSERT INTO task_completions (task_id, user_id, status, xp_change, completed_at) 
                VALUES (:task_id, :user_id, :status, :xp_change, :completed_at)";
    $log_stmt = $pdo->prepare($log_sql);
    $log_stmt->execute([
        ':task_id' => $task_id,
        ':user_id' => $user_id,
        ':status' => $status,
        ':xp_change' => $xp_change,
        ':completed_at' => $completed_at
    ]);
    
    // 4. If completed on time, update streak
    if ($status === 'completed') {
        // Log activity for streak tracking
        $activity_sql = "INSERT INTO user_activity_log (user_id, activity_type, created_at) 
                         VALUES (:user_id, 'task_completed', NOW())
                         ON DUPLICATE KEY UPDATE created_at = NOW()";
        $activity_stmt = $pdo->prepare($activity_sql);
        $activity_stmt->execute([':user_id' => $user_id]);
    }
    
    // Commit transaction
    $pdo->commit();
    
    // Success response
    echo json_encode([
        'status' => 'success',
        'message' => 'Task ' . $status . ' successfully',
        'data' => [
            'task_id' => $task_id,
            'new_status' => $status,
            'xp_change' => floatval($xp_change),
            'previous_xp' => $current_xp,
            'new_xp' => $new_xp,
            'completed_at' => $completed_at
        ]
    ]);
    
} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Error in complete_task_with_reward.php: " . $e->getMessage());
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Error in complete_task_with_reward.php: " . $e->getMessage());
    echo json_encode(['status' => 'error', 'message' => 'Server error: ' . $e->getMessage()]);
}
?>
