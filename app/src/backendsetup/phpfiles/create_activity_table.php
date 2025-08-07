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

// Check if database connection is established
if (!isset($pdo)) {
    echo json_encode(['status' => 'error', 'message' => 'Database connection failed']);
    exit;
}

try {
    // First, check if the table exists and drop it if it does
    $pdo->exec("DROP TABLE IF EXISTS `user_activity_log`");
    
    // Create the user_activity_log table WITHOUT foreign key constraint first
    $pdo->exec("CREATE TABLE `user_activity_log` (
        `log_id` int(11) NOT NULL AUTO_INCREMENT,
        `user_id` int(11) NOT NULL,
        `activity_type` enum('login','task_completed','habit_completed') NOT NULL DEFAULT 'login',
        `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
        PRIMARY KEY (`log_id`),
        KEY `user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
    
    // Now try to add the foreign key constraint separately
    try {
        $pdo->exec("ALTER TABLE `user_activity_log` 
            ADD CONSTRAINT `user_activity_log_ibfk_1` 
            FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE");
        echo json_encode(['status' => 'success', 'message' => 'User activity log table created successfully with foreign key']);
    } catch (PDOException $e) {
        // If adding the constraint fails, log it but consider the table creation successful
        error_log("Warning: Could not add foreign key constraint: " . $e->getMessage());
        echo json_encode(['status' => 'partial_success', 'message' => 'User activity log table created but without foreign key constraint']);
    }
} catch (PDOException $e) {
    echo json_encode(['status' => 'error', 'message' => 'Error creating table: ' . $e->getMessage()]);
}
?>
