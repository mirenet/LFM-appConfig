package com.webhtml.app;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class RefreshHelper extends SwipeRefreshLayout {

    public RefreshHelper(Context context) {
        super(context);
        init();
    }

    public RefreshHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Podešavamo boje i pozadinu da prate temu aplikacije
        setProgressBackgroundColorSchemeColor(Color.parseColor("#1F1F1F"));
        setColorSchemeColors(Color.parseColor("#FFFFFF"));
    }
}
