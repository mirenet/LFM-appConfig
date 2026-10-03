package com.webhtml.app;

import android.webkit.JavascriptInterface;

public class RefreshBridge {
    private boolean isAtTop = true;

    @JavascriptInterface
    public void setScrollAtTop(boolean atTop) {
        this.isAtTop = atTop;
    }

    public boolean isAtTop() {
        return isAtTop;
    }
}
