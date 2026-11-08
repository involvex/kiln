# Kiln release rules (M0 baseline).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
