<?php
// Include DB connection
include 'db.php';

// Handle GET request to fetch habits by user_id
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $user_id = $_GET['user_id'] ?? null;

    if (!$user_id) {
        http_response_code(400);
        echo json_encode(["error" => "Missing user_id"]);
        exit;
    }

    // Get habits with today's completion status
    $sql = "SELECT h.*, 
            CASE WHEN hc.completion_date IS NOT NULL THEN 1 ELSE 0 END as completed_today
            FROM habits h
            LEFT JOIN habit_completions hc 
                ON h.habit_id = hc.habit_id 
                AND hc.user_id = h.user_id 
                AND hc.completion_date = CURDATE()
            WHERE h.user_id = :user_id";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':user_id' => $user_id]);
    $habits = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (empty($habits)) {
        echo json_encode(["message" => "No habits found for user_id $user_id"]);
    } else {
        echo json_encode($habits);
    }
} else {
    http_response_code(405);
    echo json_encode(["error" => "Method not allowed"]);
}
?>