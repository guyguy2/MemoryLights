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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Koin reflection-based DI module wiring
-keep class org.koin.** { *; }
-keep class com.happypuppy.memorylights.di.** { *; }

# Keep Activity classes so the system can instantiate them (also satisfies the
# Instantiatable lint check during lintVitalRelease).
-keep public class * extends androidx.activity.ComponentActivity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Activity

# Preserve line numbers in release stack traces, hide original source filenames
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile