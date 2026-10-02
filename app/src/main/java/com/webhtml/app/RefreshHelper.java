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
    private float translationY = 0;
    private boolean isRefreshing = false;
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

        // Kreiramo jednostavan indikator (spiner) koji će se pojaviti pri vrhu
        progressBar = new ProgressBar(context);
        LayoutParams pbParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pbParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
        pbParams.topMargin = 40;
        progressBar.setLayoutParams(pbParams);
        progressBar.setVisibility(View.GONE);
        // Možeš prilagoditi boju ako želiš, ili ostaviti standardnu
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isRefreshing) return true;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                canPull = (webView != null && !webView.canScrollVertically(-1));
                break;

            case MotionEvent.ACTION_MOVE:
                float dy = ev.getY() - startY;
                // Ako korisnik vuče nadole, a WebView je na apsolutnom vrhu (nema unutrašnjeg skrola nagore)
                if (canPull && dy > 0 && webView != null && !webView.canScrollVertically(-1)) {
                    // Presrećemo dodir i preuzimamo kontrolu nad povlačenjem
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRefreshing) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - startY;
                if (canPull && dy > 0) {
                    // Ograničavamo maksimalno povlačenje da ne ide unedogled
                    translationY = Math.min(dy * 0.4f, 300f);
                    if (webView != null) {
                        webView.setTranslationY(translationY);
                    }
                    if (translationY > 100 && progressBar.getVisibility() != View.VISIBLE) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (translationY > 150) {
                    // Pokrećemo osvežavanje
                    startRefreshing();
                } else {
                    // Vraćamo nazad glatko
                    resetPosition();
                }
                canPull = false;
                break;
        }
        return super.onTouchEvent(event);
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
