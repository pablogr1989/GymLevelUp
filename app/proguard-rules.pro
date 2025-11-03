# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep data classes
-keep class com.gymlog.app.data.local.entity.** { *; }
-keep class com.gymlog.app.domain.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedComponent
-keep,allowobfuscation,allowshrinking @dagger.hilt.* class *

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coil
-dontwarn coil.network.**

# Keep generic type information
-keepattributes Signature
-keepattributes Exceptions
