<?php
// Set content type header before any output to prevent HTML in response
header("Content-Type: application/json");
header("Cache-Control: no-cache, no-store, must-revalidate");
header("Pragma: no-cache");
header("Expires: 0");

// Disable error reporting in production to prevent HTML errors in JSON
error_reporting(0);
ini_set('display_errors', 0);

// Include database connection
require_once 'db_connect.php';

function sendError($message) {
    echo json_encode([
        'status' => 'error',
        'message' => $message
    ]);
    exit;
}

// Get user_id from request with validation
$userId = isset($_GET['user_id']) ? $_GET['user_id'] : '';

if (empty($userId)) {
    sendError('User ID is required');
}

try {
    // Log the request for debugging
    error_log("User profile request for user ID: " . $userId);
    
    // Prepare statement to prevent SQL injection
    $stmt = $conn->prepare("SELECT name, email, avatar_url, username, streak_count, xp_points FROM users WHERE user_id = ?");
    if (!$stmt) {
        sendError('Database prepare error: ' . $conn->error);
    }

    $stmt->bind_param("s", $userId);
    if (!$stmt->execute()) {
        sendError('Database execution error: ' . $stmt->error);
    }

    $result = $stmt->get_result();
    if ($result->num_rows > 0) {
        $user = $result->fetch_assoc();
        
        // Ensure all fields have values to prevent null issues
        $response = [
            'status' => 'success',
            'user' => [
                'name' => $user['name'] ?? '',
                'email' => $user['email'] ?? '',
                'avatar_url' => $user['avatar_url'] ?? '',
                'username' => $user['username'] ?? '',
                'streak_count' => (int)($user['streak_count'] ?? 0),
                'xp_points' => (int)($user['xp_points'] ?? 0)
            ]
        ];
        
        // Log the response for debugging
        error_log("User profile response: " . json_encode($response));
        
        echo json_encode($response);
    } else {
        // If user not found, create a default response with empty values
        $response = [
            'status' => 'success',
            'user' => [
                'name' => 'User',
                'email' => '',
                'avatar_url' => '',
                'username' => 'user_' . $userId,
                'streak_count' => 0,
                'xp_points' => 0
            ]
        ];
        
        echo json_encode($response);
    }
} catch (Exception $e) {
    error_log("Error in get_user_profile.php: " . $e->getMessage());
    sendError('Database error: ' . $e->getMessage());
} finally {
    if (isset($stmt)) {
        $stmt->close();
    }
    if (isset($conn)) {
        $conn->close();
    }
}
?>