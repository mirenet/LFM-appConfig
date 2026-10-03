package com.example.yourapp; // Prilagodi svom paketu

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;

    // JavaScript skripta sa naprednom multi-phase logikom za panele i vrh stranice
    private final String ptrCheckJsContent = 
        "(function () {" +
        "    if (window._ptrBridgeLoaded) return;" +
        "    window._ptrBridgeLoaded = true;" +
        "    let startY = 0;" +
        "    let activeScrollElement = null;" +
        "    " +
        "    function getScrollableParent(el) {" +
        "        let curr = el;" +
        "        while (curr && curr !== document.body && curr !== document.documentElement) {" +
        "            const style = window.getComputedStyle(curr);" +
        "            const overflowY = style.getPropertyValue('overflow-y');" +
        "            const isScrollable = (overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay');" +
        "            if (isScrollable && curr.scrollHeight > curr.clientHeight) {" +
        "                return curr;" +
        "            }" +
        "            curr = curr.parentElement;" +
        "        }" +
        "        return null;" +
        "    }" +
        "    " +
        "    window.addEventListener('touchstart', function (e) {" +
        "        startY = e.touches[0].clientY;" +
        "        activeScrollElement = getScrollableParent(e.target);" +
        "        // U startu uvek gasimo nativni refresh da Android ne otme gest" +
        "        if (window.PtrControl) window.PtrControl.setSwipeEnabled(false);" +
        "    }, { passive: true });" +
        "    " +
        "    window.addEventListener('touchmove', function (e) {" +
        "        let currentY = e.touches[0].clientY;" +
        "        let diff = currentY - startY;" +
        "        " +
        "        // Zanemarujemo sitna pomeranja da sačekamo pravu nameru korisnika" +
        "        if (diff <= 5) return;" +
        "        " +
        "        if (window.scrollY === 0) {" +
        "            if (!activeScrollElement) {" +
        "                // Nismo u skrolabilnom panelu (obična stranica na vrhu) -> DOZVOLI" +
        "                if (window.PtrControl) window.PtrControl.setSwipeEnabled(true);" +
        "            } else {" +
        "                // Jesmo u unutrašnjem panelu: proveravamo da li je ON NA VRHU" +
        "                if (activeScrollElement.scrollTop === 0) {" +
        "                    // Jeste na vrhu -> DOZVOLI pull-to-refresh" +
        "                    if (window.PtrControl) window.PtrControl.setSwipeEnabled(true);" +
        "                } else {" +
        "                    // Nije na vrhu (pomeren nadole) -> ZABRANI, neka se panel skroluje" +
        "                    if (window.PtrControl) window.PtrControl.setSwipeEnabled(false);" +
        "                }" +
        "            }" +
        "        } else {" +
        "            // Glavni prozor nije na vrhu -> ZABRANI" +
        "            if (window.PtrControl) window.PtrControl.setSwipeEnabled(false);" +
        "        }" +
        "    }, { passive: true });" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Povezano sa tvojim XML layout-om

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        webView = findViewById(R.id.webView);

        // Osnovna podešavanja WebView-a
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // Povezivanje JavaScript interfejs mosta
        webView.addJavascriptInterface(new PtrBridge(), "PtrControl");

        // Ubacivanje skripte čim se stranica učita
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(ptrCheckJsContent, null);
            }
        });

        // Šta se dešava kada korisnik pokrene Pull-to-Refresh
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload(); // Osvežava stranicu
                // Isključujemo indikator osvežavanja nakon malog odlaganja ili kada stranica krene da se učitava
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Učitaj željeni URL (zameni svojim linkom)
        webView.loadUrl("https://tvoj-sajt.com");
    }

    // Klasa preko koje JavaScript komunicira sa Android SwipeRefreshLayout-om
    public class PtrBridge {
        @JavascriptInterface
        public void setSwipeEnabled(final boolean enabled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setEnabled(enabled);
                    }
                }
            });
        }
    }
}
