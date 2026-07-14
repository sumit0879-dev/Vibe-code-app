# Add project specific ProGuard rules here.

-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.internal.SerializationConstructorMarker

-keep,includedescriptorclasses class com.vibecode.ide.**$$serializer { *; }
-keepclassmembers class com.vibecode.ide.** {
    *** Companion;
}
-keepclasseswithmembers class com.vibecode.ide.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
