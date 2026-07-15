# Keep WebView JS interfaces if any get added later.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
