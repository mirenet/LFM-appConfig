package com.webhtml.app;

import android.webkit.JavascriptInterface;

public class RefreshBridge {
    private boolean isAtTop = true;
    private Runnable onRefreshCallback;

    public RefreshBridge(Runnable onRefreshCallback) {
        this.onRefreshCallback = onRefreshCallback;
    }

    @JavascriptInterface
    public void setScrollAtTop(boolean atTop) {
        this.isAtTop = atTop;
    }

    @JavascriptInterface
    public void triggerRefresh() {
        if (onRefreshCallback != null) {
            onRefreshCallback.run();
        }
    }

    public boolean isAtTop() {
        return isAtTop;
    }
}
