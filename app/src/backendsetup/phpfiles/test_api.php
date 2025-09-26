<?php
// API Test - simulates Android app API calls
echo "<h2>API Test for Task Completion</h2>\n";

$base_url = "http://localhost/shedulytic/phpfiles"; // Adjust this URL as needed
$test_user_id = 6;
$test_date = "2025-06-01";

echo "<h3>1. Testing get_today_tasks.php</h3>\n";

// Test get_today_tasks API
$get_tasks_url = $base_url . "/get_today_tasks.php?user_id=" . $test_user_id . "&date=" . $test_date;
echo "URL: $get_tasks_url<br>\n";

$response = @file_get_contents($get_tasks_url);
if ($response !== false) {
    $data = json_decode($response, true);
    echo "Response: " . json_encode($data, JSON_PRETTY_PRINT) . "<br>\n";
    
    if (isset($data['tasks']) && !empty($data['tasks'])) {
        $task = $data['tasks'][0];
        $task_id = $task['id'];
        echo "Found task to test with ID: $task_id<br>\n";
        
        // Test task completion API
        echo "<h3>2. Testing task_completion.php (Complete)</h3>\n";
        
        $completion_data = json_encode([
            'task_id' => $task_id,
            'user_id' => $test_user_id,
            'date' => $test_date,
            'action' => 'complete'
        ]);
        
        $context = stream_context_create([
            'http' => [
                'method' => 'POST',
                'header' => 'Content-Type: application/json',
                'content' => $completion_data
            ]
        ]);
        
        $completion_url = $base_url . "/task_completion.php";
        $completion_response = @file_get_contents($completion_url, false, $context);
        
        if ($completion_response !== false) {
            $completion_result = json_decode($completion_response, true);
            echo "Completion Response: " . json_encode($completion_result, JSON_PRETTY_PRINT) . "<br>\n";
            
            // Test getting tasks again to see if completion status changed
            echo "<h3>3. Testing get_today_tasks.php (After Completion)</h3>\n";
            $response2 = @file_get_contents($get_tasks_url);
            if ($response2 !== false) {
                $data2 = json_decode($response2, true);
                echo "Response after completion: " . json_encode($data2, JSON_PRETTY_PRINT) . "<br>\n";
                
                // Check if our task is now marked as completed
                if (isset($data2['tasks'])) {
                    foreach ($data2['tasks'] as $updated_task) {
                        if ($updated_task['id'] == $task_id) {
                            echo "Task status after completion: " . $updated_task['status'];
                            echo ($updated_task['status'] == 'completed') ? " ✅ SUCCESS<br>\n" : " ❌ FAILED<br>\n";
                        }
                    }
                }
            }
            
            // Test uncompleting the task
            echo "<h3>4. Testing task_completion.php (Uncomplete)</h3>\n";
            
            $uncompletion_data = json_encode([
                'task_id' => $task_id,
                'user_id' => $test_user_id,
                'date' => $test_date,
                'action' => 'uncomplete'
            ]);
            
            $uncompletion_context = stream_context_create([
                'http' => [
                    'method' => 'POST',
                    'header' => 'Content-Type: application/json',
                    'content' => $uncompletion_data
                ]
            ]);
            
            $uncompletion_response = @file_get_contents($completion_url, false, $uncompletion_context);
            
            if ($uncompletion_response !== false) {
                $uncompletion_result = json_decode($uncompletion_response, true);
                echo "Uncompletion Response: " . json_encode($uncompletion_result, JSON_PRETTY_PRINT) . "<br>\n";
                
                // Final check
                echo "<h3>5. Final Status Check</h3>\n";
                $response3 = @file_get_contents($get_tasks_url);
                if ($response3 !== false) {
                    $data3 = json_decode($response3, true);
                    
                    if (isset($data3['tasks'])) {
                        foreach ($data3['tasks'] as $final_task) {
                            if ($final_task['id'] == $task_id) {
                                echo "Final task status: " . $final_task['status'];
                                echo ($final_task['status'] == 'pending') ? " ✅ SUCCESS<br>\n" : " ❌ FAILED<br>\n";
                            }
                        }
                    }
                }
            } else {
                echo "❌ Failed to call uncompletion API<br>\n";
            }
        } else {
            echo "❌ Failed to call completion API<br>\n";
        }
    } else {
        echo "❌ No tasks found for testing<br>\n";
    }
} else {
    echo "❌ Failed to call get_today_tasks API. Make sure the server is running and URLs are correct.<br>\n";
}

echo "<h3>✅ API Test completed!</h3>\n";
?>
