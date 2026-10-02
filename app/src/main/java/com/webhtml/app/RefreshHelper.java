package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    private WebView webView;
    private boolean isInternalScrollAtTop = true; // Keširano stanje iz JS-a

    public RefreshHelper(Context context) {
        super(context);
        init();
    }

    public RefreshHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setProgressBackgroundColorSchemeColor(Color.parseColor("#1F1F1F"));
        setColorSchemeColors(Color.parseColor("#FFFFFF"));
    }

    @Override
    public void onViewAdded(android.view.View child) {
        super.onViewAdded(child);
        if (child instanceof WebView) {
            this.webView = (WebView) child;
            setupScrollBridge();
        }
    }

    /**
     * Ubacujemo JS koji kontinuirano osluškuje touch događaje i odmah
     * javlja nativnom kodu da li je element pod prstom na vrhu skrola.
     */
    private void setupScrollBridge() {
        if (webView == null) return;

        // Injektujemo skriptu koja prati touchstart i postavlja flag
        String injectionJs = "(function() {" +
                "  window.addEventListener('touchstart', function(e) {" +
                "    var el = e.target;" +
                "    var atTop = true;" +
                "    while (el && el !== document.body && el !== document.documentElement) {" +
                "      var style = window.getComputedStyle(el);" +
                "      var overflowY = style.getPropertyValue('overflow-y');" +
                "      if ((overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {" +
                "        if (el.scrollTop > 0) {" +
                "          atTop = false;" +
                "          break;" +
                "        }" +
                "      }" +
                "      el = el.parentElement;" +
                "    }" +
                "    if (window.AndroidScrollBridge) {" +
                "      window.AndroidScrollBridge.setScrollAtTop(atTop);" +
                "    }" +
                "  }, {passive: true});" +
                "})();";

        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(injectionJs, null);
            }
        });

        // Registrujemo brzi interfejs za komunikaciju iz JS-a ka nativnom kodu
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void setScrollAtTop(boolean atTop) {
                isInternalScrollAtTop = atTop;
            }
        }, "AndroidScrollBridge");
    }

    /**
     * Ključna metoda: Standardni SwipeRefreshLayout pita ovu metodu
     * da li dete (WebView) može da se skroluje nagore.
     */
    @Override
    public boolean canChildScrollUp() {
        if (webView == null) {
            return super.canChildScrollUp();
        }
        
        // Ako je glavni prozor webview-a sišao sa vrha ILI je unutrašnji panel 
        // pod prstom skrolovan nadole, sprečavamo pull-to-refresh!
        boolean webViewScrolledDown = webView.getScrollY() > 0;
        
        return webViewScrolledDown || !isInternalScrollAtTop;
    }
}
