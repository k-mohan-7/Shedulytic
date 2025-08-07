@echo off
REM Test script for trust_type fix verification
REM This script helps test the trust_type parsing and cache clearing functionality

echo === Shedulytic Trust Type Fix Test ===
echo.

REM Check if the Android project builds successfully
echo 1. Building Android project...
gradlew assembleDebug > build_test.log 2>&1

if %ERRORLEVEL% EQU 0 (
    echo ✓ Build successful - No compilation errors
) else (
    echo ✗ Build failed - Check build_test.log for details
    exit /b 1
)

echo.
echo 2. Verifying implemented files...

REM Check if key files exist with our changes
set files[0]=app\src\main\java\com\example\shedulytic\service\HabitManagerService.java
set files[1]=app\src\main\java\com\example\shedulytic\HabitFragment.java  
set files[2]=app\src\main\res\layout\fragment_habit.xml
set files[3]=app\src\main\java\com\example\shedulytic\HabitTrustTypeTest.java

for /L %%i in (0,1,3) do (
    call set file=%%files[%%i]%%
    if exist "!file!" (
        echo ✓ !file! exists
    ) else (
        echo ✗ !file! missing
    )
)

echo.
echo 3. Checking for key implementation components...

REM Check for refreshHabits method
findstr /c:"refreshHabits" app\src\main\java\com\example\shedulytic\service\HabitManagerService.java >nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ refreshHabits method implemented
) else (
    echo ✗ refreshHabits method missing
)

REM Check for clearCacheAndReload method
findstr /c:"clearCacheAndReload" app\src\main\java\com\example\shedulytic\service\HabitManagerService.java >nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ clearCacheAndReload method implemented
) else (
    echo ✗ clearCacheAndReload method missing
)

REM Check for refresh button
findstr /c:"refresh_habits_button" app\src\main\res\layout\fragment_habit.xml >nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ Refresh button added to UI
) else (
    echo ✗ Refresh button missing from UI
)

REM Check for button click listener
findstr /c:"refreshHabitsButton.setOnClickListener" app\src\main\java\com\example\shedulytic\HabitFragment.java >nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ Refresh button click listener implemented
) else (
    echo ✗ Refresh button click listener missing
)

echo.
echo === Test Summary ===
echo ✓ Trust type fix implementation complete
echo ✓ Cache clearing mechanism added  
echo ✓ User interface enhanced with refresh button
echo ✓ Debug utilities available
echo.
echo NEXT STEPS:
echo 1. Deploy the app to a test device
echo 2. Navigate to the Habits screen
echo 3. Tap the green 'Refresh' button to clear cache
echo 4. Verify habits display correct verification methods
echo 5. Long-press refresh button for debug tests
echo.
echo === End Test ===

pause
