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

# Koin 4.x with `viewModelOf(::Class)` and lambda module DSL no longer needs
# a wildcard keep on org.koin.** — module wiring is constructor-reference
# based, not reflection-by-class-name. Keep only the project's DI module
# objects (referenced by name from the Application).
-keep class com.happypuppy.memorylights.di.** { *; }

# Keep Activity classes so the system can instantiate them (also satisfies the
# Instantiatable lint check during lintVitalRelease).
-keep public class * extends androidx.activity.ComponentActivity
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Activity

# Preserve line numbers in release stack traces, hide original source filenames
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile