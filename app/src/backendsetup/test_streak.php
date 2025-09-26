<?php
// Test script for streak data functionality
include 'phpfiles/db.php';

// Set headers for cross-origin requests and JSON response
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Test parameters
$user_id = 1; // Replace with a valid user ID from your database
$start_date = date('Y-m-d', strtotime('-30 days'));
$end_date = date('Y-m-d');

// Log test parameters
error_log("Testing streak data for user_id: $user_id, start_date: $start_date, end_date: $end_date");

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
    
    // Fetch completed tasks for the date range
    $taskStmt = $pdo->prepare(
        "SELECT DATE(due_date) as completion_date, COUNT(*) as count 
         FROM tasks 
         WHERE user_id = :user_id 
         AND status = 'completed' 
         AND DATE(due_date) BETWEEN :start_date AND :end_date 
         GROUP BY DATE(due_date)"
    );
    $taskStmt->bindParam(':user_id', $user_id);
    $taskStmt->bindParam(':start_date', $start_date);
    $taskStmt->bindParam(':end_date', $end_date);
    $taskStmt->execute();
    $taskCompletions = $taskStmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Log task completions
    error_log("Task completions: " . json_encode($taskCompletions));
    
    // Create streak data structure
    $streakData = [];
    $currentDate = new DateTime($start_date);
    $endDateTime = new DateTime($end_date);
    $endDateTime->modify('+1 day'); // Include end date
    
    // Initialize streak data for all days in range
    while ($currentDate < $endDateTime) {
        $dateStr = $currentDate->format('Y-m-d');
        $streakData[$dateStr] = [
            'date' => $dateStr,
            'has_activity' => false,
            'count' => 0
        ];
        $currentDate->modify('+1 day');
    }
    
    // Update with completed tasks
    foreach ($taskCompletions as $completion) {
        $date = $completion['completion_date'];
        if (isset($streakData[$date])) {
            $streakData[$date]['has_activity'] = true;
            $streakData[$date]['count'] = $completion['count'];
        }
    }
    
    // Calculate streak count
    $streakCount = 0;
    $currentStreak = 0;
    
    // Sort dates in ascending order
    ksort($streakData);
    
    // Calculate current streak
    foreach ($streakData as $date => $data) {
        if ($data['has_activity']) {
            $currentStreak++;
        } else {
            $currentStreak = 0;
        }
        
        if ($currentStreak > $streakCount) {
            $streakCount = $currentStreak;
        }
    }
    
    // Convert to indexed array for JSON response
    $result = array_values($streakData);
    
    // Log the streak data for debugging
    error_log("Streak data: " . json_encode($result));
    error_log("Streak count: $streakCount");
    
    // Return the response
    $response = [
        'status' => 'success', 
        'streak_data' => $result,
        'streak_count' => $streakCount
    ];
    
    echo json_encode($response);
    
} catch (PDOException $e) {
    error_log("PDO Error: " . $e->getMessage());
    echo json_encode(['status' => 'error', 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
