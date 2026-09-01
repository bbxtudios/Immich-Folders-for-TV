# Reglas Proguard / R8 para Immich TV

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers enum * { *; }

# KotlinX Serialization y Modelos de Datos
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable class *;
}
-keep,allowobfuscation,allowshrinking class com.bbxtudios.immichtv.data.model.** { *; }
-keepclassmembers class com.bbxtudios.immichtv.data.model.** { *; }

# Retrofit y OkHttp
-keepattributes EnclosingMethod
-keep interface com.bbxtudios.immichtv.data.api.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
