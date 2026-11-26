# Pocket Sensei R8 / ProGuard rules

# Keep line numbers for readable crash stack traces in Play Console.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# WebView <-> JS bridge (Sensei 3D avatar). Methods are invoked by name from JavaScript.
-keepclassmembers class com.example.ui.view.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Firestore maps documents to these POJOs by reflection (field/property names must survive).
-keep class com.example.data.model.** { *; }

# Room entities / DAOs are referenced by generated code; keep their members.
-keep class com.example.data.local.** { *; }

# Play Billing
-keep class com.android.billingclient.** { *; }
