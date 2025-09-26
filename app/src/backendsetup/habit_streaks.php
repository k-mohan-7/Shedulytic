<?php
require_once 'db_connection.php';

function getHabitStreak($taskId, $userId) {
    global $conn;
    
    // Get current date in user's timezone
    $currentDate = date('Y-m-d');
    
    // Calculate current streak using the MySQL function
    $query = "SELECT calculate_streak(?, ?) as streak";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("is", $taskId, $currentDate);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();
    
    return $row['streak'];
}

function updateHabitCompletion($taskId, $userId, $isCompleted) {
    global $conn;
    
    $currentDate = date('Y-m-d');
    $response = array();
    
    try {
        $conn->begin_transaction();
        
        if ($isCompleted) {
            // Add completion record
            $query = "INSERT INTO task_completions (task_id, user_id, completion_date) 
                     VALUES (?, ?, ?) 
                     ON DUPLICATE KEY UPDATE completion_date = VALUES(completion_date)";
        } else {
            // Remove completion record
            $query = "DELETE FROM task_completions 
                     WHERE task_id = ? AND user_id = ? AND completion_date = ?";
        }
        
        $stmt = $conn->prepare($query);
        $stmt->bind_param("iss", $taskId, $userId, $currentDate);
        $stmt->execute();
        
        // Get updated streak
        $newStreak = getHabitStreak($taskId, $userId);
        
        // Update task status in tasks table
        $status = $isCompleted ? 'completed' : 'pending';
        $updateQuery = "UPDATE tasks 
                       SET status = ?, current_streak = ? 
                       WHERE task_id = ? AND user_id = ?";
        $stmt = $conn->prepare($updateQuery);
        $stmt->bind_param("siis", $status, $newStreak, $taskId, $userId);
        $stmt->execute();
        
        $conn->commit();
        
        $response['success'] = true;
        $response['streak'] = $newStreak;
        $response['message'] = $isCompleted ? 'Habit completed successfully' : 'Habit uncompleted successfully';
        
    } catch (Exception $e) {
        $conn->rollback();
        $response['success'] = false;
        $response['message'] = 'Error updating habit completion: ' . $e->getMessage();
    }
    
    return $response;
}

// API endpoint for handling habit completion
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $data = json_decode(file_get_contents('php://input'), true);
    
    if (isset($data['task_id']) && isset($data['user_id']) && isset($data['completed'])) {
        $taskId = $data['task_id'];
        $userId = $data['user_id'];
        $isCompleted = $data['completed'];
        
        $result = updateHabitCompletion($taskId, $userId, $isCompleted);
        echo json_encode($result);
        
    } else {
        echo json_encode(array(
            'success' => false,
            'message' => 'Missing required parameters'
        ));
    }
}

// API endpoint for getting habit streak
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if (isset($_GET['task_id']) && isset($_GET['user_id'])) {
        $taskId = $_GET['task_id'];
        $userId = $_GET['user_id'];
        
        $streak = getHabitStreak($taskId, $userId);
        echo json_encode(array(
            'success' => true,
            'streak' => $streak
        ));
        
    } else {
        echo json_encode(array(
            'success' => false,
            'message' => 'Missing required parameters'
        ));
    }
}
?>