# Shedulytic - Smart Task and Habit Management App

Shedulytic is a comprehensive Android application designed to help users manage their daily tasks, build healthy habits, and track their progress. The app features an intuitive interface, advanced notification system, location-based verification, Pomodoro timer integration, and comprehensive streak tracking.

## 🚀 Features

### Core Functionality
- **Task Management**: Create, organize, and track workflow and reminder tasks
- **Habit Tracking**: Build and maintain healthy habits with smart verification systems
- **Streak Tracking**: Visual streak tracking with fire icons and statistics
- **Calendar Integration**: View your daily activities and progress on an interactive calendar
- **Progress Analytics**: Track completion rates and analyze your productivity patterns

### Advanced Features
- **Location Verification**: Verify habit completion based on GPS location
- **Pomodoro Timer**: Built-in Pomodoro technique support for focused work sessions
- **Smart Notifications**: Contextual notifications for tasks, habits, and reminders
- **Pull-to-Refresh**: Optimized scrolling and data synchronization
- **Offline Support**: Local data storage with server synchronization
- **Multi-verification System**: Checkbox, location, and Pomodoro-based habit verification

### User Interface
- **Modern Material Design**: Clean, intuitive interface following Material Design principles
- **Fragment-based Navigation**: Smooth navigation between Home, Tasks, Habits, Streak, and Profile sections
- **Responsive Design**: Optimized for various screen sizes and orientations
- **Real-time Updates**: Live updates across all fragments when data changes

## 📱 Application Structure

### Main Components

