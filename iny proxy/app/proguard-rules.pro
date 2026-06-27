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
# ========================================
# PROGUARD RULES - KEAMANAN MAXIMAL
# ========================================

# ========================================
# 1. Keep WebView Interface (PENTING!)
# ========================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class com.whatsap.whatunban.MainActivity$WebAppInterface {
    public *;
}

# ========================================
# 2. Keep MainActivity (biar gak crash)
# ========================================
-keep public class com.whatsap.whatunban.MainActivity

# ========================================
# 3. Keep semua class di package
# ========================================
-keep class com.whatsap.whatunban.** { *; }

# ========================================
# 4. Keep HTML assets
# ========================================
-keep class **.R$*
-keepattributes *Annotation*

# ========================================
# 5. HILANGKAN SEMUA LOG (biar gak kebaca)
# ========================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** println(...);
}

# ========================================
# 6. Hilangkan Source Code Attribution
# ========================================
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ========================================
# 7. Hilangkan semua atribut debug
# ========================================
-keepattributes !Code, !SourceFile, !LineNumberTable

# ========================================
# 8. Obfuscate nama class, method, variable
# ========================================
-obfuscationdictionary obfuscation.txt
-classobfuscationdictionary obfuscation.txt
-packageobfuscationdictionary obfuscation.txt

# ========================================
# 9. Flatten package (biar makin susah dibaca)
# ========================================
-flattenpackagehierarchy ''
-repackageclasses ''

# ========================================
# 10. Encrypt Strings (jika didukung)
# ========================================
# -encryptstrings

# ========================================
# 11. Keep semua method yang dipanggil dari JS
# ========================================
-keepclassmembers class com.whatsap.whatunban.MainActivity$WebAppInterface {
    public *;
}

# ========================================
# 12. Keep semua class yang extend Activity
# ========================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ========================================
# 13. Keep View constructor (biar layout gak error)
# ========================================
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ========================================
# 14. Keep JSON dan XML
# ========================================
-keep class org.json.** { *; }
-keep class org.xml.** { *; }

# ========================================
# 15. Keep WebView (biar gak error)
# ========================================
-keep class android.webkit.** { *; }

# ========================================
# 16. Hilangkan semua metadata (biar susah dibaca)
# ========================================
-dontwarn android.webkit.**
-dontwarn org.apache.**
-dontwarn org.json.**
