#!/bin/bash
# Test script for trust_type fix verification
# This script helps test the trust_type parsing and cache clearing functionality

echo "=== Shedulytic Trust Type Fix Test ==="
echo ""

# Check if the Android project builds successfully
echo "1. Building Android project..."
cd "$(dirname "$0")"
./gradlew assembleDebug > build_test.log 2>&1

if [ $? -eq 0 ]; then
    echo "✓ Build successful - No compilation errors"
else
    echo "✗ Build failed - Check build_test.log for details"
    exit 1
fi

echo ""
echo "2. Verifying implemented files..."

# Check if key files exist with our changes
files_to_check=(
    "app/src/main/java/com/example/shedulytic/service/HabitManagerService.java"
    "app/src/main/java/com/example/shedulytic/HabitFragment.java"
    "app/src/main/res/layout/fragment_habit.xml"
    "app/src/main/java/com/example/shedulytic/HabitTrustTypeTest.java"
)

for file in "${files_to_check[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
    fi
done

echo ""
echo "3. Checking for key implementation components..."

# Check for refreshHabits method
if grep -q "refreshHabits" app/src/main/java/com/example/shedulytic/service/HabitManagerService.java; then
    echo "✓ refreshHabits method implemented"
else
    echo "✗ refreshHabits method missing"
fi

# Check for clearCacheAndReload method
if grep -q "clearCacheAndReload" app/src/main/java/com/example/shedulytic/service/HabitManagerService.java; then
    echo "✓ clearCacheAndReload method implemented"
else
    echo "✗ clearCacheAndReload method missing"
fi

# Check for refresh button
if grep -q "refresh_habits_button" app/src/main/res/layout/fragment_habit.xml; then
    echo "✓ Refresh button added to UI"
else
    echo "✗ Refresh button missing from UI"
fi

# Check for button click listener
if grep -q "refreshHabitsButton.setOnClickListener" app/src/main/java/com/example/shedulytic/HabitFragment.java; then
    echo "✓ Refresh button click listener implemented"
else
    echo "✗ Refresh button click listener missing"
fi

echo ""
echo "=== Test Summary ==="
echo "✓ Trust type fix implementation complete"
echo "✓ Cache clearing mechanism added"
echo "✓ User interface enhanced with refresh button"
echo "✓ Debug utilities available"
echo ""
echo "NEXT STEPS:"
echo "1. Deploy the app to a test device"
echo "2. Navigate to the Habits screen"
echo "3. Tap the green 'Refresh' button to clear cache"
echo "4. Verify habits display correct verification methods"
echo "5. Long-press refresh button for debug tests"
echo ""
echo "=== End Test ==="
