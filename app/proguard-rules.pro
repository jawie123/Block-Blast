# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number information for Play Console crash/ANR deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Firestore POJOs and Kotlin data classes deserialized via reflection (toObject / toObjects)
-keepattributes *Annotation*,RuntimeVisibleAnnotations,Signature,InnerClasses,EnclosingMethod
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** {
    *;
}

# Preserve WorkManager & Room database implementations and constructors under R8 Full Mode
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.WorkManagerInitializer { *; }
-keep class androidx.work.* { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.startup.InitializationProvider { *; }

# Preserve Google Mobile Ads & User Messaging Platform (UMP) SDK classes
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep public class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
    public boolean *(android.webkit.WebView, java.lang.String);
}

# Preserve Credential Manager & Google Identity classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
