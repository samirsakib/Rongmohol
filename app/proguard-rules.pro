# Rongmohal Production R8 / ProGuard Keep Rules

# Preserve line numbers and source filenames for Crashlytics/stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization (Navigation Compose @Serializable routes)
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.ui.navigation.AppDestination** { *; }

# Firebase RTDB & Firestore & Cloud Functions model safety
-keepclassmembers class com.example.data.remote.** { *; }
-keepclassmembers class com.example.domain.model.** { *; }
-keepattributes Signature

# Room & Hilt
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void assertNotMainThread();
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Jetpack Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
