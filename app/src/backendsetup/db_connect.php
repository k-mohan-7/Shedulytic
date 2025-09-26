<?php
// Database connection
$host = 'localhost';
$dbname = 'schedlytic';
$username = 'root'; // Replace with your database username
$password = '';     // Replace with your database password

// Create connection using mysqli
$conn = new mysqli($host, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Set charset to ensure proper handling of special characters
$conn->set_charset("utf8mb4");

// Function to safely handle JSON responses
function outputJSON($data) {
    header('Content-Type: application/json');
    echo json_encode($data);
    exit;
}

// Function to handle errors consistently
function outputError($message) {
    header('Content-Type: application/json');
    echo json_encode([
        'status' => 'error',
        'message' => $message
    ]);
    exit;
}
?>
