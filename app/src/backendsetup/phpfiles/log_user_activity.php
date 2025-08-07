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

    if (empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID is required.']);
        exit;
    }

    try {
        // Check if the user_activity_log table exists
        $tableExists = false;
        try {
            $stmt = $pdo->query("SHOW TABLES LIKE 'user_activity_log'");
            $tableExists = ($stmt->rowCount() > 0);
        } catch (PDOException $e) {
            error_log("Error checking if table exists: " . $e->getMessage());
        }
        
        // If the table doesn't exist, create it
        if (!$tableExists) {
            try {
                $pdo->exec("CREATE TABLE `user_activity_log` (
                    `log_id` int(11) NOT NULL AUTO_INCREMENT,
                    `user_id` int(11) NOT NULL,
                    `activity_type` enum('login','task_completed','habit_completed') NOT NULL DEFAULT 'login',
                    `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
                    PRIMARY KEY (`log_id`),
                    KEY `user_id` (`user_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
                
                // Try to add the foreign key constraint
                try {
                    $pdo->exec("ALTER TABLE `user_activity_log` 
                        ADD CONSTRAINT `user_activity_log_ibfk_1` 
                        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE");
                } catch (PDOException $e) {
                    // If adding the constraint fails, just log it but continue
                    error_log("Warning: Could not add foreign key constraint: " . $e->getMessage());
                }
            } catch (PDOException $e) {
                error_log("Error creating user_activity_log table: " . $e->getMessage());
                // Continue anyway - we'll handle the case where the table doesn't exist
            }
        }
        
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
                "INSERT INTO user_activity_log (user_id) VALUES (:user_id)"
            );
            $stmt->bindParam(':user_id', $user_id);
            $stmt->execute();
            error_log("Logged new activity for user $user_id");
        } else {
            error_log("Activity already logged today for user $user_id");
        }

        echo json_encode(['status' => 'success', 'message' => 'Activity logged successfully']);
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}