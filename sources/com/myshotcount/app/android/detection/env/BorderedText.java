package com.myshotcount.app.android.detection.env;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import androidx.core.view.ViewCompat;
import java.util.Iterator;
import java.util.Vector;

public class BorderedText {
    private final Paint exteriorPaint;
    private final Paint interiorPaint;
    private final float textSize;

    public BorderedText(float f) {
        this(-1, ViewCompat.MEASURED_STATE_MASK, f);
    }

    public BorderedText(int i, int i2, float f) {
        Paint paint = new Paint();
        this.interiorPaint = paint;
        paint.setTextSize(f);
        this.interiorPaint.setColor(i);
        this.interiorPaint.setStyle(Paint.Style.FILL);
        this.interiorPaint.setAntiAlias(false);
        this.interiorPaint.setAlpha(255);
        Paint paint2 = new Paint();
        this.exteriorPaint = paint2;
        paint2.setTextSize(f);
        this.exteriorPaint.setColor(i2);
        this.exteriorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.exteriorPaint.setStrokeWidth(f / 8.0f);
        this.exteriorPaint.setAntiAlias(false);
        this.exteriorPaint.setAlpha(255);
        this.textSize = f;
    }

    public void setTypeface(Typeface typeface) {
        this.interiorPaint.setTypeface(typeface);
        this.exteriorPaint.setTypeface(typeface);
    }

    public void drawText(Canvas canvas, float f, float f2, String str) {
        canvas.drawText(str, f, f2, this.exteriorPaint);
        canvas.drawText(str, f, f2, this.interiorPaint);
    }

    public void drawText(Canvas canvas, float f, float f2, String str, Paint paint) {
        float measureText = this.exteriorPaint.measureText(str);
        float textSize2 = this.exteriorPaint.getTextSize();
        Paint paint2 = new Paint(paint);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setAlpha(160);
        canvas.drawRect(f, f2 + ((float) ((int) textSize2)), f + ((float) ((int) measureText)), f2, paint2);
        canvas.drawText(str, f, f2 + textSize2, this.interiorPaint);
    }

    public void drawLines(Canvas canvas, float f, float f2, Vector<String> vector) {
        Iterator<String> it = vector.iterator();
        int i = 0;
        while (it.hasNext()) {
            drawText(canvas, f, f2 - (getTextSize() * ((float) ((vector.size() - i) - 1))), it.next());
            i++;
        }
    }

    public void setInteriorColor(int i) {
        this.interiorPaint.setColor(i);
    }

    public void setExteriorColor(int i) {
        this.exteriorPaint.setColor(i);
    }

    public float getTextSize() {
        return this.textSize;
    }

    public void setAlpha(int i) {
        this.interiorPaint.setAlpha(i);
        this.exteriorPaint.setAlpha(i);
    }

    public void getTextBounds(String str, int i, int i2, Rect rect) {
        this.interiorPaint.getTextBounds(str, i, i2, rect);
    }

    public void setTextAlign(Paint.Align align) {
        this.interiorPaint.setTextAlign(align);
        this.exteriorPaint.setTextAlign(align);
    }
}