#### 1. **HomeFragment**
- Central dashboard showing today's activities
- Quick access to task and habit creation
- Real-time progress tracking
- Integrated timeline view
![home](https://github.com/k-mohan-7/Shedulytic/blob/main/Home_screen.jpg)


#### 2. **TaskFragment** 
- Comprehensive task management interface
- Support for workflow and reminder task types
- Task completion tracking with server synchronization
- Advanced filtering and sorting options
(https://github.com/k-mohan-7/Shedulytic/blob/main/Task_Management.jpg)
#### 3. **HabitFragment**
- Dedicated habit management interface
- Multiple verification methods (checkbox, location, Pomodoro)
- Daily habit scheduling and frequency management
- Progress visualization and streak tracking
![Habits](https://github.com/k-mohan-7/Shedulytic/blob/main/Habits_Management.jpg)
#### 4. **StreakFragment**
- Visual streak calendar with fire icons
- Detailed progress analytics
- Historical data visualization
- Achievement tracking

#### 5. **ProfileFragment**
- User profile management
- Settings and preferences
- Account information
- App configuration options
![profile](https://github.com/k-mohan-7/Shedulytic/blob/main/Profile_Management.jpg)
### Key Activities

#### **AddTaskActivity / AddHabitActivity**
- Comprehensive forms for creating tasks and habits
- Support for various task types and verification methods
- Location selection for location-based habits
- Scheduling and frequency configuration

#### **PomodoroActivity**
- Full-screen Pomodoro timer interface
- Session tracking and statistics
- Integration with habit verification system
- Customizable timer durations

#### **LocationVerificationActivity**
- GPS-based location verification
- Map integration for location selection
- Distance-based verification thresholds
- Real-time location tracking

#### **NotificationSettingsActivity**
- Comprehensive notification management
- Customizable notification preferences
- Debug and testing tools
- Notification channel configuration

## 🛠️ Technical Implementation

### Architecture

#### **MVVM Pattern**
- Model-View-ViewModel architecture
- Clear separation of concerns
- Reactive data binding
- Lifecycle-aware components

#### **Service-Oriented Architecture**
- **HabitManagerService**: Habit lifecycle management
- **LocationVerificationService**: GPS and location services
- **PomodoroService**: Timer management and notifications
- **NotificationHandler**: Centralized notification management

#### **Manager Classes**
- **TaskManager**: Task CRUD operations and synchronization
- **ProfileManager**: User profile and authentication
- **TodayActivitiesManager**: Daily activity aggregation
- **HabitManager**: Habit logic and verification
- **VolleyNetworkManager**: Network request management

### Data Management

#### **Local Storage**
- SQLite database via `DatabaseHelper`
- SharedPreferences for user settings
- Local caching for offline functionality
- Data persistence across app sessions

#### **Server Synchronization**
- RESTful API integration
- Real-time data synchronization
- Conflict resolution
- Network error handling

### Notification System

#### **Smart Notifications**
- Context-aware notification scheduling
- Multiple notification types (reminders, completions, streaks)
- Interactive notification actions
- Background processing support

#### **Notification Types**
- **Task Reminders**: Scheduled task notifications
- **Habit Reminders**: Daily habit prompts
- **Streak Notifications**: Achievement and milestone alerts
- **Workflow Notifications**: Work session start/end alerts

### Background Services

#### **Boot Receiver**
- Automatic notification rescheduling after device restart
- Service restoration after system events
- Background task management

#### **Location Services**
- Background location tracking for habit verification
- Geofencing support
- Battery-optimized location updates

#### **Timer Services**
- Pomodoro timer background execution
- Session persistence across app lifecycle
- Notification integration

## 🌐 Backend Setup

### Database Schema

The application uses a MySQL database with the following key tables:

#### **Users Table**
```sql
- user_id (Primary Key)
- username
- email
- password_hash
- created_at
- updated_at
```

#### **Tasks Table**
```sql
- task_id (Primary Key)
- user_id (Foreign Key)
- title
- description
- type (workflow/remainder)
- status (pending/completed)
- due_date
- created_at
```

#### **Habits Table**
```sql
- habit_id (Primary Key)
- user_id (Foreign Key)
- title
- description
- verification_type (checkbox/location/pomodoro)
- frequency
- location_lat
- location_lng
- pomodoro_length
- created_at
```

#### **Activity Tracking**
```sql
- activity_id (Primary Key)
- user_id (Foreign Key)
- activity_type
- activity_date
- completion_status
- streak_count
```

### Backend PHP Files

#### **Core API Endpoints**

1. **Authentication**
   - `login.php` - User authentication
   - `signup.php` - User registration
   - `get_user_profile.php` - Profile information

2. **Task Management**
   - `add_task.php` - Create new tasks
   - `update_task.php` - Update existing tasks
   - `delete_task.php` - Remove tasks
   - `get_today_tasks.php` - Fetch daily tasks
   - `update_task_status.php` - Update completion status

3. **Habit Management**
   - `add_habit.php` - Create new habits
   - `get_habits.php` - Retrieve user habits
   - `add_completion.php` - Record habit completions
   - `habit_streaks.php` - Calculate streak data

4. **Analytics and Tracking**
   - `get_user_streak.php` - Fetch streak statistics
   - `log_user_activity.php` - Record user activities
   - `task_completion.php` - Completion analytics

5. **Utility**
   - `ping.php` - Server connectivity test
   - `db_connect.php` - Database connection management
   - `test_api.php` - API endpoint testing

#### **Database Migration Files**
- `schedlytic22_05_25.sql` - Initial database schema
- `V4__add_habit_streaks.sql` - Streak tracking enhancements
- `create_activity_table.php` - Activity tracking setup

#### **Testing and Validation**
- `test_add_task.php` - Task creation testing
- `test_completion.php` - Completion workflow testing
- `test_task_completion.php` - Task completion validation
- `test_streak.php` - Streak calculation testing

### Server Configuration

#### **Network Security**
- HTTPS/HTTP support via `network_security_config.xml`
- Certificate pinning for enhanced security
- Encrypted data transmission
- Secure API endpoint management

#### **Database Connection**
- Connection pooling for optimal performance
- Error handling and recovery
- Transaction management
- Query optimization

## 🔧 Development Setup

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK API 21+ (minimum) / API 33+ (target)
- MySQL/MariaDB server
- PHP 7.4+ with required extensions

### Local Development

#### **Android App Setup**
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Configure local properties for server endpoints
5. Run on emulator or physical device

#### **Backend Setup**
1. Set up LAMP/WAMP/XAMPP stack
2. Import database schema from `schedlytic22_05_25.sql`
3. Configure database connection in `db_connect.php`
4. Deploy PHP files to web server
5. Update app configuration with server URLs

### Build Configuration

#### **Gradle Configuration**
- Multi-module project structure
- Optimized build performance
- Custom force clean tasks for development
- Proguard rules for release builds

#### **Build Variants**
- Debug build with verbose logging
- Release build with optimization
- Testing build with mock data

### Testing

#### **Unit Testing**
- JUnit tests for core functionality
- Mockito for dependency injection
- Local unit tests for business logic

#### **Integration Testing**
- Instrumented tests for UI components
- Network testing with mock servers
- Database testing with test data

#### **Manual Testing**
- Comprehensive testing guides in documentation
- Test scenarios for all major features
- Performance testing procedures

## 📊 Performance Optimizations

### UI Optimization
- RecyclerView optimizations with view recycling
- Lazy loading for large datasets
- Efficient layout hierarchies
- Scroll optimizations for nested views

### Network Optimization
- Request caching and batching
- Background synchronization
- Retry mechanisms with exponential backoff
- Compressed data transfers

### Battery Optimization
- Doze mode compatibility
- Background execution limits
- Efficient location services usage
- Smart notification scheduling

### Memory Management
- Proper activity/fragment lifecycle handling
- Bitmap memory optimization
- Leak prevention strategies
- Garbage collection optimizations

## 🔐 Security Features

### Data Protection
- Local data encryption
- Secure API communication
- User authentication tokens
- Privacy-compliant data handling

### Permission Management
- Runtime permission requests
- Minimal permission usage
- User consent for sensitive features
- Privacy policy compliance

## 📋 Deployment Guide

### Release Preparation
1. Update version codes and names
2. Configure release signing
3. Run final testing suite
4. Generate release APK/AAB
5. Prepare store listing materials

### Server Deployment
1. Set up production database
2. Configure SSL certificates
3. Deploy PHP backend files
4. Set up monitoring and logging
5. Configure backup systems

### Distribution
- Google Play Store deployment
- Internal testing distribution
- Beta testing programs
- Update management

## 🤝 Contributing

### Development Guidelines
- Follow Android development best practices
- Maintain code documentation
- Write comprehensive tests
- Use consistent coding style

### Issue Reporting
- Use GitHub Issues for bug reports
- Provide detailed reproduction steps
- Include device and version information
- Attach relevant logs and screenshots

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For support, bug reports, or feature requests:
- Create an issue on GitHub
- Contact the development team
- Check the documentation wiki
- Join the community discussions

---

**Shedulytic** - Making productivity simple, one task at a time. 🚀
