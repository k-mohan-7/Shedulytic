-- Add streak column to tasks table
ALTER TABLE tasks
ADD COLUMN current_streak INT DEFAULT 0;

-- Add completion_date column to track daily completions
CREATE TABLE IF NOT EXISTS task_completions (
    task_id VARCHAR(36),
    user_id VARCHAR(36),
    completion_date DATE,
    PRIMARY KEY (task_id, user_id, completion_date),
    FOREIGN KEY (task_id) REFERENCES tasks(task_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create function to calculate current streak
DELIMITER //
CREATE FUNCTION calculate_streak(p_task_id VARCHAR(36), p_current_date DATE)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE streak INT DEFAULT 0;
    DECLARE check_date DATE;
    
    SET check_date = p_current_date;
    
    -- Check backwards from current date
    WHILE EXISTS (
        SELECT 1 FROM task_completions 
        WHERE task_id = p_task_id 
        AND completion_date = check_date
    ) DO
        SET streak = streak + 1;
        SET check_date = DATE_SUB(check_date, INTERVAL 1 DAY);
    END WHILE;
    
    RETURN streak;
END //
DELIMITER ;

-- Create index for better performance
CREATE INDEX idx_task_completions_date 
ON task_completions(task_id, completion_date);