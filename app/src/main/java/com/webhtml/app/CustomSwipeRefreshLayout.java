package com.webhtml.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {

    private OnScrollUpCheckListener scrollUpCheckListener;

    public interface OnScrollUpCheckListener {
        boolean canScrollUp();
    }

    public void setOnScrollUpCheckListener(OnScrollUpCheckListener listener) {
        this.scrollUpCheckListener = listener;
    }

    public CustomSwipeRefreshLayout(Context context) {
        super(context);
    }

    public CustomSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canChildScrollUp() {
        if (scrollUpCheckListener != null) {
            return scrollUpCheckListener.canScrollUp();
        }
        return super.canChildScrollUp();
    }
}
