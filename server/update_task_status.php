<?php
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw POST data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Extract input data
    $task_id = $data['task_id'] ?? null;
    $status = $data['status'] ?? null;

    // Validate inputs
    if (empty($task_id)) {
        echo json_encode(['status' => 'error', 'message' => 'Task ID is required.']);
        exit;
    }

    if (empty($status)) {
        echo json_encode(['status' => 'error', 'message' => 'Status is required.']);
        exit;
    }

    // Valid status values
    $validStatuses = ['pending', 'in_progress', 'completed', 'cancelled', 'skipped_today'];
    if (!in_array($status, $validStatuses)) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid status value.']);
        exit;
    }

    try {
        // Update task status
        $sql = "UPDATE tasks SET status = :status WHERE id = :task_id";
        $stmt = $pdo->prepare($sql);
        $stmt->bindValue(':status', $status);
        $stmt->bindValue(':task_id', $task_id);

        if ($stmt->execute()) {
            // Check if any row was actually updated
            if ($stmt->rowCount() > 0) {
                echo json_encode([
                    'status' => 'success', 
                    'message' => 'Task status updated successfully.',
                    'data' => [
                        'task_id' => $task_id,
                        'new_status' => $status,
                        'updated_at' => date('Y-m-d H:i:s')
                    ]
                ]);
            } else {
                echo json_encode(['status' => 'error', 'message' => 'Task not found or status unchanged.']);
            }
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Failed to update task status.']);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
    }
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method. Only POST is allowed.']);
}
?>
