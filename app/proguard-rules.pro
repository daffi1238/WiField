# Room - keep entities and DAOs
-keep class com.wifield.app.data.local.entity.** { *; }
-keep class com.wifield.app.data.local.dao.** { *; }
-keep class com.wifield.app.data.local.WiFieldDatabase { *; }

# Keep domain models (used in UI and data layer)
-keep class com.wifield.app.domain.model.** { *; }

# Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Kotlin serialization / reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
