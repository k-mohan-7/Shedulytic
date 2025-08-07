# Shedulytic App - Functional Testing Guide

## Overview
This guide provides comprehensive testing procedures for the Shedulytic habit tracking Android app to ensure all features work correctly before deployment.

## Testing Environment Setup

### Requirements
- Android device or emulator (API level 24+)
- Network connection for backend testing
- PHP server running with database setup
- APK files: `app-debug.apk` (9.79MB) and `app-release-unsigned.apk` (8.12MB)

### Backend Server Configuration
**Primary Server IP**: `192.168.53.64`
**Fallback URLs**:
- `http://192.168.53.64/schedlytic/`
- `http://10.0.2.2/schedlytic/` (for emulator)

### Installation
1. Enable "Unknown Sources" in Android settings
2. Install APK: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Grant required permissions when prompted

## Core Feature Testing

### 1. Authentication Flow Testing
**Test Case 1.1: App Launch & Splash Screen**
- [ ] App launches successfully
- [ ] Splash screen displays for 3 seconds
- [ ] Transitions to onboarding screens (MainActivity2-5)
- [ ] User streak tracking initializes

**Test Case 1.2: User Registration**
- [ ] Navigate to SignupActivity
- [ ] Fill required fields (name, username, email, password, confirm password)
- [ ] Validate email format checking
- [ ] Test password matching validation
- [ ] Successful registration saves user data
- [ ] Redirects to login or main app

**Test Case 1.3: User Login**
- [ ] Navigate to LoginActivity
- [ ] Enter valid credentials
- [ ] Successful login saves user session
- [ ] User ID and username stored in SharedPreferences
- [ ] Redirects to HomepageActivity

### 2. Main Navigation Testing
**Test Case 2.1: Tab Navigation**
- [ ] HomepageActivity loads with tab layout
- [ ] Switch between Home, Task, Habit, Profile tabs
- [ ] Each fragment loads correctly
- [ ] Tab state maintained during navigation

### 3. Home Fragment Testing
**Test Case 3.1: Dashboard Display**
- [ ] User name displays correctly
- [ ] Streak count shows current value
- [ ] Profile image loads (or shows default)
- [ ] Progress bar reflects habit completion

**Test Case 3.2: Streak Calendar**
- [ ] 7-day calendar displays current week
- [ ] Fire icons show for active days
- [ ] Calendar updates with real streak data
- [ ] Offline data fallback works

**Test Case 3.3: Today's Activities**
- [ ] MyDay section loads tasks for current date
- [ ] Separates habits and regular tasks
- [ ] Task completion toggles work
- [ ] Progress updates in real-time

### 4. Habit Management Testing
**Test Case 4.1: Habit Creation (AddHabitActivity)**
- [ ] Navigate to add habit interface
- [ ] Create checkbox verification habit
- [ ] Create location-based verification habit
- [ ] Create Pomodoro timer verification habit
- [ ] Habit saves with correct verification type

**Test Case 4.2: Habit Fragment Display**
- [ ] All habits load in list format
- [ ] Habit adapter shows verification controls
- [ ] Streak information displays correctly
- [ ] Habit colors match verification type

**Test Case 4.3: Verification Methods**

**Checkbox Verification:**
- [ ] Simple checkbox toggle works
- [ ] Completion updates streak immediately
- [ ] Backend logs activity correctly

**Location Verification:**
- [ ] GPS permission granted
- [ ] Location verification activity launches
- [ ] Current location captured
- [ ] Habit marked complete when at target location
- [ ] LocationVerificationService functions properly

**Pomodoro Verification:**
- [ ] Timer interface launches (PomodoroActivity)
- [ ] Countdown timer functions correctly
- [ ] Timer completion marks habit as done
- [ ] PomodoroService manages timer state
- [ ] Break intervals work as expected

### 5. Task Management Testing
**Test Case 5.1: Task Creation**
- [ ] AddTaskActivity interface loads
- [ ] Date picker functionality
- [ ] Time picker functionality
- [ ] Task categorization works
- [ ] Priority setting functions
- [ ] Task saves to backend

**Test Case 5.2: Task Fragment**
- [ ] Task list displays all user tasks
- [ ] Filtering and sorting work
- [ ] Task completion toggles
- [ ] Task editing functionality
- [ ] Task deletion works

### 6. Backend Integration Testing
**Test Case 6.1: Network Connectivity**
- [ ] App detects network availability
- [ ] Fallback URLs work in order
- [ ] Connection timeout handling
- [ ] Error message display

