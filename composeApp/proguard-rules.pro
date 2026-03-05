# SQLDelight
-keep class com.hieuwu.supabasestorageclient.database.** { *; }

# Supabase & Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.IntellijIdeaDebugDetector

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Koin
-keep class org.koin.** { *; }

# Coil
-keep class coil3.** { *; }

# General Android
-keep class androidx.security.crypto.** { *; }
