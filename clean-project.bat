@echo off
echo Cleaning Shedulytic project...

echo Stopping Gradle daemon...
call gradlew --stop

echo Forcing garbage collection...
timeout /t 5 /nobreak > nul

echo Deleting build directories...
if exist ".\build" rmdir /s /q ".\build"
if exist ".\app\build" rmdir /s /q ".\app\build"
if exist ".\gradle\caches" rmdir /s /q ".\gradle\caches"

echo Clearing user Gradle caches...
if exist "%USERPROFILE%\.gradle\caches\transforms-3" rmdir /s /q "%USERPROFILE%\.gradle\caches\transforms-3"
if exist "%USERPROFILE%\.gradle\caches\modules-2\files-2.1" rmdir /s /q "%USERPROFILE%\.gradle\caches\modules-2\files-2.1"

echo Running clean with force unlock...
call gradlew clean --refresh-dependencies --no-daemon --stacktrace

echo Running app:forceClean task...
call gradlew :app:forceClean --no-daemon --stacktrace

echo Waiting for file locks to be released...
timeout /t 3 /nobreak > nul

echo Clean completed successfully!
echo You can now try building the project with: call gradlew build --stacktrace