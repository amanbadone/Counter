package com.myshotcount.app.android.detection.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

public class AutoFitTextureView extends TextureView {
    private int ratioHeight;
    private int ratioWidth;

    public AutoFitTextureView(Context context) {
        this(context, (AttributeSet) null);
    }

    public AutoFitTextureView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.ratioWidth = 0;
        this.ratioHeight = 0;
    }

    public void setAspectRatio(int i, int i2) {
        if (i < 0 || i2 < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        this.ratioWidth = i;
        this.ratioHeight = i2;
        requestLayout();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        super.onMeasure(i, i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int i4 = this.ratioWidth;
        if (i4 == 0 || (i3 = this.ratioHeight) == 0) {
            setMeasuredDimension(size, size2);
        } else if (size < (size2 * i4) / i3) {
            setMeasuredDimension(size, (i3 * size) / i4);
        } else {
            setMeasuredDimension((i4 * size2) / i3, size2);
        }
    }
}
