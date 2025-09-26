<?php
// Test script for task completion functionality
include 'db.php';

echo "<h1>Task Completion System Test</h1>\n";

// Test data
$test_user_id = 6; // Using existing user from your database
$test_date = '2025-06-01';

echo "<h2>Testing Task Completion Flow</h2>\n";

try {
    // 1. Get existing tasks for today
    echo "<h3>1. Getting tasks for today ($test_date):</h3>\n";
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
    
    if (empty($tasks)) {
        echo "No tasks found for today. Creating a test task...<br>\n";
        
        // Create a test task for today
        $insertStmt = $pdo->prepare("
            INSERT INTO tasks (user_id, task_type, title, description, due_date, status, created_at) 
            VALUES (:user_id, 'remainder', 'Test Task for Completion', 'Testing task completion system', :due_date, 'pending', NOW())
        ");
        $insertStmt->execute([
            ':user_id' => $test_user_id,
            ':due_date' => $test_date . ' 00:00:00'
        ]);
        
        $test_task_id = $pdo->lastInsertId();
        echo "Created test task with ID: $test_task_id<br>\n";
        
        // Re-fetch tasks
        $stmt->execute([':user_id' => $test_user_id, ':date' => $test_date]);
        $tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    foreach ($tasks as $task) {
        echo "Task ID: {$task['id']}, Title: {$task['title']}, Status: {$task['status']}<br>\n";
    }
    
    // 2. Test completing a task
    if (!empty($tasks)) {
        $first_task = $tasks[0];
        $task_id = $first_task['id'];
        
        echo "<h3>2. Testing task completion (Task ID: $task_id):</h3>\n";
        
        // Simulate completion
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
            echo "✅ Task marked as completed successfully<br>\n";
        } catch (Exception $e) {
            $pdo->rollBack();
            echo "❌ Error completing task: " . $e->getMessage() . "<br>\n";
        }
        
        // 3. Verify completion
        echo "<h3>3. Verifying completion status:</h3>\n";
        $stmt->execute([':user_id' => $test_user_id, ':date' => $test_date]);
        $updated_tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        foreach ($updated_tasks as $task) {
            if ($task['id'] == $task_id) {
                echo "Task ID: {$task['id']}, Status: {$task['status']} ";
                if ($task['status'] == 'completed') {
                    echo "✅ Correctly marked as completed<br>\n";
                } else {
                    echo "❌ Status not updated correctly<br>\n";
                }
            }
        }
        
        // 4. Test uncompleting the task
        echo "<h3>4. Testing task uncompletion:</h3>\n";
        
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
            echo "✅ Task marked as pending successfully<br>\n";
        } catch (Exception $e) {
            $pdo->rollBack();
            echo "❌ Error uncompleting task: " . $e->getMessage() . "<br>\n";
        }
        
        // 5. Final verification
        echo "<h3>5. Final verification:</h3>\n";
        $stmt->execute([':user_id' => $test_user_id, ':date' => $test_date]);
        $final_tasks = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        foreach ($final_tasks as $task) {
            if ($task['id'] == $task_id) {
                echo "Task ID: {$task['id']}, Status: {$task['status']} ";
                if ($task['status'] == 'pending') {
                    echo "✅ Correctly reverted to pending<br>\n";
                } else {
                    echo "❌ Status not reverted correctly<br>\n";
                }
            }
        }
    }

} catch (PDOException $e) {
    echo "❌ Database error: " . $e->getMessage() . "<br>\n";
}

echo "<h2>✅ Test completed!</h2>\n";
?>
