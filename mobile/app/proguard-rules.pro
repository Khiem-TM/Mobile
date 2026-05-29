# Keep generated Moshi adapters and Retrofit service metadata.
-keep class **JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Hilt/Room/WorkManager generally work with the Android Gradle plugin defaults.
# Add backend/model-specific keep rules here if release smoke tests expose missing reflection metadata.
