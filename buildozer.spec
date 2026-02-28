[app]

# (str) Title of your application
title = HELA SMART SACCO

# (str) Package name (no spaces, all lowercase)
package.name = helasmartsacco

# (str) Package domain (needed for android/ios packaging)
package.domain = org.test

# (str) Source code where the main.py lives
source.dir = .

# (list) Source files to include (let empty to include all the files)
source.include_exts = py,png,jpg,kv,atlas

# (str) Main script file
main.py

# (str) Application version
version = 0.1

# (list) Application requirements
requirements = python3,kivy

# (str) Icon of the application
icon.filename = %(source.dir)s/myicon.png

# (list) Supported orientations
orientation = portrait

# (bool) Indicate if the application should be fullscreen or not
fullscreen = 1  # change to 1 for fullscreen, 0 to show status bar

# (list) The Android archs to build for
android.archs = arm64-v8a, armeabi-v7a

# (bool) Enables Android auto backup feature (Android API >=23)
android.allow_backup = True

[buildozer]

# (int) Log level (0 = error only, 1 = info, 2 = debug)
log_level = 2

# (int) Display warning if buildozer is run as root
warn_on_root = 1