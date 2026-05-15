-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }

-keep class com.univesp.lumme.data.remote.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**

-keep class com.univesp.lumme.data.local.** { *; }
