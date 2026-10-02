package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.WebView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    private WebView webView;

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
        
        // STANDARDNO REŠENJE:
        // Povezujemo zvanični callback koji SwipeRefreshLayout koristi da proveri 
        // da li WebView (uključujući i njegove unutrašnje skrolabilne elemente) 
        // može da se pomera nagore.
        setOnChildScrollUpCallback(new OnChildScrollUpCallback() {
            @Override
            public boolean canChildScrollUp(SwipeRefreshLayout parent, android.view.View child) {
                if (webView != null) {
                    // canScrollVertically(-1) proverava da li se WebView ili bilo koji 
                    // njegov unutrašnji element pod prstom/fokusom može pomeriti nagore.
                    // Ako može, vraćamo true -> SwipeRefreshLayout zna da korisnik skroluje tekst 
                    // i NIKADA neće prikazati spiner!
                    return webView.canScrollVertically(-1);
                }
                return false;
            }
        });
    }
}
