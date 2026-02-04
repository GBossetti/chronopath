# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== Room Database =====
# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * {
    *;
}
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * {
    *;
}

# Room type converters
-keep class * extends androidx.room.TypeConverter
-keepclassmembers class * extends androidx.room.TypeConverter {
    *;
}

# ===== Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# ===== Timber =====
-dontwarn org.jetbrains.annotations.**
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===== Kotlin Serialization (if used) =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ===== SQLCipher =====
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.* { *; }

# ===== AndroidX Security Crypto =====
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===== DataStore =====
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ===== Google Play Services Location =====
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.common.** { *; }

# ===== Application specific =====
# Keep domain models
-keep class com.chronopath.locationtracker.domain.model.** { *; }
-keep class com.chronopath.locationtracker.data.local.entity.** { *; }

# Keep data classes for serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
