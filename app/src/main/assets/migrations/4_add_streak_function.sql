CREATE FUNCTION calculate_streak(p_task_id VARCHAR(36), p_current_date DATE)
RETURNS INTEGER
BEGIN
    DECLARE v_streak INT DEFAULT 0;
    DECLARE v_last_date DATE;
    
    -- Get the most recent completion date before current date
    SELECT MAX(completion_date)
    INTO v_last_date
    FROM task_completions
    WHERE task_id = p_task_id
    AND completion_date <= p_current_date;
    
    -- If no completion found, return 0
    IF v_last_date IS NULL THEN
        RETURN 0;
    END IF;
    
    -- Calculate streak by checking consecutive days backwards
    SET v_streak = 1;
    
    WHILE EXISTS (
        SELECT 1 
        FROM task_completions
        WHERE task_id = p_task_id
        AND completion_date = DATE_SUB(v_last_date, INTERVAL v_streak DAY)
    ) DO
        SET v_streak = v_streak + 1;
    END WHILE;
    
    RETURN v_streak;
END;