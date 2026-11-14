# Proguard rules for Fisalia Files
-keep class com.hierynomus.** { *; }
-keep class org.apache.commons.net.** { *; }
-keep class com.jcraft.jsch.** { *; }
-keep class com.fisalia.files.data.model.** { *; }
-dontwarn com.hierynomus.**
-dontwarn org.bouncycastle.**
-dontwarn com.jcraft.jsch.**
