<?php
include "db.php";

// Handle POST request
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Get raw POST data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);

    // Extract input data
    $name = trim($data['name'] ?? '');
    $username = trim($data['username'] ?? '');
    $email = trim($data['email'] ?? '');
    $password = $data['password'] ?? '';
    $confirm_password = $data['confirm_password'] ?? '';

    // Validate inputs
    if (empty($name) || empty($username) || empty($email) || empty($password) || empty($confirm_password)) {
        echo json_encode(['status' => 'error', 'message' => 'All fields are required.']);
        exit;
    }

    if ($password !== $confirm_password) {
        echo json_encode(['status' => 'error', 'message' => 'Passwords do not match.']);
        exit;
    }

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        echo json_encode(['status' => 'error', 'message' => 'Invalid email format.']);
        exit;
    }

    // Check if username or email already exists
    try {
        $stmt = $pdo->prepare("SELECT user_id FROM users WHERE username = :username OR email = :email");
        $stmt->bindParam(':username', $username);
        $stmt->bindParam(':email', $email);
        $stmt->execute();
        $existingUser = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($existingUser) {
            if ($existingUser['username'] === $username) {
                echo json_encode(['status' => 'error', 'message' => 'This username is already taken.']);
            } else {
                echo json_encode(['status' => 'error', 'message' => 'This email is already registered.']);
            }
            exit;
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error checking username/email: ' . $e->getMessage()]);
        exit;
    }

    // Hash the password
    $hashed_password = password_hash($password, PASSWORD_BCRYPT);

    // Insert into database
    try {
        $stmt = $pdo->prepare("INSERT INTO users (name, username, email, password) VALUES (:name, :username, :email, :password)");
        $stmt->bindParam(':name', $name);
        $stmt->bindParam(':username', $username);
        $stmt->bindParam(':email', $email);
        $stmt->bindParam(':password', $hashed_password);

        if ($stmt->execute()) {
            echo json_encode(['status' => 'success', 'message' => 'User registered successfully.']);
        } else {
            echo json_encode(['status' => 'error', 'message' => 'Registration failed. Please try again.']);
        }
    } catch (PDOException $e) {
        echo json_encode(['status' => 'error', 'message' => 'Error: ' . $e->getMessage()]);
    }
}
?>