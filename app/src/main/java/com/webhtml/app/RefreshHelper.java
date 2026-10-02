package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class RefreshHelper extends FrameLayout {

    private WebView webView;
    private float startY;
    private float startX;
    private float translationY = 0;
    private boolean isRefreshing = false;
    private boolean isPulling = false;
    private boolean canPull = false;
    
    private OnRefreshListener refreshListener;
    private ProgressBar progressBar;

    public interface OnRefreshListener {
        void onRefresh();
    }

    public RefreshHelper(Context context) {
        super(context);
        init(context);
    }

    public RefreshHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setBackgroundColor(Color.parseColor("#070707"));

        progressBar = new ProgressBar(context);
        LayoutParams pbParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pbParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
        pbParams.topMargin = 40;
        progressBar.setLayoutParams(pbParams);
        progressBar.setVisibility(View.GONE);
        addView(progressBar);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof WebView) {
            this.webView = (WebView) child;
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isRefreshing) {
            return super.dispatchTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                startX = ev.getX();
                isPulling = false;
                
                // Ključna stvar: Proveravamo da li je WebView uopšte na vrhu u trenutku spuštanja prsta.
                // Ako je korisnik spustio prst u unutrašnji panel, canScrollVertically(-1) može da zavisi od položaja,
                // ali glavno je da zapamtimo da li je celo stablo na vrhu.
                canPull = (webView != null && !webView.canScrollVertically(-1));
                break;

            case MotionEvent.ACTION_MOVE:
                if (!canPull) {
                    // Ako nismo na vrhu, pusti WebView da radi šta hoće, nema govora o refresh-u
                    return super.dispatchTouchEvent(ev);
                }

                float currentY = ev.getY();
                float currentX = ev.getX();
                float dy = currentY - startY;
                float dx = Math.abs(currentX - startX);

                // Ako ide više levo-desno nego gore-dole, ignoriši
                if (dx > Math.abs(dy)) {
                    return super.dispatchTouchEvent(ev);
                }

                // Korisnik mora da vuče strogo nadole (dy > 0) i da smo provereno na vrhu
                if (dy > 30) {
                    isPulling = true;
                    translationY = Math.min((dy - 30) * 0.4f, 300f);
                    
                    if (webView != null) {
                        webView.setTranslationY(translationY);
                    }

                    if (translationY > 100 && progressBar.getVisibility() != View.VISIBLE) {
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    // Dok vučemo nadole za refresh, sprečavamo WebView da prima ovaj pokret
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean wasPulling = isPulling;
                isPulling = false;
                canPull = false;

                if (wasPulling) {
                    if (translationY > 150) {
                        startRefreshing();
                    } else {
                        resetPosition();
                    }
                    return true;
                }
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    private void startRefreshing() {
        isRefreshing = true;
        translationY = 150f;
        if (webView != null) {
            webView.animate().translationY(translationY).setDuration(200).start();
        }
        if (refreshListener != null) {
            refreshListener.onRefresh();
        }
    }

    public void setRefreshing(boolean refreshing) {
        this.isRefreshing = refreshing;
        if (!refreshing) {
            resetPosition();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void resetPosition() {
        translationY = 0;
        if (webView != null) {
            webView.animate().translationY(0).setDuration(200).start();
        }
        progressBar.setVisibility(View.GONE);
    }
}
