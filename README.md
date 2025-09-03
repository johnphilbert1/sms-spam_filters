# SMS Spam Filter App

A comprehensive Android application for filtering and managing SMS spam messages using both keyword-based and Bayesian filtering approaches.

## Features

### Core Features
- **SMS Spam Detection**
  - Keyword-based filtering
  - Bayesian spam filter (offline)
  - Real-time message processing
  - Automatic spam categorization

### User Interface
- **Tabbed Interface**
  - Inbox tab for regular messages
  - Spam tab for filtered messages
  - Search functionality in both tabs
  - Swipe-to-delete support

### Message Management
- **Message Actions**
  - Delete messages
  - Restore messages from spam
  - Mark messages as spam/not spam
  - View message details

### Advanced Features
- **Bayesian Filter Training**
  - User-driven training system
  - Mark messages as spam/not spam
  - Automatic learning from user actions
  - Persistent training data

- **Automatic Cleanup**
  - WorkManager-based cleanup system
  - Configurable retention period (default: 30 days)
  - Automatic deletion of old spam messages
  - Daily cleanup schedule

- **Push Notifications**
  - Real-time spam detection alerts
  - Clickable notifications
  - Message preview in notifications
  - Android 13+ notification support

## Technical Implementation

### Architecture
- **MVVM Architecture**
  - ViewModel for UI state management
  - Repository pattern for data operations
  - LiveData/Flow for reactive updates
  - Coroutines for asynchronous operations

### Data Management
- **Room Database**
  - Message storage
  - Spam keyword management
  - Word frequency tracking
  - Efficient querying and updates

### Spam Detection
- **Bayesian Filter**
  - Naive Bayes implementation
  - Word frequency analysis
  - Probability calculations
  - Laplace smoothing for unknown words

### Background Processing
- **WorkManager**
  - Periodic cleanup tasks
  - Battery-efficient scheduling
  - Reliable task execution
  - System constraints handling

### Notifications
- **Notification System**
  - Custom notification channel
  - Rich notification content
  - Deep linking to app
  - Permission handling

## Setup and Usage

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+
- Kotlin 1.8.0+

### Installation
1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the application

### Required Permissions
- SMS receiving
- SMS reading
- SMS sending
- Notifications (Android 13+)

### Configuration
- Default spam retention period: 30 days
- Adjustable through preferences
- Customizable notification settings
- Configurable cleanup schedule

## Contributing
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments
- Android Jetpack libraries
- Room persistence library
- WorkManager
- Material Design components 