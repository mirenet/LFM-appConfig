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
    private long lastTouchUpTime = 0;
    
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
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isRefreshing) {
            return true;
        }

        int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                startX = ev.getX();
                isPulling = false;
                
                // Osnovna provera: Da li je WebView na apsolutnom vrhu?
                // Dodajemo i zaštitni vremenski prozor (300ms) nakon skrolanja panela 
                // da sprečimo lažni trzaj prsta da okine refresh.
                boolean isAtTop = (webView != null && !webView.canScrollVertically(-1));
                boolean timeElapsed = (System.currentTimeMillis() - lastTouchUpTime) > 300;
                
                canPull = isAtTop && timeElapsed;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!canPull) {
                    return false;
                }

                float currentY = ev.getY();
                float currentX = ev.getX();
                float dy = currentY - startY;
                float dx = Math.abs(currentX - startX);

                // Ako je pokret horizontalniji, ne diramo ništa
                if (dx > Math.abs(dy)) {
                    return false;
                }

                // Samo ako korisnik vuče strogo nadole i nalazimo se na vrhu
                if (dy > 30 && webView != null && !webView.canScrollVertically(-1)) {
                    isPulling = true;
                    // Vraćamo true u onInterceptTouchEvent da preuzmemo kontrolu nad gestom
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                lastTouchUpTime = System.currentTimeMillis();
                canPull = false;
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isRefreshing) {
            return super.onTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float currentY = ev.getY();
                float dy = currentY - startY;

                if (isPulling && dy > 0) {
                    translationY = Math.min((dy - 30) * 0.4f, 300f);
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
                lastTouchUpTime = System.currentTimeMillis();
                if (isPulling) {
                    isPulling = false;
                    if (translationY > 150) {
                        startRefreshing();
                    } else {
                        resetPosition();
                    }
                    return true;
                }
                break;
        }

        return super.onTouchEvent(ev);
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
