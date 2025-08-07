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
include 'db.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    // Get query parameters
    $user_id = $_GET['user_id'] ?? null;

    // Validate input
    if (empty($user_id)) {
        echo json_encode(['status' => 'error', 'message' => 'User ID is required.']);
        exit;
    }

    // Fetch user profile data
    try {
        $stmt = $pdo->prepare("SELECT name, email, avatar_url, username, streak_count, xp_points FROM users WHERE user_id = :user_id");
        $stmt->bindParam(':user_id', $user_id);
        $stmt->execute();
        $user = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($user) {
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
            
            echo json_encode($response);
        } else {
            // If user not found, create a default response with empty values
            $response = [
                'status' => 'success',
                'user' => [
                    'name' => 'User',
                    'email' => '',
                    'avatar_url' => '',
                    'username' => 'user_' . $user_id,
                    'streak_count' => 0,
                    'xp_points' => 0
                ]
            ];
            
            echo json_encode($response);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
} else {
    echo json_encode(['status' => 'error', 'message' => 'Invalid request method']);
}
?>