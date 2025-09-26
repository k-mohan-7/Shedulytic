<?php
// Simple ping endpoint to test connectivity
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

// Return a simple success response
echo json_encode([
    'status' => 'success', 
    'message' => 'Server is running', 
    'timestamp' => date('Y-m-d H:i:s')
]);
?>
