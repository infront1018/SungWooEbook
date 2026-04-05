# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Uncomment this to preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }

# Media3 (ExoPlayer)
-keep class androidx.media3.** { *; }

# PDF Viewer
-keep class com.github.barteksc.pdfviewer.** { *; }
-keep class com.github.mhiew.** { *; }

# Models & View
-keep class com.sungwoobook.ebook.model.** { *; }
-keep class com.sungwoobook.ebook.view.** { *; }