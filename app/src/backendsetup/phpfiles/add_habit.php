<?php
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);    $user_id = $data['user_id'] ?? null; // Required
    $title = $data['title'] ?? null;
    $frequency = $data['frequency'] ?? 'none';
    $trust_type = $data['trust_type'] ?? 'checkbox';
    
    // Map app verification method to database enum values
    if ($trust_type === 'location') {
        $trust_type = 'map'; // Convert location to map for database compatibility
    }
    
    $reminder_date = $data['reminder_date'] ?? null;
    $reminder_time = $data['reminder_time'] ?? null;
    $map_lat = $data['map_lat'] ?? null;
    $map_lon = $data['map_lon'] ?? null;
    $pomodoro_duration = $data['pomodoro_duration'] ?? null;

    if (!$user_id || !$title) {
        http_response_code(400);
        echo json_encode(["error" => "Missing required fields: user_id or title"]);
        exit;
    }

    $created_at = date("Y-m-d H:i");

    $sql = "INSERT INTO habits (user_id, title, reminder_date, reminder_time, frequency, trust_type, map_lat, map_lon, pomodoro_duration, created_at) 
            VALUES (:user_id, :title, :reminder_date, :reminder_time, :frequency, :trust_type, :map_lat, :map_lon, :pomodoro_duration, :created_at)";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':user_id' => $user_id,
        ':title' => $title,
        ':reminder_date' => $reminder_date,
        ':reminder_time' => $reminder_time,
        ':frequency' => $frequency,
        ':trust_type' => $trust_type,
        ':map_lat' => $map_lat,
        ':map_lon' => $map_lon,
        ':pomodoro_duration' => $pomodoro_duration,
        ':created_at' => $created_at
    ]);

    http_response_code(201);
    echo json_encode(["message" => "Habit added", "habit_id" => $pdo->lastInsertId()]);
} else {
    http_response_code(405);
    echo json_encode(["error" => "Method not allowed"]);
}
?>