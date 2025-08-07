# Java Compatibility Fix for Gradle Build Issues

## Problem

The build was failing with the following error:

```
Could not open init generic class cache for initialization script
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' Unsupported class file major version 65
```

## Cause

This error occurs when there's a mismatch between the Java version being used by Gradle and the Java version configured in the project:

- The error message "Unsupported class file major version 65" indicates that Java 21 is being used somewhere in the build process
- Gradle 8.1.1 (the version used in this project) is not fully compatible with Java 21
- The project is configured to use Java 17 in the build.gradle.kts file

## Solution

The following changes were made to fix the issue:

1. Updated `local.properties` to include proper JVM arguments:
   ```properties
   org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --add-opens=java.base/java.lang=ALL-UNNAMED
   ```

2. Updated `gradle.properties` to:
   - Reduce memory allocation from 4096m to 2048m
   - Explicitly set Java home to JDK 17
   ```properties
   org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 --add-opens=java.base/java.lang=ALL-UNNAMED
   org.gradle.java.home=C:\Program Files\Java\jdk-17
   ```

3. Created and ran a script to clean the problematic Gradle cache directory:
   ```batch
   rmdir /s /q "%USERPROFILE%\.gradle\caches\8.1.1\scripts\5duou9ht93a7vi5svik3kz92"
   ```

## If Issues Persist

1. Make sure JAVA_HOME environment variable is set to JDK 17
2. Run the `clean_gradle_cache.bat` script
3. Try running `gradlew clean` before building
4. If using Android Studio, try File > Invalidate Caches / Restart