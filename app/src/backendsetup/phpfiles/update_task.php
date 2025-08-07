<?php
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw POST data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Extract input data
    $task_id = $data['task_id'] ?? null;
    $status = $data['status'] ?? null;
    $priority = $data['priority'] ?? null;
    $start_time = $data['start_time'] ?? null;
    $end_time = $data['end_time'] ?? null;

    // Validate inputs
    if (empty($task_id)) {
        echo json_encode(['status' => 'error', 'message' => 'Task ID is required.']);
        exit;
    }

    // Build the SQL query dynamically based on provided fields
    $updates = [];
    $params = [];

    if (!empty($status)) {
        $updates[] = "status = :status";
        $params[':status'] = $status;
    }
    if (!empty($priority)) {
        $updates[] = "priority = :priority";
        $params[':priority'] = $priority;
    }
    if (!empty($start_time)) {
        $updates[] = "start_time = :start_time";
        $params[':start_time'] = $start_time;
    }
    if (!empty($end_time)) {
        $updates[] = "end_time = :end_time";
        $params[':end_time'] = $end_time;
    }

    if (empty($updates)) {
        echo json_encode(['status' => 'error', 'message' => 'No fields to update.']);
        exit;
    }

    // Add task_id to params
    $params[':task_id'] = $task_id;

    // Construct the SQL query
    $sql = "UPDATE tasks SET " . implode(", ", $updates) . " WHERE id = :task_id";

    // Execute the query
    try {
        $stmt = $pdo->prepare($sql);
        foreach ($params as $key => $value) {
            $stmt->bindValue($key, $value);
        }

        if ($stmt->execute()) {
            echo json_encode(['status' => 'success', 'message' => 'Task updated successfully.']);
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Failed to update task.']);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>