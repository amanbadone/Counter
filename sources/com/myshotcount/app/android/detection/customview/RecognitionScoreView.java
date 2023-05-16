package com.myshotcount.app.android.detection.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import com.myshotcount.app.android.detection.tflite.Classifier;
import java.util.List;

public class RecognitionScoreView extends View implements ResultsView {
    private static final float TEXT_SIZE_DIP = 14.0f;
    private final Paint bgPaint;
    private final Paint fgPaint;
    private List<Classifier.Recognition> results;
    private final float textSizePx = TypedValue.applyDimension(1, TEXT_SIZE_DIP, getResources().getDisplayMetrics());

    public RecognitionScoreView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Paint paint = new Paint();
        this.fgPaint = paint;
        paint.setTextSize(this.textSizePx);
        Paint paint2 = new Paint();
        this.bgPaint = paint2;
        paint2.setColor(-868055564);
    }

    public void setResults(List<Classifier.Recognition> list) {
        this.results = list;
        postInvalidate();
    }

    public void onDraw(Canvas canvas) {
        int textSize = (int) (this.fgPaint.getTextSize() * 1.5f);
        canvas.drawPaint(this.bgPaint);
        List<Classifier.Recognition> list = this.results;
        if (list != null) {
            for (Classifier.Recognition next : list) {
                canvas.drawText(next.getTitle() + ": " + next.getConfidence(), 10.0f, (float) textSize, this.fgPaint);
                textSize += (int) (this.fgPaint.getTextSize() * 1.5f);
            }
        }
    }
}
