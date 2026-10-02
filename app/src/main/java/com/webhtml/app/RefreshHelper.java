package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    private WebView webView;
    private float startX, startY;
    private boolean isInsideScrollableElement = false;

    public RefreshHelper(Context context) {
        super(context);
        initStyle();
    }

    public RefreshHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        initStyle();
    }

    private void initStyle() {
        setBackgroundColor(Color.parseColor("#070707"));
        setProgressBackgroundColorSchemeColor(Color.parseColor("#1a1a1c")); 
        setColorSchemeColors(Color.parseColor("#CBD868"));                  
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                
                // Kada korisnik tek spusti prst, odmah pitamo WebView da li je prst 
                // na nekom unutrašnjem elementu koji može da se skroluje
                if (webView != null) {
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    
                    String js = "(function() {" +
                            "  var el = document.elementFromPoint(" + x + ", " + y + ");" +
                            "  while (el && el !== document.body && el !== document.documentElement) {" +
                            "    var style = window.getComputedStyle(el);" +
                            "    var overflowY = style.getPropertyValue('overflow-y');" +
                            "    if ((overflowY === 'auto' || overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {" +
                            "      return true;" +
                            "    }" +
                            "    el = el.parentElement;" +
                            "  }" +
                            "  return false;" +
                            "})()";

                    webView.evaluateJavascript(js, value -> {
                        isInsideScrollableElement = "true".equals(value);
                    });
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);

                // Ako korisnik vuče horizontalno, ne diraj
                if (dx > dy) {
                    return false;
                }

                // KLJUČNO: Ako je prst spušten unutar skrolabilnog elementa (tekst box-a),
                // NIKADA ne dozvoli pull-to-refresh spiner, pusti element da radi svoj skrol!
                if (isInsideScrollableElement) {
                    return false;
                }

                // Ako je WebView skrolovan nadole
                if (webView != null && webView.getScrollY() > 0) {
                    return false;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
