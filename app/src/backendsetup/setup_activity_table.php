<?php
// Database connection details
$host = 'localhost';
$db = 'schedlytic';
$user = 'root';
$pass = '';
$charset = 'utf8mb4';

// Connect to database
try {
    $dsn = "mysql:host=$host;dbname=$db;charset=$charset";
    $options = [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ];
    $pdo = new PDO($dsn, $user, $pass, $options);
    echo "Connected to database successfully.\n";
} catch (PDOException $e) {
    die("Connection failed: " . $e->getMessage() . "\n");
}

// Step 1: Drop the table if it exists
try {
    $pdo->exec("DROP TABLE IF EXISTS `user_activity_log`");
    echo "Dropped existing user_activity_log table if it existed.\n";
} catch (PDOException $e) {
    echo "Error dropping table: " . $e->getMessage() . "\n";
}

// Step 2: Create the table without foreign key constraint
try {
    $pdo->exec("CREATE TABLE `user_activity_log` (
        `log_id` int(11) NOT NULL AUTO_INCREMENT,
        `user_id` int(11) NOT NULL,
        `activity_type` enum('login','task_completed','habit_completed') NOT NULL DEFAULT 'login',
        `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
        PRIMARY KEY (`log_id`),
        KEY `user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
    echo "Created user_activity_log table successfully.\n";
} catch (PDOException $e) {
    echo "Error creating table: " . $e->getMessage() . "\n";
    die();
}

// Step 3: Add the foreign key constraint
try {
    $pdo->exec("ALTER TABLE `user_activity_log` 
        ADD CONSTRAINT `user_activity_log_ibfk_1` 
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE");
    echo "Added foreign key constraint successfully.\n";
} catch (PDOException $e) {
    echo "Warning: Could not add foreign key constraint: " . $e->getMessage() . "\n";
    echo "This is not critical - the table will still work without the constraint.\n";
}

echo "\nSetup complete! The user_activity_log table has been created successfully.\n";
?>
