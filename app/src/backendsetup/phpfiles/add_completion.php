<?php
// Include DB connection
include 'db.php';

// Handle POST request to add a completion
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);

    $habit_id = $data['habit_id'] ?? null;
    $date = $data['date'] ?? date("Y-m-d"); // Default to today
    $progress = $data['progress'] ?? 'not_started';
    $is_trusted = $data['is_trusted'] ?? 0;

    if (!$habit_id) {
        http_response_code(400);
        echo json_encode(["error" => "Missing habit_id"]);
        exit;
    }

    $sql = "INSERT INTO completions (habit_id, date, progress, is_trusted) 
            VALUES (:habit_id, :date, :progress, :is_trusted)";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':habit_id' => $habit_id,
        ':date' => $date,
        ':progress' => $progress,
        ':is_trusted' => $is_trusted
    ]);

    http_response_code(201);
    echo json_encode(["message" => "Completion added", "completion_id" => $pdo->lastInsertId()]);
} else {
    http_response_code(405);
    echo json_encode(["error" => "Method not allowed"]);
}
?>