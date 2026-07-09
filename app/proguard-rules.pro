# Project-specific ProGuard/R8 rules.

# Room compiler rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.RoomDatabase

# Apache POI rules for Excel import/export
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.schemas.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class com.microsoft.schemas.** { *; }
-dontwarn com.microsoft.schemas.**

# Keep original line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Ignore missing AWT classes referenced by Apache POI / graph libraries
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**

# Ignore missing OSGi framework classes referenced by Log4j / Apache POI
-dontwarn org.osgi.framework.**
-dontwarn org.apache.logging.log4j.**

# Ignore missing graphbuilder classes
-dontwarn com.graphbuilder.**
