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
    private boolean isLockedToWebView = false;

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
                isLockedToWebView = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);

                // Ako je pomeranje više horizontalno nego vertikalno, pusti WebView
                if (dx > dy) {
                    return super.onInterceptTouchEvent(ev);
                }

                if (webView != null) {
                    // Ključna GitHub fora: 
                    // Pitamo WebView da li se sadržaj u njemu pomera ili može da se skroluje nagore.
                    // Ako je korisnik unutar panela i skroluje, canScrollVertically(-1) vraća true.
                    if (webView.canScrollVertically(-1)) {
                        // Sprečavamo SwipeRefreshLayout da presretne gest!
                        return false; 
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
