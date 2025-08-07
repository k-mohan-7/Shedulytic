<?php
include 'db.php';

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

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Get query parameters
    $user_id = $_GET['user_id'] ?? null;
    $date = $_GET['date'] ?? date('Y-m-d'); // Default to today if not provided

    // Validate input
    if (empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID is required.']);
        exit;
    }

    // Fetch tasks for the specified date from the database
    try {
        $stmt = $pdo->prepare("
            SELECT t.*, 
                   CASE 
                       WHEN t.task_type = '' OR t.task_type IS NULL THEN 'remainder'
                       ELSE t.task_type 
                   END as task_type,
                   CASE 
                       WHEN tc.completion_id IS NOT NULL THEN 'completed'
                       ELSE t.status 
                   END as status
            FROM tasks t
            LEFT JOIN task_completions tc ON t.id = tc.task_id 
                AND tc.user_id = :user_id 
                AND tc.completion_date = :date
            WHERE t.user_id = :user_id 
            AND DATE(t.due_date) = :date 
            AND (t.task_type = 'workflow' OR t.task_type = 'remainder' OR t.task_type = '' OR t.task_type IS NULL)
        ");
        $stmt->bindParam(':user_id', $user_id);
        $stmt->bindParam(':date', $date);
        $stmt->execute();
        $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);

        // Return only tasks (no habits)
        $response = [
            'status' => 'success',
            'tasks' => $tasks
        ];

        echo json_encode($response);
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>