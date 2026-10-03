package com.webhtml.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.view.ViewCompat;

class CircleImageView extends AppCompatImageView {

    private int mShadowRadius;
    private Animation.AnimationListener mListener;

    // Konstruktor koji koristi GoNativeSwipeRefreshLayout (sa 2 parametra)
    public CircleImageView(Context context, int color) {
        super(context);
        init(color, 20f); // Podrazumevani radius
    }

    // Konstruktor sa 3 parametra (ako se negde poziva)
    public CircleImageView(Context context, int color, float radius) {
        super(context);
        init(color, radius);
    }

    private void init(int color, float radius) {
        final float density = getContext().getResources().getDisplayMetrics().density;
        final int shadowYOffset = (int) (density * 1.75f);
        final int shadowXOffset = (int) (0f * density);

        mShadowRadius = (int) (density * 3.5f);
        ShapeDrawable circle;
        if (elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, density * 4.0f);
        } else {
            OvalShape oval = new OvalShape() {
                @Override
                public void draw(Canvas canvas, Paint paint) {
                    int viewWidth = CircleImageView.this.getWidth();
                    int viewHeight = CircleImageView.this.getHeight();
                    canvas.drawCircle(viewWidth / 2, viewHeight / 2, (viewWidth / 2), paint);
                }
            };
            circle = new ShapeDrawable(oval);
            ViewCompat.setLayerType(this, View.LAYER_TYPE_SOFTWARE, circle.getPaint());
            circle.getPaint().setShadowLayer(mShadowRadius, shadowXOffset, shadowYOffset, 0x1E000000);
            int padding = mShadowRadius;
            setPadding(padding, padding, padding, padding);
        }
        circle.getPaint().setColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(circle);
        } else {
            setBackgroundDrawable(circle);
        }
    }

    private boolean elevationSupported() {
        return android.os.Build.VERSION.SDK_INT >= 21;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!elevationSupported()) {
            int diameter = getMeasuredWidth() + mShadowRadius * 2;
            int height = getMeasuredHeight() + mShadowRadius * 2;
            setMeasuredDimension(diameter, height);
        }
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }
}
