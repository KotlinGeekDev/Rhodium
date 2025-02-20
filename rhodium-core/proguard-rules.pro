# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# preserve the line number information for debugging stack traces.
-dontobfuscate
-keepattributes LocalVariableTable
-keepattributes LocalVariableTypeTable
-keepattributes *Annotation*
-keepattributes SourceFile
-keepattributes LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes MethodParameters
-keepparameternames


# Keep all names
-keepnames class ** { *; }

# Keep All enums
-keep enum ** { *; }

# preserve access to native classses
-keep class fr.acinq.secp256k1.** { *; }
