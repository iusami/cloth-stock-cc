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

# Room Database の設定
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# CameraX の設定
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Glide の設定
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# データクラスの保持（Room Entityなど）
-keep class com.example.clothstock.data.model.** { *; }

# ViewModelの保持
-keep class com.example.clothstock.ui.**.** extends androidx.lifecycle.ViewModel { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile