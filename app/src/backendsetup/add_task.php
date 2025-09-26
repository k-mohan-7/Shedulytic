<?php
header("Content-Type: application/json");
require_once 'db_connect.php';

function sendResponse($status, $message, $data = null) {
    $response = array(
        'status' => $status,
        'message' => $message
    );
    if ($data !== null) {
        $response['data'] = $data;
    }
    echo json_encode($response);
    exit;
}

// Get POST data
$input = json_decode(file_get_contents('php://input'), true);

if (!$input) {
    sendResponse('error', 'Invalid input data');
}

// Validate required fields
$requiredFields = ['title', 'start_time', 'end_time', 'due_date', 'task_type', 'user_id'];
foreach ($requiredFields as $field) {
    if (!isset($input[$field]) || empty($input[$field])) {
        sendResponse('error', "Missing required field: $field");
    }
}

// Extract data
$title = $input['title'];
$description = isset($input['description']) ? $input['description'] : '';
$startTime = $input['start_time'];
$endTime = $input['end_time'];
$taskDate = $input['due_date'];
$taskType = strtolower($input['task_type']); // Convert to lowercase for consistency
$userId = $input['user_id'];
$repeatFreq = isset($input['repeat_frequency']) ? $input['repeat_frequency'] : 'none';

// Validate task type
$validTypes = ['workflow', 'reminder', 'habit'];
if (!in_array($taskType, $validTypes)) {
    sendResponse('error', 'Invalid task type: ' . $taskType . '. Valid types are: ' . implode(', ', $validTypes));
}

try {
    // Prepare statement
    $stmt = $conn->prepare("INSERT INTO tasks (user_id, title, description, start_time, end_time, due_date, task_type, repeat_frequency, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending')");
    
    if (!$stmt) {
        sendResponse('error', 'Failed to prepare statement: ' . $conn->error);
    }
    
    $stmt->bind_param("ssssssss", $userId, $title, $description, $startTime, $endTime, $taskDate, $taskType, $repeatFreq);
    
    if ($stmt->execute()) {
        $taskId = $stmt->insert_id;
        
        // If it's a habit, create recurring entries
        if ($taskType === 'habit' && $repeatFreq !== 'none') {
            createRecurringHabits($conn, $taskId, $input);
        }
        
        sendResponse('success', 'Task added successfully', array('task_id' => $taskId));
    } else {
        sendResponse('error', 'Failed to add task: ' . $stmt->error);
    }
    
} catch (Exception $e) {
    sendResponse('error', 'Database error: ' . $e->getMessage());
} finally {
    if (isset($stmt)) {
        $stmt->close();
    }
    if (isset($conn)) {
        $conn->close();
    }
}

function createRecurringHabits($conn, $parentTaskId, $taskData) {
    $startDate = new DateTime($taskData['due_date']);
    $endDate = clone $startDate;
    
    switch ($taskData['repeat_frequency']) {
        case 'daily':
            $endDate->modify('+30 days');
            $interval = 'P1D';
            break;
        case 'weekly':
            $endDate->modify('+12 weeks');
            $interval = 'P1W';
            break;
        case 'monthly':
            $endDate->modify('+12 months');
            $interval = 'P1M';
            break;
        default:
            return;
    }
    
    $period = new DatePeriod(
        $startDate->modify('+1 day'),
        new DateInterval($interval),
        $endDate
    );
    
    $stmt = $conn->prepare("INSERT INTO tasks (user_id, title, description, start_time, end_time, due_date, task_type, repeat_frequency, parent_task_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')");
    
    foreach ($period as $date) {
        $taskDate = $date->format('Y-m-d');
        $stmt->bind_param("ssssssssi", 
            $taskData['user_id'],
            $taskData['title'],
            $taskData['description'],
            $taskData['start_time'],
            $taskData['end_time'],
            $taskDate,
            $taskData['task_type'],
            $taskData['repeat_frequency'],
            $parentTaskId
        );
        $stmt->execute();
    }
    
    $stmt->close();
}
?>