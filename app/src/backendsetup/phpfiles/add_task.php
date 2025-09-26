<?php
include 'db.php';

// Additional error handling for database connection
if (!isset($pdo)) {
    echo json_encode(['status' => 'error', 'message' => 'Database connection failed']);
    exit;
}

// Set headers for cross-origin requests and JSON response
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle OPTIONS requests for CORS preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw POST data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Extract input data
    $user_id = $data['user_id'] ?? null;
    $task_type = $data['task_type'] ?? null;
    $title = trim($data['title'] ?? '');
    $description = $data['description'] ?? '';
    $start_time = $data['start_time'] ?? null;
    $end_time = $data['end_time'] ?? null;
    $due_date = $data['due_date'] ?? null;
    $status = $data['status'] ?? 'pending';
    $repeat_frequency = $data['repeat_frequency'] ?? 'none';
    $priority = $data['priority'] ?? 'medium';

    // Validate inputs
    if (empty($user_id) || empty($task_type) || empty($title)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID, Task Type, and Title are required.']);
        exit;
    }

    // Insert into database
    try {
        $stmt = $pdo->prepare("
            INSERT INTO tasks (user_id, task_type, title, description, start_time, end_time, due_date, status, repeat_frequency, priority)
            VALUES (:user_id, :task_type, :title, :description, :start_time, :end_time, :due_date, :status, :repeat_frequency, :priority)
        ");
        $stmt->bindParam(':user_id', $user_id);
        $stmt->bindParam(':task_type', $task_type);
        $stmt->bindParam(':title', $title);
        $stmt->bindParam(':description', $description);
        $stmt->bindParam(':start_time', $start_time);
        $stmt->bindParam(':end_time', $end_time);
        $stmt->bindParam(':due_date', $due_date);
        $stmt->bindParam(':status', $status);
        $stmt->bindParam(':repeat_frequency', $repeat_frequency);
        $stmt->bindParam(':priority', $priority);

        if ($stmt->execute()) {
            $task_id = $pdo->lastInsertId();
            
            // Log success for debugging
            error_log("Task added successfully with ID: $task_id for user: $user_id");
            
            // Return success with task ID
            echo json_encode([
                'status' => 'success', 
                'message' => 'Task added successfully.',
                'task_id' => $task_id
            ]);
        } else {
            // Log error for debugging
            error_log("Failed to add task for user: $user_id. Error: " . json_encode($stmt->errorInfo()));
            
            echo json_encode(['status' => 'error', 'message' => 'Failed to add task: ' . json_encode($stmt->errorInfo())]);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>