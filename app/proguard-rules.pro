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

# Start.io: se conservan las clases del SDK para que no las elimine R8.
-keep class com.startapp.** { *; }
-dontwarn com.startapp.**

# Reglas de Keep para interfaces de serialización generadas.
-keep,includedescriptorclasses class com.andrespar2.wallance.**$$serializer { *; }
