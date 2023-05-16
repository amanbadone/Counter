package com.myshotcount.app.android.detection;

import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Size;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.myshotcount.app.android.R;
import com.myshotcount.app.android.detection.env.ImageUtils;
import com.myshotcount.app.android.detection.env.Logger;
import java.nio.ByteBuffer;

public abstract class CameraActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener, Camera.PreviewCallback, CompoundButton.OnCheckedChangeListener {
    private static final Logger LOGGER = new Logger();
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = "android.permission.CAMERA";
    private SwitchCompat apiSwitchCompat;
    private Button beginButton;
    protected ImageView bottomSheetArrowImageView;
    /* access modifiers changed from: private */
    public LinearLayout bottomSheetLayout;
    protected TextView cropValueTextView;
    private boolean debug = false;
    protected TextView frameValueTextView;
    /* access modifiers changed from: private */
    public LinearLayout gestureLayout;
    private Handler handler;
    private HandlerThread handlerThread;
    private Runnable imageConverter;
    protected TextView inferenceTimeTextView;
    /* access modifiers changed from: private */
    public boolean isProcessingFrame = false;
    private Runnable postInferenceCallback;
    protected int previewHeight = 0;
    protected int previewWidth = 0;
    /* access modifiers changed from: private */
    public int[] rgbBytes = null;
    /* access modifiers changed from: private */
    public BottomSheetBehavior<LinearLayout> sheetBehavior;
    private boolean useCamera2API;
    /* access modifiers changed from: private */
    public int yRowStride;
    /* access modifiers changed from: private */
    public byte[][] yuvBytes = new byte[3][];

    /* access modifiers changed from: protected */
    public abstract Size getDesiredPreviewFrameSize();

    /* access modifiers changed from: protected */
    public abstract int getLayoutId();

    /* access modifiers changed from: protected */
    public abstract void onPreviewSizeChosen(Size size, int i);

    /* access modifiers changed from: protected */
    public abstract void processImage();

    /* access modifiers changed from: protected */
    public abstract void setNumThreads(int i);

