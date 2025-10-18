#######################################
# Room (Entity / DAO / Database)
#######################################
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-keep @androidx.room.* class * { *; }
-dontwarn androidx.room.**

#######################################
# Retrofit + Gson
#######################################
-keep interface retrofit2.** { *; }
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson 직렬화 필드 유지
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature
-keepattributes *Annotation*

# DTO 및 Response 모델 유지
-keep class com.sumi.flowplay.data.model.** { *; }

#######################################
# Hilt (Dagger)
#######################################
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * {
    @dagger.hilt.InstallIn <fields>;
}
-keepattributes *Annotation*
-dontwarn dagger.**
-dontwarn javax.inject.**

#######################################
# DataStore (Preferences)
#######################################
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

#######################################
# Media3 (ExoPlayer)
#######################################
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

#######################################
# Navigation-Compose
#######################################
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

#######################################
# Coil (이미지 로딩)
#######################################
-keep class coil.** { *; }
-dontwarn coil.**
-dontwarn coil3.**
-dontwarn okhttp3.**

#######################################
# Paging
#######################################
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

# PagingSource 난독화 방지
# PagingSource 난독화 방지
-keep class * extends androidx.paging.PagingSource { *; }
-keepclassmembers class * extends androidx.paging.PagingSource {
    *;
}
-keepattributes SourceFile, LineNumberTable

# JamendoPagingSource 개별 보호
-keep class com.sumi.flowplay.data.paging.JamendoPagingSource { *; }

# Jamendo API DTO 보호
-keep class com.sumi.flowplay.data.model.JamendoTrackResponse { *; }
-keep class com.sumi.flowplay.data.model.JamendoTrack { *; }

#######################################
# Kotlin Reflection & Compose
#######################################
-keepclassmembers class kotlin.Metadata { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.activity.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-dontwarn kotlin.**
-dontwarn androidx.compose.**
-dontwarn androidx.lifecycle.**

#######################################
# AndroidX & Support
#######################################
-dontwarn android.support.**
-dontwarn androidx.**

#######################################
# Kotlinx Serialization
#######################################
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

#######################################
# 일반 설정
#######################################
# 로그 제거 (릴리즈용, 필요 시 주석)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# 리소스 이름, BuildConfig 등 유지
-keep class **.BuildConfig { *; }
-keep class **.R$* { *; }