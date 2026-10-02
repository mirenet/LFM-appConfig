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
                break;

            case MotionEvent.ACTION_MOVE:
                float currentY = ev.getY();
                float currentX = ev.getX();
                float dy = currentY - startY;
                float dx = Math.abs(currentX - startX);

                // Ako je korisnik krenuo više horizontalno nego vertikalno, ne diramo ništa
                if (dx > Math.abs(dy)) {
                    break;
                }

                // Uslov za povlačenje:
                // 1. Vuče nadole (dy > 0)
                // 2. WebView uopšte ne može da se skroluje nagore (na vrhu je)
                // 3. Nismo u sred unutrašnjeg skrola nekog drugog elementa
                if (dy > 0 && webView != null && !webView.canScrollVertically(-1)) {
                    // Ako je pomeraj veći od minimalnog praga, preuzimamo gest
                    if (dy > 20 || isPulling) {
                        isPulling = true;
                        
                        // Skaliramo pomeraj da ide glatko uz blagi otpor
                        translationY = Math.min((dy - 20) * 0.4f, 300f);
                        webView.setTranslationY(translationY);

                        if (translationY > 100 && progressBar.getVisibility() != View.VISIBLE) {
                            progressBar.setVisibility(View.VISIBLE);
                        }

                        // Vraćamo true da sprečimo WebView da primi ovaj pokret povlačenja nadole
                        // i pretvorimo ga u naš pull-to-refresh efekat
                        return true;
                    }
                } else {
                    // Ako je korisnik krenuo nagore ili je WebView u sred skrola, 
                    // vraćamo poziciju u nulu ako je bila pomerena
                    if (translationY > 0) {
                        resetPosition();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
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
