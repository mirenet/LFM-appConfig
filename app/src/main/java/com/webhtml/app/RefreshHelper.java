package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    private WebView webView;
    private float startX, startY;
    private int touchSlop;
    private boolean isInternalScrolling = false;

    public RefreshHelper(Context context) {
        super(context);
        init(context);
    }

    public RefreshHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setProgressBackgroundColorSchemeColor(Color.parseColor("#1F1F1F"));
        setColorSchemeColors(Color.parseColor("#FFFFFF"));
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public void onViewAdded(View child) {
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
                startX = ev.getX();
                startY = ev.getY();
                isInternalScrolling = false;

                // Sinhrona/brza provera preko JS-a da li je element pod prstom pomeren sa vrha
                String js = "(function() {" +
                        "  var el = document.elementFromPoint(" + startX + ", " + startY + ");" +
                        "  while (el && el !== document.body && el !== document.documentElement) {" +
                        "    var style = window.getComputedStyle(el);" +
                        "    var overflowY = style.getPropertyValue('overflow-y');" +
                        "    if ((overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {" +
                        "      if (el.scrollTop > 2) {" + // Ako je unutrašnji panel skrolovan nadole bar 2 piksela
                        "        return 'BUSY';" +
                        "      }" +
                        "    }" +
                        "    el = el.parentElement;" +
                        "  }" +
                        "  if (window.scrollY > 2) {" + // Ako je cela stranica skrolovana
                        "    return 'BUSY';" +
                        "  }" +
                        "  return 'FREE';" +
                        "})();";

                // Pošto evaluateJavascript vraća rezultat kroz callback, ovde hvatamo flag
                webView.evaluateJavascript(js, result -> {
                    if (result != null && result.contains("BUSY")) {
                        isInternalScrolling = true;
                    }
                });
                break;

            case MotionEvent.ACTION_MOVE:
                float diffY = ev.getY() - startY;
                float diffX = Math.abs(ev.getX() - startX);

                // Ako korisnik vuče nadole (refresh gest), a nalazimo se u skrolovanom unutrašnjem elementu
                if (diffY > touchSlop && diffY > diffX && isInternalScrolling) {
                    return false; // Strogo zabrani SwipeRefreshLayout-u da preuzme gest!
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }
}
