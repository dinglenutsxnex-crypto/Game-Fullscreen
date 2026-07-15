package com.dinglenuts.gamewebview

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * Single-purpose fullscreen WebView shell.
 *
 * Loads a fixed URL and hides both the status bar and the navigation bar so
 * the game feels like a native, installed app rather than a browser tab.
 */
class MainActivity : AppCompatActivity() {

    // Hardcoded target: change this to point the shell at a different game.
    private val gameUrl = "https://dinglenutsxnex-crypto.github.io/-"

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind system bars before anything else is laid out.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hideSystemBars()

        webView = findViewById(R.id.webview)
        webView.setBackgroundColor(Color.BLACK)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        // Zoom support is required for the engine to actually apply the
        // fit-to-screen scale computed by useWideViewPort/overview mode.
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        // 0 = let the engine auto-compute the initial scale instead of
        // defaulting to 100%, which is what causes desktop-sized content to
        // spill past the edges of the screen.
        webView.setInitialScale(0)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): Boolean {
                // Keep navigation inside the WebView, including redirects
                // that stay within the same site.
                return false
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                // Belt-and-braces: force the page to stretch/squeeze into the
                // visible screen even if the page itself isn't responsive or
                // is missing a proper viewport meta tag.
                view.evaluateJavascript(FIT_TO_SCREEN_JS, null)
                view.postDelayed({ view.evaluateJavascript(FIT_TO_SCREEN_JS, null) }, 600)
                view.postDelayed({ view.evaluateJavascript(FIT_TO_SCREEN_JS, null) }, 1800)
            }
        }
        webView.webChromeClient = WebChromeClient()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(gameUrl)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        // Forces the loaded page to occupy exactly the visible screen size,
        // stretching or squeezing it non-uniformly if needed. This is a
        // deliberate override for pages that overflow the viewport (fixed
        // desktop-style widths, missing/incorrect viewport meta tags, game
        // canvases sized for a specific resolution, etc).
        private const val FIT_TO_SCREEN_JS = """
            (function() {
              try {
                var head = document.head || document.getElementsByTagName('head')[0];
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta && head) {
                  meta = document.createElement('meta');
                  meta.name = 'viewport';
                  head.appendChild(meta);
                }
                if (meta) {
                  meta.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no');
                }

                var html = document.documentElement;
                var body = document.body;
                if (!html || !body) return;

                html.style.margin = '0';
                html.style.padding = '0';
                html.style.overflow = 'hidden';
                html.style.height = '100%';
                html.style.width = '100%';

                body.style.margin = '0';
                body.style.padding = '0';
                body.style.overflow = 'hidden';
                body.style.transformOrigin = 'top left';

                // Reset any transform/explicit size from a previous run
                // before re-measuring the natural content size.
                body.style.transform = 'none';
                body.style.width = 'auto';
                body.style.height = 'auto';

                var contentWidth = Math.max(
                  body.scrollWidth, html.scrollWidth, body.offsetWidth, html.offsetWidth
                );
                var contentHeight = Math.max(
                  body.scrollHeight, html.scrollHeight, body.offsetHeight, html.offsetHeight
                );
                var viewWidth = window.innerWidth;
                var viewHeight = window.innerHeight;

                if (contentWidth > 0 && contentHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                  var scaleX = viewWidth / contentWidth;
                  var scaleY = viewHeight / contentHeight;
                  // Only intervene when content actually overflows in a
                  // direction; avoids upscaling tiny pages into blur.
                  if (scaleX < 0.999 || scaleY < 0.999 || scaleX > 1.001 || scaleY > 1.001) {
                    body.style.width = contentWidth + 'px';
                    body.style.height = contentHeight + 'px';
                    body.style.transform = 'scale(' + scaleX + ', ' + scaleY + ')';
                  }
                }
              } catch (e) {}
            })();
        """
    }
}
