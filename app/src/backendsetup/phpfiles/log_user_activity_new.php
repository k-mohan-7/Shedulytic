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

// Support both POST and GET methods for flexibility
if ($_SERVER['REQUEST_METHOD'] === 'POST' || $_SERVER['REQUEST_METHOD'] === 'GET') {
    // Get user_id from either POST or GET
    $user_id = $_POST['user_id'] ?? $_GET['user_id'] ?? null;
    $activity_type = $_POST['activity_type'] ?? $_GET['activity_type'] ?? 'login';

    if (empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID is required.']);
        exit;
    }

    try {
        // Create the user_activity_log table if it doesn't exist
        $pdo->exec("CREATE TABLE IF NOT EXISTS `user_activity_log` (
            `log_id` int(11) NOT NULL AUTO_INCREMENT,
            `user_id` int(11) NOT NULL,
            `activity_type` enum('login','task_completed','habit_completed') NOT NULL DEFAULT 'login',
            `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
            PRIMARY KEY (`log_id`),
            KEY `user_id` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");

        // Check if we already logged activity for today
        $checkStmt = $pdo->prepare(
            "SELECT COUNT(*) as count 
             FROM user_activity_log 
             WHERE user_id = :user_id 
             AND DATE(created_at) = CURDATE()"
        );
        $checkStmt->bindParam(':user_id', $user_id);
        $checkStmt->execute();
        $result = $checkStmt->fetch(PDO::FETCH_ASSOC);

        if ($result['count'] == 0) {
            // Log new activity for today
            $stmt = $pdo->prepare(
                "INSERT INTO user_activity_log (user_id, activity_type) VALUES (:user_id, :activity_type)"
            );
            $stmt->bindParam(':user_id', $user_id);
            $stmt->bindParam(':activity_type', $activity_type);
            $stmt->execute();
            
            error_log("Logged new activity for user $user_id with type $activity_type");
            echo json_encode(['status' => 'success', 'message' => 'Activity logged successfully', 'new_log' => true]);
        } else {
            error_log("Activity already logged today for user $user_id");
            echo json_encode(['status' => 'success', 'message' => 'Activity already logged for today', 'new_log' => false]);
        }
    } catch (PDOException $e) {
        error_log("Error logging user activity: " . $e->getMessage());
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>
