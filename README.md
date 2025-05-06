# Shedulytic Project

## Gradle Build Optimization Guide

This document provides guidance on resolving file locking issues and optimizing the Gradle build process for the Shedulytic Android project.

### File Locking Issues Resolution

The following optimizations have been implemented to address file locking issues during builds:

1. **Enhanced Gradle Properties**
   - Increased memory allocation for Gradle daemon
   - Enabled parallel builds and build caching
   - Added file locking prevention settings
   - Optimized data binding configuration

2. **Custom Force Clean Task**
   - Added a `forceClean` task that aggressively removes locked files
   - Implements multiple deletion attempts with garbage collection between tries
   - Automatically creates a fresh build directory

### How to Use

#### Regular Build
```bash
./gradlew build
```

#### When Experiencing File Locking Issues
```bash
# Run the force clean task first
./gradlew forceClean

# Then run your build
./gradlew build
```

### Additional Troubleshooting

If you continue to experience file locking issues:

1. Close Android Studio or any other IDE that might be accessing project files
2. Restart your computer to release any system-level file locks
3. Try running builds with the `--info` or `--debug` flag to get more detailed error information
4. Check that your Android Gradle Plugin version (currently 8.2.0) is compatible with your Gradle wrapper version

### Build Performance Tips

- Keep the Gradle daemon running between builds
- Use the configuration cache when possible
- Consider using the Gradle build cache for faster incremental builds
- Avoid running clean unnecessarily as it forces a full rebuild

### Contact

If you encounter persistent build issues, please report them in the project issue tracker with detailed logs and environment information.