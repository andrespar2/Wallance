# Reglas R8/ProGuard de Wallance.

# Modelos serializados con Kotlin Serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.andrespar2.wallance.** {
    *** Companion;
}
-keepclasseswithmembers class com.andrespar2.wallance.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SDK de publicidad: se conservan sus clases para que R8 no las elimine.
-keep class com.startapp.** { *; }
-dontwarn com.startapp.**

# Reglas de Keep para interfaces de serialización generadas.
-keep,includedescriptorclasses class com.andrespar2.wallance.**$$serializer { *; }
