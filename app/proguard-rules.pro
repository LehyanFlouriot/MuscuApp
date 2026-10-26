# The app only uses generated kotlinx.serialization serializers (no reflection lookups),
# but keep them explicitly so backups written by older versions can always be read.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.lehyanflouriot.muscuapp.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lehyanflouriot.muscuapp.**$$serializer { *; }
