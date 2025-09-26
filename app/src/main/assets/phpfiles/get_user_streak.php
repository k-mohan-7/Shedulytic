<?php
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

// Include database connection
require_once '../db_connect.php';

// Get and validate parameters
$userId = isset($_GET['user_id']) ? $_GET['user_id'] : '';
$startDate = isset($_GET['start_date']) ? $_GET['start_date'] : date('Y-m-d', strtotime('-30 days'));
$endDate = isset($_GET['end_date']) ? $_GET['end_date'] : date('Y-m-d');

if (empty($userId)) {
    echo json_encode([
        'status' => 'error',
        'message' => 'User ID is required'
    ]);
    exit;
}

try {
    // Log request for debugging
    error_log("Streak data request for user: $userId, from $startDate to $endDate");
    
    // Query to get task completions for the date range
    $query = "
        SELECT DATE(due_date) as date, COUNT(*) as count 
        FROM tasks 
        WHERE user_id = ? 
        AND status = 'completed' 
        AND DATE(due_date) BETWEEN ? AND ? 
        GROUP BY DATE(due_date)
    ";
    
    $stmt = $conn->prepare($query);
    if (!$stmt) {
        throw new Exception("Failed to prepare statement: " . $conn->error);
    }
    
    $stmt->bind_param("sss", $userId, $startDate, $endDate);
    if (!$stmt->execute()) {
        throw new Exception("Failed to execute query: " . $stmt->error);
    }
    
    $result = $stmt->get_result();
    $completions = array();
    
    while ($row = $result->fetch_assoc()) {
        $completions[$row['date']] = $row['count'];
    }
    
    // Create a structured array for all days in the range
    $streakData = array();
    $currentDate = new DateTime($startDate);
    $endDateTime = new DateTime($endDate);
    $endDateTime->modify('+1 day'); // Include end date
    
    while ($currentDate < $endDateTime) {
        $dateStr = $currentDate->format('Y-m-d');
        $hasActivity = isset($completions[$dateStr]) && $completions[$dateStr] > 0;
        
        $streakData[] = array(
            'date' => $dateStr,
            'has_activity' => $hasActivity,
            'count' => isset($completions[$dateStr]) ? (int)$completions[$dateStr] : 0
        );
        
        $currentDate->modify('+1 day');
    }
    
    // Calculate streak count
    $streakCount = 0;
    $currentStreak = 0;
    
    // Calculate from most recent day backwards
    for ($i = count($streakData) - 1; $i >= 0; $i--) {
        if ($streakData[$i]['has_activity']) {
            $currentStreak++;
        } else {
            break; // Break on first day without activity
        }
    }
    
    $streakCount = $currentStreak;
    
    // Return the response
    echo json_encode([
        'status' => 'success',
        'streak_data' => $streakData,
        'streak_count' => $streakCount,
        'message' => 'Streak data loaded successfully'
    ]);
    
} catch (Exception $e) {
    error_log("Error in get_user_streak.php: " . $e->getMessage());
    echo json_encode([
        'status' => 'error',
        'message' => $e->getMessage()
    ]);
} finally {
    // Close statement and connection
    if (isset($stmt)) {
        $stmt->close();
    }
    if (isset($conn)) {
        $conn->close();
    }
}
?>