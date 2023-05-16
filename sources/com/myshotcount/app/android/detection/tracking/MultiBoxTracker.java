package com.myshotcount.app.android.detection.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Pair;
import android.util.TypedValue;
import androidx.core.internal.view.SupportMenu;
import androidx.core.view.InputDeviceCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.myshotcount.app.android.BuildConfig;
import com.myshotcount.app.android.detection.env.BorderedText;
import com.myshotcount.app.android.detection.env.ImageUtils;
import com.myshotcount.app.android.detection.env.Logger;
import com.myshotcount.app.android.detection.tflite.Classifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MultiBoxTracker {
    private static final int[] COLORS = {-16776961, -65536, -16711936, -256, -16711681, -65281, -1, Color.parseColor("#55FF55"), Color.parseColor("#FFA500"), Color.parseColor("#FF8888"), Color.parseColor("#AAAAFF"), Color.parseColor("#FFFFAA"), Color.parseColor("#55AAAA"), Color.parseColor("#AA33AA"), Color.parseColor("#0D0068")};
    private static final float MIN_SIZE = 16.0f;
    private static final float TEXT_SIZE_DIP = 18.0f;
    private final Queue<Integer> availableColors = new LinkedList();
    private final BorderedText borderedText;
    private final Paint boxPaint = new Paint();
    private int frameHeight;
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private final Logger logger = new Logger();
    final List<Pair<Float, RectF>> screenRects = new LinkedList();
    private int sensorOrientation;
    private final float textSizePx;
    private final List<TrackedRecognition> trackedObjects = new LinkedList();

    public MultiBoxTracker(Context context) {
        for (int valueOf : COLORS) {
            this.availableColors.add(Integer.valueOf(valueOf));
        }
        this.boxPaint.setColor(SupportMenu.CATEGORY_MASK);
        this.boxPaint.setStyle(Paint.Style.STROKE);
        this.boxPaint.setStrokeWidth(3.0f);
        this.boxPaint.setStrokeCap(Paint.Cap.ROUND);
        this.boxPaint.setStrokeJoin(Paint.Join.ROUND);
        this.boxPaint.setStrokeMiter(100.0f);
        this.textSizePx = TypedValue.applyDimension(1, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        this.borderedText = new BorderedText(this.textSizePx);
    }

    public synchronized void setFrameConfiguration(int i, int i2, int i3) {
        this.frameWidth = i;
        this.frameHeight = i2;
        this.sensorOrientation = i3;
    }

    public synchronized void drawDebug(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(-1);
        paint.setTextSize(60.0f);
        Paint paint2 = new Paint();
        paint2.setColor(SupportMenu.CATEGORY_MASK);
        paint2.setAlpha(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION);
        paint2.setStyle(Paint.Style.STROKE);
        for (Pair next : this.screenRects) {
            RectF rectF = (RectF) next.second;
            canvas.drawRect(rectF, paint2);
            canvas.drawText(BuildConfig.FLAVOR + next.first, rectF.left, rectF.top, paint);
            BorderedText borderedText2 = this.borderedText;
            float centerX = rectF.centerX();
            float centerY = rectF.centerY();
            borderedText2.drawText(canvas, centerX, centerY, BuildConfig.FLAVOR + next.first);
        }
    }

    public synchronized void trackResults(List<Classifier.Recognition> list, long j) {
        processResults(list);
    }

    private Matrix getFrameToCanvasMatrix() {
        return this.frameToCanvasMatrix;
    }

    public synchronized void draw(Canvas canvas) {
        boolean z = this.sensorOrientation % 180 == 90;
        float min = Math.min(((float) canvas.getHeight()) / ((float) (z ? this.frameWidth : this.frameHeight)), ((float) canvas.getWidth()) / ((float) (z ? this.frameHeight : this.frameWidth)));
        this.frameToCanvasMatrix = ImageUtils.getTransformationMatrix(this.frameWidth, this.frameHeight, (int) (((float) (z ? this.frameHeight : this.frameWidth)) * min), (int) (min * ((float) (z ? this.frameWidth : this.frameHeight))), this.sensorOrientation, false);
        for (TrackedRecognition next : this.trackedObjects) {
            RectF rectF = new RectF(next.location);
            getFrameToCanvasMatrix().mapRect(rectF);
            this.boxPaint.setColor(next.color);
            float min2 = Math.min(rectF.width(), rectF.height()) / 8.0f;
            canvas.drawRoundRect(rectF, min2, min2, this.boxPaint);
        }
    }

    private void processResults(List<Classifier.Recognition> list) {
        LinkedList<Pair> linkedList = new LinkedList<>();
        this.screenRects.clear();
        Matrix matrix = new Matrix(getFrameToCanvasMatrix());
        for (Classifier.Recognition next : list) {
            if (next.getLocation() != null) {
                RectF rectF = new RectF(next.getLocation());
                RectF rectF2 = new RectF();
                matrix.mapRect(rectF2, rectF);
                Logger logger2 = this.logger;
                logger2.v("Result! Frame: " + next.getLocation() + " mapped to screen:" + rectF2, new Object[0]);
                this.screenRects.add(new Pair(next.getConfidence(), rectF2));
                if (rectF.width() >= MIN_SIZE && rectF.height() >= MIN_SIZE) {
                    linkedList.add(new Pair(next.getConfidence(), next));
                }
            }
        }
        this.trackedObjects.clear();
        if (linkedList.isEmpty()) {
            this.logger.v("Nothing to track, aborting.", new Object[0]);
            return;
        }
        for (Pair pair : linkedList) {
            TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = ((Float) pair.first).floatValue();
            trackedRecognition.location = new RectF(((Classifier.Recognition) pair.second).getLocation());
            trackedRecognition.title = ((Classifier.Recognition) pair.second).getTitle();
            if (((Classifier.Recognition) pair.second).getTitle().equals("shot")) {
                trackedRecognition.color = -16776961;
            } else if (((Classifier.Recognition) pair.second).getTitle().equals("dribble")) {
                trackedRecognition.color = InputDeviceCompat.SOURCE_ANY;
            } else if (((Classifier.Recognition) pair.second).getTitle().equals("empty")) {
                trackedRecognition.color = -1;
            } else if (((Classifier.Recognition) pair.second).getTitle().equals("net")) {
                trackedRecognition.color = SupportMenu.CATEGORY_MASK;
            } else if (((Classifier.Recognition) pair.second).getTitle().equals("net_make")) {
                trackedRecognition.color = -16711936;
            } else if (((Classifier.Recognition) pair.second).getTitle().equals("ball")) {
                trackedRecognition.color = -16711681;
            } else if (((Classifier.Recognition) pair.second).getTitle().equals("ball_make")) {
                trackedRecognition.color = -65281;
            } else {
                trackedRecognition.color = -7829368;
            }
            this.trackedObjects.add(trackedRecognition);
            if (this.trackedObjects.size() >= COLORS.length) {
                return;
            }
        }
    }

    private static class TrackedRecognition {
        int color;
        float detectionConfidence;
        RectF location;
        String title;

        private TrackedRecognition() {
        }
    }
}
