#########################
# retrofit2 - additions #
#########################

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# R8 full mode strips generic signatures from return types if not kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response



#######################
# okhttp3 - additions #
#######################

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**



#####################
# moshi - additions #
#####################

# Keep ToJson/FromJson-annotated methods
-keepclassmembers class * {
  @com.squareup.moshi.FromJson <methods>;
  @com.squareup.moshi.ToJson <methods>;
}

# R8 full mode strips generic signatures from return types if not kept.
-keep,allowobfuscation,allowshrinking class com.squareup.moshi.JsonAdapter



#######################
# glassfy android-sdk #
#######################

-keep class io.glassfy.androidsdk.** { *; }
