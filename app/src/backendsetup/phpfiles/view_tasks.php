<?php
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Get query parameters
    $user_id = $_GET['user_id'] ?? null;

    // Validate input
    if (empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID is required.']);
        exit;
    }    // Fetch tasks from the database
    try {
        $stmt = $pdo->prepare("
            SELECT t.*, 
                   CASE 
                       WHEN tc.completion_id IS NOT NULL THEN 'completed'
                       ELSE t.status 
                   END as status
            FROM tasks t
            LEFT JOIN task_completions tc ON t.id = tc.task_id 
                AND tc.user_id = :user_id
            WHERE t.user_id = :user_id
        ");
        $stmt->bindParam(':user_id', $user_id);
        $stmt->execute();
        $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);

        if ($tasks) {
            echo json_encode(['status' => 'success', 'tasks' => $tasks]);
        } else {
            echo json_encode(['status' => 'success', 'message' => 'No tasks found for this user.']);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>