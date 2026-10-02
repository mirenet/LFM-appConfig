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
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);

                // Ako korisnik vuče horizontalno, pusti WebView
                if (dx > dy) {
                    return false;
                }

                if (webView != null) {
                    // KLJUČNA PROVERA: 
                    // Ako WebView ima mogućnost skrola nagore (što znači da je ili stranica 
                    // pomerena nadole, ILI se prst nalazi unutar unutrašnjeg panela/teksta 
                    // koji se skroluje), apsolutno zabranjujemo SwipeRefreshLayout-u da preotme gest!
                    if (webView.canScrollVertically(-1)) {
                        // Vraćanjem false ovde sprečavamo roditelja da aktivira spiner
                        return false; 
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
