# water keeps almost nothing by hand: AGP's default optimize file plus the
# consumer rules bundled inside androidx libraries (Room, Glance, WorkManager,
# DataStore) cover reflection entry points.

# Keep line numbers for readable crash reports from background receivers.
-keepattributes SourceFile,LineNumberTable

# Room schemas are validated at compile time; nothing to keep at runtime.
