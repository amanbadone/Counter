package com.myshotcount.app.android.detection;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;
import androidx.core.internal.view.SupportMenu;
import com.myshotcount.app.android.BuildConfig;
import com.myshotcount.app.android.R;
import com.myshotcount.app.android.detection.customview.OverlayView;
import com.myshotcount.app.android.detection.env.BorderedText;
import com.myshotcount.app.android.detection.env.ImageUtils;
import com.myshotcount.app.android.detection.env.Logger;
import com.myshotcount.app.android.detection.tflite.Classifier;
import com.myshotcount.app.android.detection.tflite.TFLiteObjectDetectionAPIModel;
import com.myshotcount.app.android.detection.tracking.MultiBoxTracker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DetectorActivity extends CameraActivity implements ImageReader.OnImageAvailableListener {
    private static final Size DESIRED_PREVIEW_SIZE = new Size(480, 270);
    private static final Logger LOGGER = new Logger();
    private static final boolean MAINTAIN_ASPECT = false;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.2f;
    /* access modifiers changed from: private */
    public static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10.0f;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/basketball_labelmap.txt";
    private static final String TF_OD_API_MODEL_FILE = "basketball_detect_v3_quantized_200000_steps.tflite";
    public static int totalMakes = 0;
    public static int totalMisses = 0;
    public static int totalShots = 0;
    private BorderedText borderedText;
    int buffer = 0;
    /* access modifiers changed from: private */
    public boolean computingDetection = false;
    /* access modifiers changed from: private */
    public Bitmap cropCopyBitmap = null;
    /* access modifiers changed from: private */
    public Matrix cropToFrameTransform;
    /* access modifiers changed from: private */
    public Bitmap croppedBitmap = null;
    /* access modifiers changed from: private */
    public Classifier detector;
    private Matrix frameToCropTransform;
    List<String> lastFour = new ArrayList();
    /* access modifiers changed from: private */
    public long lastProcessingTimeMs;
    List<Long> lastTenTimestamps = new ArrayList();
    private Bitmap rgbFrameBitmap = null;
    private Integer sensorOrientation;
    boolean shotInProgress = false;
    private long timestamp = 0;
    /* access modifiers changed from: private */
    public MultiBoxTracker tracker;
    OverlayView trackingOverlay;

    private enum DetectorMode {
        TF_OD_API
    }

    /* access modifiers changed from: protected */
    public int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    public void onPreviewSizeChosen(Size size, int i) {
        float applyDimension = TypedValue.applyDimension(1, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        Logger logger = LOGGER;
        logger.i("onPreviewSizeChosen: " + size, new Object[0]);
        BorderedText borderedText2 = new BorderedText(applyDimension);
        this.borderedText = borderedText2;
        borderedText2.setTypeface(Typeface.MONOSPACE);
        this.tracker = new MultiBoxTracker(this);
        try {
            this.detector = TFLiteObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE, TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!", new Object[0]);
            Toast.makeText(getApplicationContext(), "Classifier could not be initialized", 0).show();
            finish();
        }
        this.previewWidth = size.getWidth();
        this.previewHeight = size.getHeight();
        Integer valueOf = Integer.valueOf(i - getScreenOrientation());
        this.sensorOrientation = valueOf;
        LOGGER.i("Camera orientation relative to screen canvas: %d", valueOf);
        LOGGER.i("Initializing at size %dx%d", Integer.valueOf(this.previewWidth), Integer.valueOf(this.previewHeight));
        this.rgbFrameBitmap = Bitmap.createBitmap(this.previewWidth, this.previewHeight, Bitmap.Config.ARGB_8888);
        this.croppedBitmap = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);
        this.frameToCropTransform = ImageUtils.getTransformationMatrix(this.previewWidth, this.previewHeight, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, this.sensorOrientation.intValue(), false);
        Matrix matrix = new Matrix();
        this.cropToFrameTransform = matrix;
        this.frameToCropTransform.invert(matrix);
        OverlayView overlayView = (OverlayView) findViewById(R.id.tracking_overlay);
        this.trackingOverlay = overlayView;
        overlayView.addCallback(new OverlayView.DrawCallback() {
            public void drawCallback(Canvas canvas) {
                DetectorActivity.this.tracker.draw(canvas);
                if (DetectorActivity.this.isDebug()) {
                    DetectorActivity.this.tracker.drawDebug(canvas);
                }
            }
        });
        this.tracker.setFrameConfiguration(this.previewWidth, this.previewHeight, this.sensorOrientation.intValue());
    }

    /* access modifiers changed from: protected */
    public void processImage() {
        final long j = this.timestamp + 1;
        this.timestamp = j;
        this.trackingOverlay.postInvalidate();
        if (this.computingDetection) {
            readyForNextImage();
            return;
        }
        this.computingDetection = TF_OD_API_IS_QUANTIZED;
        this.rgbFrameBitmap.setPixels(getRgbBytes(), 0, this.previewWidth, 0, 0, this.previewWidth, this.previewHeight);
        readyForNextImage();
        new Canvas(this.croppedBitmap).drawBitmap(this.rgbFrameBitmap, this.frameToCropTransform, (Paint) null);
        runInBackground(new Runnable() {
            public void run() {
                long uptimeMillis = SystemClock.uptimeMillis();
                List<Classifier.Recognition> recognizeImage = DetectorActivity.this.detector.recognizeImage(DetectorActivity.this.croppedBitmap);
                long unused = DetectorActivity.this.lastProcessingTimeMs = SystemClock.uptimeMillis() - uptimeMillis;
                DetectorActivity.this.lastTenTimestamps.add(Long.valueOf(DetectorActivity.this.lastProcessingTimeMs));
                if (DetectorActivity.this.lastTenTimestamps.size() > 100) {
                    DetectorActivity.this.lastTenTimestamps.remove(0);
                }
                for (int i = 0; i < DetectorActivity.this.lastTenTimestamps.size(); i++) {
                    DetectorActivity.this.lastTenTimestamps.get(i).longValue();
                }
                DetectorActivity detectorActivity = DetectorActivity.this;
                Bitmap unused2 = detectorActivity.cropCopyBitmap = Bitmap.createBitmap(detectorActivity.croppedBitmap);
                Canvas canvas = new Canvas(DetectorActivity.this.cropCopyBitmap);
                Paint paint = new Paint();
                paint.setColor(SupportMenu.CATEGORY_MASK);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);
                int i2 = AnonymousClass3.$SwitchMap$com$myshotcount$app$android$detection$DetectorActivity$DetectorMode[DetectorActivity.MODE.ordinal()];
                LinkedList linkedList = new LinkedList();
                int i3 = 0;
                int i4 = 0;
                for (Classifier.Recognition next : recognizeImage) {
                    RectF location = next.getLocation();
                    if (location != null && next.getConfidence().floatValue() >= DetectorActivity.MINIMUM_CONFIDENCE_TF_OD_API) {
                        canvas.drawRect(location, paint);
                        DetectorActivity.this.cropToFrameTransform.mapRect(location);
                        next.setLocation(location);
                        linkedList.add(next);
                        if (next.getTitle().equals("shot")) {
                            i3++;
                        }
                        if (next.getTitle().equals("net_make")) {
                            i4++;
                        }
                    }
                }
                if (i3 > 0) {
                    DetectorActivity.this.lastFour.add("shot");
                } else {
                    DetectorActivity.this.lastFour.add("no");
                }
                if (DetectorActivity.this.lastFour.size() > 4) {
                    DetectorActivity.this.lastFour.remove(0);
                }
                int frequency = Collections.frequency(DetectorActivity.this.lastFour, "shot");
                if (DetectorActivity.this.shotInProgress) {
                    DetectorActivity.this.buffer++;
                    if (DetectorActivity.this.buffer > 12) {
                        DetectorActivity.this.shotInProgress = false;
                        DetectorActivity.totalMisses++;
                    }
                } else {
                    DetectorActivity.this.buffer = 0;
                }
                if (frequency > 1 && !DetectorActivity.this.shotInProgress) {
                    DetectorActivity.totalShots++;
                    DetectorActivity.this.lastFour = new ArrayList();
                    DetectorActivity.this.shotInProgress = DetectorActivity.TF_OD_API_IS_QUANTIZED;
                }
                if (i4 > 0 && DetectorActivity.this.shotInProgress) {
                    DetectorActivity.totalMakes++;
                    DetectorActivity.this.lastFour = new ArrayList();
                    DetectorActivity.this.shotInProgress = false;
                }
                DetectorActivity.this.tracker.trackResults(linkedList, j);
                DetectorActivity.this.trackingOverlay.postInvalidate();
                boolean unused3 = DetectorActivity.this.computingDetection = false;
                DetectorActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        DetectorActivity.this.showFrameInfo(String.valueOf(DetectorActivity.totalShots));
                        DetectorActivity.this.showCropInfo(String.valueOf(DetectorActivity.totalMakes));
                        if (DetectorActivity.totalShots >= 1) {
                            DetectorActivity.this.showInference(String.format("%.2f", new Object[]{Double.valueOf(((double) DetectorActivity.totalMakes) / ((double) DetectorActivity.totalShots))}).replace("0.", BuildConfig.FLAVOR).replace("1.00", "100") + "%");
                            return;
                        }
                        DetectorActivity.this.showInference("-");
                    }
                });
            }
        });
    }

    /* renamed from: com.myshotcount.app.android.detection.DetectorActivity$3  reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$myshotcount$app$android$detection$DetectorActivity$DetectorMode;

        static {
            int[] iArr = new int[DetectorMode.values().length];
            $SwitchMap$com$myshotcount$app$android$detection$DetectorActivity$DetectorMode = iArr;
            try {
                iArr[DetectorMode.TF_OD_API.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
        }
    }

    /* access modifiers changed from: protected */
    public Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    public /* synthetic */ void lambda$setUseNNAPI$0$DetectorActivity(boolean z) {
        this.detector.setUseNNAPI(z);
    }

    /* access modifiers changed from: protected */
    public void setUseNNAPI(boolean z) {
        runInBackground(new Runnable(z) {
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                DetectorActivity.this.lambda$setUseNNAPI$0$DetectorActivity(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$setNumThreads$1$DetectorActivity(int i) {
        this.detector.setNumThreads(i);
    }

    /* access modifiers changed from: protected */
    public void setNumThreads(int i) {
        runInBackground(new Runnable(i) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                DetectorActivity.this.lambda$setNumThreads$1$DetectorActivity(this.f$1);
            }
        });
    }
}
