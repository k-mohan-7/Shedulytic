<?php
// Simple test to verify task completion system
include 'db.php';

header('Content-Type: text/html; charset=utf-8');

echo "<h2>Task Completion System Test</h2>\n";

// Test data
$test_user_id = 6; // Using existing user ID
$test_date = '2025-06-01';

try {
    // Check for existing tasks or create a test task
    $checkStmt = $pdo->prepare("SELECT id, title, status FROM tasks WHERE user_id = ? AND DATE(due_date) = ? LIMIT 1");
    $checkStmt->execute([$test_user_id, $test_date]);
    $existing_task = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$existing_task) {
        // Create a test task
        $insertStmt = $pdo->prepare("INSERT INTO tasks (user_id, task_type, title, description, due_date, status, created_at) VALUES (?, 'remainder', 'Test Task', 'Testing completion system', ?, 'pending', NOW())");
        $insertStmt->execute([$test_user_id, $test_date . ' 09:00:00']);
        $task_id = $pdo->lastInsertId();
        echo "✅ Created test task with ID: $task_id<br>\n";
    } else {
        $task_id = $existing_task['id'];
        echo "✅ Using existing task with ID: $task_id<br>\n";
    }
    
    // Test 1: Get tasks using the same query as get_today_tasks.php
    echo "<h3>Test 1: Get Today's Tasks</h3>\n";
    $stmt = $pdo->prepare("
        SELECT t.*, 
               CASE 
                   WHEN t.task_type = '' OR t.task_type IS NULL THEN 'remainder'
                   ELSE t.task_type 
               END as task_type,
               CASE 
                   WHEN tc.completion_id IS NOT NULL THEN 'completed'
                   ELSE t.status 
               END as status
        FROM tasks t
        LEFT JOIN task_completions tc ON t.id = tc.task_id 
            AND tc.user_id = :user_id 
            AND tc.completion_date = :date
        WHERE t.user_id = :user_id 
        AND DATE(t.due_date) = :date 
        AND (t.task_type = 'workflow' OR t.task_type = 'remainder' OR t.task_type = '' OR t.task_type IS NULL)
    ");
    $stmt->execute([':user_id' => $test_user_id, ':date' => $test_date]);
    $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($tasks as $task) {
        echo "Task: {$task['title']} | Status: {$task['status']}<br>\n";
    }
    
    // Test 2: Complete the task
    echo "<h3>Test 2: Complete Task</h3>\n";
    $pdo->beginTransaction();
    
    try {
        // Insert completion record
        $insertSql = "INSERT INTO task_completions (task_id, user_id, completion_date, completion_time) 
                     VALUES (:task_id, :user_id, :date, NOW())
                     ON DUPLICATE KEY UPDATE completion_time = NOW()";
        $insertStmt = $pdo->prepare($insertSql);
        $insertStmt->execute([
            ':task_id' => $task_id, 
            ':user_id' => $test_user_id, 
            ':date' => $test_date
        ]);
        
        // Update task status
        $updateSql = "UPDATE tasks SET status = 'completed', updated_at = NOW() WHERE id = :task_id AND user_id = :user_id";
        $updateStmt = $pdo->prepare($updateSql);
        $updateStmt->execute([':task_id' => $task_id, ':user_id' => $test_user_id]);
        
        $pdo->commit();
        echo "✅ Task completed successfully<br>\n";
    } catch (Exception $e) {
        $pdo->rollBack();
        echo "❌ Error: " . $e->getMessage() . "<br>\n";
    }
    
    // Test 3: Check completion status
    echo "<h3>Test 3: Verify Completion</h3>\n";
    $stmt->execute([':user_id' => $test_user_id, ':date' => $test_date]);
    $updated_tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($updated_tasks as $task) {
        if ($task['id'] == $task_id) {
            echo "Task: {$task['title']} | Status: {$task['status']}";
            echo ($task['status'] == 'completed') ? " ✅ CORRECT<br>\n" : " ❌ WRONG<br>\n";
        }
    }
    
    // Test 4: Uncomplete the task
    echo "<h3>Test 4: Uncomplete Task</h3>\n";
    $pdo->beginTransaction();
    
    try {
        // Remove completion record
        $deleteSql = "DELETE FROM task_completions WHERE task_id = :task_id AND user_id = :user_id AND completion_date = :date";
        $deleteStmt = $pdo->prepare($deleteSql);
        $deleteStmt->execute([
            ':task_id' => $task_id, 
            ':user_id' => $test_user_id, 
            ':date' => $test_date
        ]);
        
        // Update task status
        $updateSql = "UPDATE tasks SET status = 'pending', updated_at = NOW() WHERE id = :task_id AND user_id = :user_id";
        $updateStmt = $pdo->prepare($updateSql);
        $updateStmt->execute([':task_id' => $task_id, ':user_id' => $test_user_id]);
        
        $pdo->commit();
        echo "✅ Task uncompleted successfully<br>\n";
    } catch (Exception $e) {
        $pdo->rollBack();
        echo "❌ Error: " . $e->getMessage() . "<br>\n";
    }
    
    // Test 5: Final verification
    echo "<h3>Test 5: Final Verification</h3>\n";
    $stmt->execute([':user_id' => $test_user_id, ':date' => $test_date]);
    $final_tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($final_tasks as $task) {
        if ($task['id'] == $task_id) {
            echo "Task: {$task['title']} | Status: {$task['status']}";
            echo ($task['status'] == 'pending') ? " ✅ CORRECT<br>\n" : " ❌ WRONG<br>\n";
        }
    }
    
    echo "<h3>✅ All tests completed!</h3>\n";
    
} catch (PDOException $e) {
    echo "❌ Database error: " . $e->getMessage() . "<br>\n";
}
?>
