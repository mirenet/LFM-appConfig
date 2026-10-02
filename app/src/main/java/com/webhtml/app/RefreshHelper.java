package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    private WebView webView;
    private float startY = 0f;
    private boolean isLockedForInternalScroll = false;

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
        if (webView == null) {
            return super.onInterceptTouchEvent(ev);
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                isLockedForInternalScroll = false;

                // Koordinatni sistem: pretvaramo y u piksele i pitamo JS da li je element pod prstom na vrhu
                float x = ev.getX();
                float y = ev.getY();

                String js = "(function() {" +
                        "  var el = document.elementFromPoint(" + x + ", " + y + ");" +
                        "  while (el && el !== document.body && el !== document.documentElement) {" +
                        "    var style = window.getComputedStyle(el);" +
                        "    var overflowY = style.getPropertyValue('overflow-y');" +
                        "    if ((overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {" +
                        "      if (el.scrollTop > 0) {" +
                        "        return 'LOCKED';" + // Unutrašnji panel nije na vrhu!
                        "      }" +
                        "    }" +
                        "    el = el.parentElement;" +
                        "  }" +
                        "  if (window.scrollY > 0) {" +
                        "    return 'LOCKED';" + // Glavni prozor nije na vrhu!
                        "  }" +
                        "  return 'FREE';" +
                        "})();";

                webView.evaluateJavascript(js, result -> {
                    if (result != null && result.contains("LOCKED")) {
                        isLockedForInternalScroll = true;
                        setEnabled(false); // Onemogući pull-to-refresh
                    } else {
                        setEnabled(true);  // Omogući pull-to-refresh
                    }
                });
                break;

            case MotionEvent.ACTION_MOVE:
                // Ako je detektovano skrolovanje unutrašnjeg panela, spreči bilo kakvo paljenje refresh-a
                if (isLockedForInternalScroll) {
                    return false;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setEnabled(true);
                isLockedForInternalScroll = false;
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }
}
