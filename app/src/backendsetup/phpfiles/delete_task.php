<?php
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw POST data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Extract input data
    $task_id = $data['task_id'] ?? null;

    // Validate inputs
    if (empty($task_id)) {
        echo json_encode(['status' => 'error', 'message' => 'Task ID is required.']);
        exit;
    }

    // Delete the task from the database
    try {
        $stmt = $pdo->prepare("DELETE FROM tasks WHERE id = :task_id");
        $stmt->bindParam(':task_id', $task_id);

        if ($stmt->execute()) {
            echo json_encode(['status' => 'success', 'message' => 'Task deleted successfully.']);
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Failed to delete task.']);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>