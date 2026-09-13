# R8 runs in full mode with optimization and obfuscation on (see DECISIONS.md ->
# "Build & release"). Compose, Koin, kotlinx.serialization, Room and the Firebase AI
# SDK all ship consumer rules, so this file holds only what those do not cover.

# Release stack traces are worth line numbers. Without the first line a crash report
# names the class and nothing else; without the second the original file name leaks
# back into the trace and undoes half the obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
