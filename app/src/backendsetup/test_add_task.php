<?php
// Test script for task addition functionality
include 'phpfiles/db.php';

// Set headers for cross-origin requests and JSON response
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Test parameters
$user_id = 1; // Replace with a valid user ID from your database
$task_type = "task";
$title = "Test Task " . date('Y-m-d H:i:s');
$description = "This is a test task created by the test script";
$start_time = date('H:i:s');
$end_time = date('H:i:s', strtotime('+1 hour'));
$due_date = date('Y-m-d');
$status = "pending";
$repeat_frequency = "none";
$priority = "medium";

// Log test parameters
error_log("Testing task addition with: user_id=$user_id, title=$title, due_date=$due_date");

// Test database connection
try {
    // Check if PDO connection is working
    if (!isset($pdo)) {
        echo json_encode(['status' => 'error', 'message' => 'PDO connection not established']);
        exit;
    }
    
    // Test query to check if the database is accessible
    $testStmt = $pdo->query("SELECT 1");
    if (!$testStmt) {
        echo json_encode(['status' => 'error', 'message' => 'Database query failed']);
        exit;
    }
    
    // Insert test task
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
        
        // Log success
        error_log("Task added successfully with ID: $task_id");
        
        // Now try to retrieve the task to verify it was added correctly
        $verifyStmt = $pdo->prepare("SELECT * FROM tasks WHERE id = :task_id");
        $verifyStmt->bindParam(':task_id', $task_id);
        $verifyStmt->execute();
        $task = $verifyStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($task) {
            echo json_encode([
                'status' => 'success',
                'message' => 'Task added and verified successfully.',
                'task_id' => $task_id,
                'task' => $task
            ]);
        } else {
            echo json_encode([
                'status' => 'warning',
                'message' => 'Task was added but could not be verified.',
                'task_id' => $task_id
            ]);
        }
    } else {
        // Log error
        error_log("Failed to add task. Error: " . json_encode($stmt->errorInfo()));
        
        echo json_encode([
            'status' => 'error',
            'message' => 'Failed to add task: ' . json_encode($stmt->errorInfo())
        ]);
    }
    
} catch (PDOException $e) {
    error_log("PDO Error: " . $e->getMessage());
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
