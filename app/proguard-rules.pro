# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Lenovo\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom keep rules here if needed.

-dontwarn sun.security.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn com.hierynomus.**
-dontwarn net.i2p.crypto.eddsa.**
-dontwarn org.slf4j.**
-dontwarn com.jcraft.jsch.**
-dontwarn com.sun.jna.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.ietf.jgss.**
-dontwarn org.newsclub.net.unix.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase

# Keep entities
-keep class com.herwin.netez.data.** { *; }

# SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

# SSH & SFTP (sshj, jsch, bouncystle)
-keep class com.hierynomus.** { *; }
-keep class com.jcraft.jsch.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class net.i2p.crypto.eddsa.** { *; }
-keepclassmembers class * implements org.bouncycastle.crypto.BlockCipher {
    public <init>();
}

# Network
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# WorkManager
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class com.herwin.netez.worker.** { *; }
-keep class com.herwin.netez.workers.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}


