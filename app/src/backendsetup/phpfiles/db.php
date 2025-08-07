<?php
// Database connection
$host = 'localhost';
$dbname = 'schedlytic';
$username_db = 'root'; // Replace with your database username
$password_db = '';     // Replace with your database password

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username_db, $password_db);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    die("Database connection failed: " . $e->getMessage());
}
?>