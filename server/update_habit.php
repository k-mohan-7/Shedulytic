<?php
/**
 * Update Habit Completion Status
 * 
 * This script handles:
 * - Habit completion status update
 * - XP reward (+1.0) for habit completion
 * - Streak calculation and update
 * - Logging habit completion
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Handle preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Database connection
require_once 'db_connect.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['status' => 'error', 'message' => 'Only POST method allowed']);
    exit;
}

// Get JSON input
$input = file_get_contents('php://input');
$data = json_decode($input, true);

// Validate required fields
$habit_id = $data['habit_id'] ?? null;
$user_id = $data['user_id'] ?? null;
$completed = isset($data['completed']) ? (int)$data['completed'] : null;
$date = $data['date'] ?? date('Y-m-d');
$current_streak = $data['current_streak'] ?? 0;
$total_completions = $data['total_completions'] ?? 0;
$verification_type = $data['verification_type'] ?? 'checkbox';

if (empty($habit_id)) {
    echo json_encode(['status' => 'error', 'message' => 'Habit ID is required']);
    exit;
}

if (empty($user_id)) {
    echo json_encode(['status' => 'error', 'message' => 'User ID is required']);
    exit;
}

if ($completed === null) {
    echo json_encode(['status' => 'error', 'message' => 'Completed status is required']);
    exit;
}

// XP rewards based on verification type
$xp_rewards = [
    'checkbox' => 1.0,
    'location' => 1.5,
    'pomodoro' => 2.0
];

$xp_change = $completed ? ($xp_rewards[$verification_type] ?? 1.0) : 0;

try {
    // Start transaction
    $pdo->beginTransaction();
    
    // 1. Check if completion already exists for today
    $check_sql = "SELECT completion_id FROM completions WHERE habit_id = :habit_id AND date = :date";
    $check_stmt = $pdo->prepare($check_sql);
    $check_stmt->execute([':habit_id' => $habit_id, ':date' => $date]);
    $existing = $check_stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        // Update existing completion
        $update_completion_sql = "UPDATE completions SET 
                                  progress = :progress, 
                                  is_trusted = :is_trusted 
                                  WHERE completion_id = :completion_id";
        $update_stmt = $pdo->prepare($update_completion_sql);
        $update_stmt->execute([
            ':progress' => $completed ? 'completed' : 'not_completed',
            ':is_trusted' => $verification_type === 'checkbox' ? 0 : 1,
            ':completion_id' => $existing['completion_id']
        ]);
    } else {
        // Insert new completion
        $insert_sql = "INSERT INTO completions (habit_id, date, progress, is_trusted) 
                       VALUES (:habit_id, :date, :progress, :is_trusted)";
        $insert_stmt = $pdo->prepare($insert_sql);
        $insert_stmt->execute([
            ':habit_id' => $habit_id,
            ':date' => $date,
            ':progress' => $completed ? 'completed' : 'not_completed',
            ':is_trusted' => $verification_type === 'checkbox' ? 0 : 1
        ]);
    }
    
    // 2. Update habit streak in habits table
    $update_habit_sql = "UPDATE habits SET 
                         current_streak = :current_streak,
                         total_completions = :total_completions,
                         last_completed_date = :last_completed_date
                         WHERE habit_id = :habit_id";
    $update_habit_stmt = $pdo->prepare($update_habit_sql);
    $update_habit_stmt->execute([
        ':current_streak' => $current_streak,
        ':total_completions' => $total_completions,
        ':last_completed_date' => $completed ? $date : null,
        ':habit_id' => $habit_id
    ]);
    
    // 3. Update user XP points if completing
    $new_xp = 0;
    if ($completed && $xp_change > 0) {
        // Get current XP
        $get_xp_sql = "SELECT xp_points FROM users WHERE user_id = :user_id";
        $xp_stmt = $pdo->prepare($get_xp_sql);
        $xp_stmt->execute([':user_id' => $user_id]);
        $user_row = $xp_stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($user_row) {
            $current_xp = floatval($user_row['xp_points'] ?? 0);
            $new_xp = $current_xp + floatval($xp_change);
            
            // Update XP
            $update_xp_sql = "UPDATE users SET xp_points = :xp_points WHERE user_id = :user_id";
            $update_xp_stmt = $pdo->prepare($update_xp_sql);
            $update_xp_stmt->execute([
                ':xp_points' => $new_xp,
                ':user_id' => $user_id
            ]);
        }
    }
    
    // 4. Log activity for streak tracking
    if ($completed) {
        // Create activity log table if not exists
        $create_log_table = "CREATE TABLE IF NOT EXISTS user_activity_log (
            log_id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            activity_type VARCHAR(50) NOT NULL,
            activity_data TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_user_id (user_id),
            INDEX idx_activity_type (activity_type),
            INDEX idx_created_at (created_at)
        )";
        $pdo->exec($create_log_table);
        
        $activity_sql = "INSERT INTO user_activity_log (user_id, activity_type, activity_data, created_at) 
                         VALUES (:user_id, 'habit_completed', :activity_data, NOW())";
        $activity_stmt = $pdo->prepare($activity_sql);
        $activity_stmt->execute([
            ':user_id' => $user_id,
            ':activity_data' => json_encode([
                'habit_id' => $habit_id,
                'date' => $date,
                'verification_type' => $verification_type,
                'xp_earned' => $xp_change
            ])
        ]);
    }
    
    // Commit transaction
    $pdo->commit();
    
    // Calculate new streak
    $new_streak = calculateStreak($pdo, $habit_id);
    
    echo json_encode([
        'status' => 'success',
        'message' => $completed ? 'Habit marked as completed' : 'Habit marked as incomplete',
        'data' => [
            'habit_id' => $habit_id,
            'completed' => $completed,
            'streak' => $new_streak,
            'xp_earned' => $xp_change,
            'total_xp' => $new_xp,
            'date' => $date
        ]
    ]);
    
} catch (Exception $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode([
        'status' => 'error',
        'message' => 'Error updating habit: ' . $e->getMessage()
    ]);
}

/**
 * Calculate habit streak based on completion history
 */
function calculateStreak($pdo, $habit_id) {
    try {
        // Get completions ordered by date descending
        $sql = "SELECT date, progress FROM completions 
                WHERE habit_id = :habit_id AND progress = 'completed'
                ORDER BY date DESC 
                LIMIT 365";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([':habit_id' => $habit_id]);
        $completions = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        if (empty($completions)) {
            return 0;
        }
        
        $streak = 0;
        $today = date('Y-m-d');
        $yesterday = date('Y-m-d', strtotime('-1 day'));
        
        // Check if there's a completion for today or yesterday
        $lastDate = $completions[0]['date'];
        if ($lastDate !== $today && $lastDate !== $yesterday) {
            return 0; // Streak broken
        }
        
        // Count consecutive days
        $expectedDate = $lastDate;
        foreach ($completions as $completion) {
            if ($completion['date'] === $expectedDate) {
                $streak++;
                $expectedDate = date('Y-m-d', strtotime($expectedDate . ' -1 day'));
            } else {
                break; // Streak broken
            }
        }
        
        return $streak;
    } catch (Exception $e) {
        return 0;
    }
}
?>
