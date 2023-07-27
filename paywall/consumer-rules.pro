###################
# glassfy paywall #
###################

# Keep WebView JS interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class io.glassfy.paywall.** { *; }
