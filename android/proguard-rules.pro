# Add project specific ProGuard rules here.

# Keep gomobile generated classes
-keep class libolm.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep app classes
-keep class net.pangolin.olm.** { *; }

# Keep VPN service
-keep public class * extends android.net.VpnService

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
