<?php
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw POST data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Extract input data
    $task_id = $data['task_id'] ?? null;
    $user_id = $data['user_id'] ?? null;
    $date = $data['date'] ?? date('Y-m-d');
    $action = $data['action'] ?? 'complete'; // 'complete' or 'uncomplete'

    // Validate inputs
    if (empty($task_id) || empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'Task ID and User ID are required.']);
        exit;
    }

    try {
        // First, get the task details to validate ownership and get task type
        $taskStmt = $pdo->prepare("SELECT task_type, title FROM tasks WHERE id = :task_id AND user_id = :user_id");
        $taskStmt->execute([':task_id' => $task_id, ':user_id' => $user_id]);
        $taskData = $taskStmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$taskData) {
            echo json_encode(['status' => 'error', 'message' => 'Task not found or access denied.']);
            exit;
        }

        // Fix empty task_type by defaulting to 'remainder'
        $taskType = !empty($taskData['task_type']) ? $taskData['task_type'] : 'remainder';
          if ($action === 'complete') {
            // Start transaction for moving task to task_completions
            $pdo->beginTransaction();
            
            try {
                // Insert completion record
                $insertSql = "INSERT INTO task_completions (task_id, user_id, completion_date, completion_time) 
                             VALUES (:task_id, :user_id, :date, NOW())";
                $insertStmt = $pdo->prepare($insertSql);
                $insertStmt->execute([
                    ':task_id' => $task_id, 
                    ':user_id' => $user_id, 
                    ':date' => $date
                ]);
                
                // Update task status to completed (keep in tasks table for reference)
                $updateSql = "UPDATE tasks SET status = 'completed', updated_at = NOW() WHERE id = :task_id AND user_id = :user_id";
                $updateStmt = $pdo->prepare($updateSql);
                $updateStmt->execute([':task_id' => $task_id, ':user_id' => $user_id]);
                
                $pdo->commit();
                $message = 'Task marked as completed';
            } catch (Exception $e) {
                $pdo->rollBack();
                throw $e;
            }
        } else {
            // Start transaction for uncompleting task
            $pdo->beginTransaction();
            
            try {
                // Remove completion record
                $deleteSql = "DELETE FROM task_completions WHERE task_id = :task_id AND user_id = :user_id AND completion_date = :date";
                $deleteStmt = $pdo->prepare($deleteSql);
                $deleteStmt->execute([
                    ':task_id' => $task_id, 
                    ':user_id' => $user_id, 
                    ':date' => $date
                ]);
                
                // Update task status back to pending
                $updateSql = "UPDATE tasks SET status = 'pending', updated_at = NOW() WHERE id = :task_id AND user_id = :user_id";
                $updateStmt = $pdo->prepare($updateSql);
                $updateStmt->execute([':task_id' => $task_id, ':user_id' => $user_id]);
                
                $pdo->commit();
                $message = 'Task marked as pending';
            } catch (Exception $e) {
                $pdo->rollBack();
                throw $e;
            }
        }
        
        echo json_encode([
            'status' => 'success', 
            'message' => $message,
            'data' => [
                'task_id' => $task_id,
                'action' => $action,
                'date' => $date,
                'task_type' => $taskType
            ]
        ]);

    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
    }
} else {
    echo json_encode(['status' => 'error', 'message' => 'Only POST method allowed']);
}
?>