    /* access modifiers changed from: protected */
    public abstract void setUseNNAPI(boolean z);

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        Logger logger = LOGGER;
        logger.d("onCreate " + this, new Object[0]);
        requestWindowFeature(1);
        getWindow().setFlags(1024, 1024);
        super.onCreate((Bundle) null);
        getWindow().addFlags(128);
        setContentView((int) R.layout.tfe_od_activity_camera);
        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }
        this.apiSwitchCompat = (SwitchCompat) findViewById(R.id.api_info_switch);
        this.bottomSheetLayout = (LinearLayout) findViewById(R.id.bottom_sheet_layout);
        this.gestureLayout = (LinearLayout) findViewById(R.id.gesture_layout);
        this.sheetBehavior = BottomSheetBehavior.from(this.bottomSheetLayout);
        this.bottomSheetArrowImageView = (ImageView) findViewById(R.id.bottom_sheet_arrow);
        Button button = (Button) findViewById(R.id.beginButton);
        this.beginButton = button;
        button.setText("BEGIN SESSION");
        this.beginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CameraActivity.this.beginButtonPressed();
            }
        });
        this.gestureLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < 16) {
                    CameraActivity.this.gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    CameraActivity.this.gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                CameraActivity.this.bottomSheetLayout.getMeasuredWidth();
                CameraActivity.this.sheetBehavior.setPeekHeight(CameraActivity.this.gestureLayout.getMeasuredHeight());
            }
        });
        this.sheetBehavior.setHideable(false);
        this.sheetBehavior.setState(3);
        this.bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
        this.sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            public void onSlide(View view, float f) {
            }

            public void onStateChanged(View view, int i) {
                if (i == 2) {
                    CameraActivity.this.bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                } else if (i == 3) {
                    CameraActivity.this.bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                } else if (i == 4) {
                    CameraActivity.this.bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                }
            }
        });
        this.frameValueTextView = (TextView) findViewById(R.id.frame_info);
        this.cropValueTextView = (TextView) findViewById(R.id.crop_info);
        this.inferenceTimeTextView = (TextView) findViewById(R.id.inference_info);
        this.apiSwitchCompat.setOnCheckedChangeListener(this);
    }

    /* access modifiers changed from: private */
    public void beginButtonPressed() {
        System.out.println(this.beginButton.getText());
        if (this.beginButton.getText() == "BEGIN SESSION") {
            showFrameInfo("0");
            showCropInfo("0");
            showInference("-");
            this.beginButton.setText("END SESSION");
            this.beginButton.setBackgroundColor(getResources().getColor(R.color.blue));
            return;
        }
        this.beginButton.setText("BEGIN SESSION");
        this.beginButton.setBackgroundColor(getResources().getColor(R.color.red));
    }

    /* access modifiers changed from: protected */
    public int[] getRgbBytes() {
        this.imageConverter.run();
        return this.rgbBytes;
    }

    /* access modifiers changed from: protected */
    public int getLuminanceStride() {
        return this.yRowStride;
    }

    /* access modifiers changed from: protected */
    public byte[] getLuminance() {
        return this.yuvBytes[0];
    }

    public void onPreviewFrame(final byte[] bArr, final Camera camera) {
        if (this.isProcessingFrame) {
            LOGGER.w("Dropping frame!", new Object[0]);
            return;
        }
        try {
            if (this.rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                this.previewHeight = previewSize.height;
                int i = previewSize.width;
                this.previewWidth = i;
                this.rgbBytes = new int[(i * this.previewHeight)];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
            this.isProcessingFrame = true;
            this.yuvBytes[0] = bArr;
            this.yRowStride = this.previewWidth;
            this.imageConverter = new Runnable() {
                public void run() {
                    ImageUtils.convertYUV420SPToARGB8888(bArr, CameraActivity.this.previewWidth, CameraActivity.this.previewHeight, CameraActivity.this.rgbBytes);
                }
            };
            this.postInferenceCallback = new Runnable() {
                public void run() {
                    camera.addCallbackBuffer(bArr);
                    boolean unused = CameraActivity.this.isProcessingFrame = false;
                }
            };
            processImage();
        } catch (Exception e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        }
    }

    public void onImageAvailable(ImageReader imageReader) {
        int i;
        int i2 = this.previewWidth;
        if (i2 != 0 && (i = this.previewHeight) != 0) {
            if (this.rgbBytes == null) {
                this.rgbBytes = new int[(i2 * i)];
            }
            try {
                final Image acquireLatestImage = imageReader.acquireLatestImage();
                if (acquireLatestImage != null) {
                    if (this.isProcessingFrame) {
                        acquireLatestImage.close();
                        return;
                    }
                    this.isProcessingFrame = true;
                    Trace.beginSection("imageAvailable");
                    Image.Plane[] planes = acquireLatestImage.getPlanes();
                    fillBytes(planes, this.yuvBytes);
                    this.yRowStride = planes[0].getRowStride();
                    final int rowStride = planes[1].getRowStride();
                    final int pixelStride = planes[1].getPixelStride();
                    this.imageConverter = new Runnable() {
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(CameraActivity.this.yuvBytes[0], CameraActivity.this.yuvBytes[1], CameraActivity.this.yuvBytes[2], CameraActivity.this.previewWidth, CameraActivity.this.previewHeight, CameraActivity.this.yRowStride, rowStride, pixelStride, CameraActivity.this.rgbBytes);
                        }
                    };
                    this.postInferenceCallback = new Runnable() {
                        public void run() {
                            acquireLatestImage.close();
                            boolean unused = CameraActivity.this.isProcessingFrame = false;
                        }
                    };
                    processImage();
                    Trace.endSection();
                }
            } catch (Exception e) {
                LOGGER.e(e, "Exception!", new Object[0]);
                Trace.endSection();
            }
        }
    }

    public synchronized void onStart() {
        Logger logger = LOGGER;
        logger.d("onStart " + this, new Object[0]);
        super.onStart();
    }

    public synchronized void onResume() {
        Logger logger = LOGGER;
        logger.d("onResume " + this, new Object[0]);
        super.onResume();
        HandlerThread handlerThread2 = new HandlerThread("inference");
        this.handlerThread = handlerThread2;
        handlerThread2.start();
        this.handler = new Handler(this.handlerThread.getLooper());
    }

    public synchronized void onPause() {
        Logger logger = LOGGER;
        logger.d("onPause " + this, new Object[0]);
        this.handlerThread.quitSafely();
        try {
            this.handlerThread.join();
            this.handlerThread = null;
            this.handler = null;
        } catch (InterruptedException e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        }
        super.onPause();
    }

    public synchronized void onStop() {
        Logger logger = LOGGER;
        logger.d("onStop " + this, new Object[0]);
        super.onStop();
    }

    public synchronized void onDestroy() {
        Logger logger = LOGGER;
        logger.d("onDestroy " + this, new Object[0]);
        super.onDestroy();
    }

    /* access modifiers changed from: protected */
    public synchronized void runInBackground(Runnable runnable) {
        if (this.handler != null) {
            this.handler.post(runnable);
        }
    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i != 1) {
            return;
        }
        if (allPermissionsGranted(iArr)) {
            setFragment();
        } else {
            requestPermission();
        }
    }

    private static boolean allPermissionsGranted(int[] iArr) {
        for (int i : iArr) {
            if (i != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT < 23 || checkSelfPermission(PERMISSION_CAMERA) == 0) {
            return true;
        }
        return false;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(this, "Camera permission is required for this demo", 1).show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, 1);
        }
    }

    private boolean isHardwareLevelSupported(CameraCharacteristics cameraCharacteristics, int i) {
        int intValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue();
        return intValue == 2 ? i == intValue : i <= intValue;
    }

    private String chooseCamera() {
        boolean z;
        CameraManager cameraManager = (CameraManager) getSystemService("camera");
        try {
            for (String str : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(str);
                Integer num = (Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (num == null || num.intValue() != 0) {
                    if (((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)) != null) {
                        if (num.intValue() != 2) {
                            if (!isHardwareLevelSupported(cameraCharacteristics, 1)) {
                                z = false;
                                this.useCamera2API = z;
                                LOGGER.i("Camera API lv2?: %s", Boolean.valueOf(z));
                                return str;
                            }
                        }
                        z = true;
                        this.useCamera2API = z;
                        LOGGER.i("Camera API lv2?: %s", Boolean.valueOf(z));
                        return str;
                    }
                }
            }
            return null;
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera", new Object[0]);
            return null;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v1, resolved type: com.myshotcount.app.android.detection.LegacyCameraConnectionFragment} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v4, resolved type: com.myshotcount.app.android.detection.CameraConnectionFragment} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v5, resolved type: com.myshotcount.app.android.detection.LegacyCameraConnectionFragment} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v6, resolved type: com.myshotcount.app.android.detection.LegacyCameraConnectionFragment} */
    /* access modifiers changed from: protected */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setFragment() {
        /*
            r4 = this;
            java.lang.String r0 = r4.chooseCamera()
            boolean r1 = r4.useCamera2API
            if (r1 == 0) goto L_0x001d
            com.myshotcount.app.android.detection.CameraActivity$8 r1 = new com.myshotcount.app.android.detection.CameraActivity$8
            r1.<init>()
            int r2 = r4.getLayoutId()
            android.util.Size r3 = r4.getDesiredPreviewFrameSize()
            com.myshotcount.app.android.detection.CameraConnectionFragment r1 = com.myshotcount.app.android.detection.CameraConnectionFragment.newInstance(r1, r4, r2, r3)
            r1.setCamera(r0)
            goto L_0x002a
        L_0x001d:
            com.myshotcount.app.android.detection.LegacyCameraConnectionFragment r1 = new com.myshotcount.app.android.detection.LegacyCameraConnectionFragment
            int r0 = r4.getLayoutId()
            android.util.Size r2 = r4.getDesiredPreviewFrameSize()
            r1.<init>(r4, r0, r2)
        L_0x002a:
            android.app.FragmentManager r0 = r4.getFragmentManager()
            android.app.FragmentTransaction r0 = r0.beginTransaction()
            r2 = 2131230768(0x7f080030, float:1.8077598E38)
            android.app.FragmentTransaction r0 = r0.replace(r2, r1)
            r0.commit()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.myshotcount.app.android.detection.CameraActivity.setFragment():void");
    }

    /* access modifiers changed from: protected */
    public void fillBytes(Image.Plane[] planeArr, byte[][] bArr) {
        for (int i = 0; i < planeArr.length; i++) {
            ByteBuffer buffer = planeArr[i].getBuffer();
            if (bArr[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", Integer.valueOf(i), Integer.valueOf(buffer.capacity()));
                bArr[i] = new byte[buffer.capacity()];
            }
            buffer.get(bArr[i]);
        }
    }

    public boolean isDebug() {
        return this.debug;
    }

    /* access modifiers changed from: protected */
    public void readyForNextImage() {
        Runnable runnable = this.postInferenceCallback;
        if (runnable != null) {
            runnable.run();
        }
    }

    /* access modifiers changed from: protected */
    public int getScreenOrientation() {
        LOGGER.i("getWindowManager().getDefaultDisplay().getRotation()", new Object[0]);
        LOGGER.i(String.valueOf(getWindowManager().getDefaultDisplay().getRotation()), new Object[0]);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == 1) {
            return 90;
        }
        if (rotation == 2) {
            return 180;
        }
        if (rotation != 3) {
            return 0;
        }
        return 270;
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        setUseNNAPI(z);
        if (z) {
            this.apiSwitchCompat.setText("NNAPI");
        } else {
            this.apiSwitchCompat.setText("TFLITE");
        }
    }

    /* access modifiers changed from: protected */
    public void showFrameInfo(String str) {
        this.frameValueTextView.setText(str);
    }

    /* access modifiers changed from: protected */
    public void showCropInfo(String str) {
        this.cropValueTextView.setText(str);
    }

    /* access modifiers changed from: protected */
    public void showInference(String str) {
        this.inferenceTimeTextView.setText(str);
    }
}