**Test Case 6.2: API Endpoints**
- [ ] `get_user_streak.php` returns streak data
- [ ] `update_user_activity.php` logs activities
- [ ] User profile data loads correctly
- [ ] Task CRUD operations work
- [ ] Habit data synchronization

**Test Case 6.3: Offline Functionality**
- [ ] Cached data displays when offline
- [ ] Local streak data fallback
- [ ] Sync when connection restored
- [ ] Error handling for network issues

### 7. Performance Testing
**Test Case 7.1: App Performance**
- [ ] Launch time under 3 seconds
- [ ] Smooth fragment transitions
- [ ] RecyclerView scrolling performance
- [ ] Memory usage acceptable
- [ ] Battery optimization

**Test Case 7.2: Data Handling**
- [ ] Large task lists render efficiently
- [ ] Image loading (Glide) performance
- [ ] Database operations speed
- [ ] Network request caching

### 8. UI/UX Testing
**Test Case 8.1: Visual Elements**
- [ ] Material Design compliance
- [ ] Color scheme consistency
- [ ] Typography readability
- [ ] Icon and image quality

**Test Case 8.2: User Experience**
- [ ] Intuitive navigation flow
- [ ] Clear feedback for actions
- [ ] Appropriate loading indicators
- [ ] Error message clarity

### 9. Device Compatibility Testing
**Test Case 9.1: Screen Sizes**
- [ ] Phone (5-7 inches)
- [ ] Tablet compatibility
- [ ] Landscape orientation
- [ ] Different resolutions

**Test Case 9.2: Android Versions**
- [ ] Minimum SDK 24 (Android 7.0)
- [ ] Target SDK 34 (Android 14)
- [ ] Permission handling on different versions

### 10. Security Testing
**Test Case 10.1: Data Protection**
- [ ] User credentials encrypted
- [ ] Session management secure
- [ ] Network traffic over HTTPS (production)
- [ ] Input validation working

**Test Case 10.2: Permissions**
- [ ] Location permission properly requested
- [ ] Network permission justified
- [ ] No unnecessary permissions

## Test Data Requirements

### Sample Users
```
User 1: test@example.com / password123
User 2: user@test.com / testpass456
```

### Sample Habits
1. **Checkbox Habit**: "Drink 8 glasses of water"
2. **Location Habit**: "Exercise at gym" (lat: 40.7128, lon: -74.0060)
3. **Pomodoro Habit**: "Study programming" (25min sessions, 3 cycles)

### Sample Tasks
1. **Daily Task**: "Review project code" - Priority: High
2. **Weekly Task**: "Grocery shopping" - Priority: Medium
3. **One-time Task**: "Doctor appointment" - Priority: High

## Bug Reporting Template

```
**Bug Title**: [Brief description]
**Severity**: [Critical/High/Medium/Low]
**Steps to Reproduce**:
1. Step 1
2. Step 2
3. Step 3

**Expected Result**: [What should happen]
**Actual Result**: [What actually happened]
**Device Info**: [Android version, device model]
**App Version**: 1.0 (Build timestamp)
**Screenshots**: [Attach if applicable]
**Logs**: [Include relevant logcat output]
```

## Testing Checklist Summary

### Pre-Testing Setup
- [ ] Backend server running and accessible
- [ ] Database tables created and populated
- [ ] Test user accounts created
- [ ] Device/emulator ready with proper Android version

### Core Testing
- [ ] Authentication (Registration, Login, Session)
- [ ] Navigation (Tabs, Fragments, Activities)
- [ ] Habit Management (Create, Verify, Track)
- [ ] Task Management (CRUD operations)
- [ ] Streak Tracking (Calendar, Progress)

### Integration Testing
- [ ] Backend API connectivity
- [ ] Offline functionality
- [ ] Data synchronization
- [ ] Real-time updates

### Performance & UX
- [ ] App performance benchmarks
- [ ] UI responsiveness
- [ ] User experience flow
- [ ] Error handling

### Final Validation
- [ ] All critical features working
- [ ] No blocking bugs found
- [ ] Performance acceptable
- [ ] Ready for deployment

## Success Criteria
- ✅ All critical test cases pass
- ✅ No major bugs detected
- ✅ Performance meets requirements
- ✅ User experience is smooth
- ✅ Backend integration stable
- ✅ Offline functionality works
- ✅ Security measures in place

## Next Steps After Testing
1. **Bug Fixes**: Address any issues found during testing
2. **Performance Optimization**: Optimize any slow operations
3. **APK Signing**: Sign release APK for production
4. **Documentation**: Update user guide and API documentation
5. **Deployment**: Prepare for Play Store or enterprise distribution
