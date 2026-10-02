package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    private WebView webView;

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
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && webView != null) {
            float x = ev.getX();
            float y = ev.getY();

            // Pitamo JavaScript: Da li je element ispod prsta na samom vrhu svog skrola?
            String js = "(function() {" +
                    "  var el = document.elementFromPoint(" + x + ", " + y + ");" +
                    "  while (el && el !== document.body) {" +
                    "    var style = window.getComputedStyle(el);" +
                    "    if ((style.overflowY === 'auto' || style.overflowY === 'scroll') && el.scrollTop > 0) {" +
                    "      return 'NO_REFRESH';" + // Unutrašnji panel se skroluje, NEMA REFRESHA
                    "    }" +
                    "    el = el.parentElement;" +
                    "  }" +
                    "  return 'ALLOW_REFRESH';" + // Na vrhu je ili nije u skrolabilnom panelu, DOZVOLI REFRESH
                    "})();";

            webView.evaluateJavascript(js, result -> {
                if (result != null && result.contains("NO_REFRESH")) {
                    setEnabled(false); // Gasimo pull-to-refresh, unutrašnji panel ima apsolutnu slobodu
                } else {
                    setEnabled(true);  // Palimo fabrički pull-to-refresh
                }
            });
        }
        return super.onInterceptTouchEvent(ev);
    }
}
