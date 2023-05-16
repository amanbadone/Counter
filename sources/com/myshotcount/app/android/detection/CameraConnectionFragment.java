package com.myshotcount.app.android.detection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.myshotcount.app.android.R;
import com.myshotcount.app.android.detection.customview.AutoFitTextureView;
import com.myshotcount.app.android.detection.env.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraConnectionFragment extends Fragment {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String FRAGMENT_DIALOG = "dialog";
    /* access modifiers changed from: private */
    public static final Logger LOGGER = new Logger();
    private static final int MINIMUM_PREVIEW_SIZE = 320;
    private static final SparseIntArray ORIENTATIONS;
    /* access modifiers changed from: private */
    public Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private final ConnectionCallback cameraConnectionCallback;
    /* access modifiers changed from: private */
    public CameraDevice cameraDevice;
    private String cameraId;
    /* access modifiers changed from: private */
    public final Semaphore cameraOpenCloseLock = new Semaphore(1);
    /* access modifiers changed from: private */
    public final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        }

        public void onCaptureProgressed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureResult captureResult) {
        }
    };
    /* access modifiers changed from: private */
    public CameraCaptureSession captureSession;
    private final ImageReader.OnImageAvailableListener imageListener;
    private final Size inputSize;
    private final int layout;
    private ImageReader previewReader;
    /* access modifiers changed from: private */
    public CaptureRequest previewRequest;
    /* access modifiers changed from: private */
    public CaptureRequest.Builder previewRequestBuilder;
    private Size previewSize;
    private Integer sensorOrientation;
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        public void onOpened(CameraDevice cameraDevice) {
            CameraConnectionFragment.this.cameraOpenCloseLock.release();
            CameraDevice unused = CameraConnectionFragment.this.cameraDevice = cameraDevice;
            CameraConnectionFragment.this.createCameraPreviewSession();
        }

        public void onDisconnected(CameraDevice cameraDevice) {
            CameraConnectionFragment.this.cameraOpenCloseLock.release();
            cameraDevice.close();
            CameraDevice unused = CameraConnectionFragment.this.cameraDevice = null;
        }

        public void onError(CameraDevice cameraDevice, int i) {
            CameraConnectionFragment.this.cameraOpenCloseLock.release();
            cameraDevice.close();
            CameraDevice unused = CameraConnectionFragment.this.cameraDevice = null;
            Activity activity = CameraConnectionFragment.this.getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };
    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            CameraConnectionFragment.this.openCamera(i, i2);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
            CameraConnectionFragment.this.configureTransform(i, i2);
        }
    };
    private AutoFitTextureView textureView;

    public interface ConnectionCallback {
        void onPreviewSizeChosen(Size size, int i);
    }

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        ORIENTATIONS = sparseIntArray;
        sparseIntArray.append(0, 90);
        ORIENTATIONS.append(1, 0);
        ORIENTATIONS.append(2, 270);
        ORIENTATIONS.append(3, 180);
    }

    private CameraConnectionFragment(ConnectionCallback connectionCallback, ImageReader.OnImageAvailableListener onImageAvailableListener, int i, Size size) {
        this.cameraConnectionCallback = connectionCallback;
        this.imageListener = onImageAvailableListener;
        this.layout = i;
        this.inputSize = size;
    }

    protected static Size chooseOptimalSize(Size[] sizeArr, int i, int i2) {
        int max = Math.max(Math.min(i, i2), MINIMUM_PREVIEW_SIZE);
        Size size = new Size(i, i2);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        boolean z = false;
        for (Size size2 : sizeArr) {
            if (size2.equals(size)) {
                z = true;
            }
            if (size2.getHeight() < max || size2.getWidth() < max) {
                arrayList2.add(size2);
            } else {
                arrayList.add(size2);
            }
        }
        LOGGER.i("Desired size: " + size + ", min size: " + max + "x" + max, new Object[0]);
        Logger logger = LOGGER;
        StringBuilder sb = new StringBuilder();
        sb.append("Valid preview sizes: [");
        sb.append(TextUtils.join(", ", arrayList));
        sb.append("]");
        logger.i(sb.toString(), new Object[0]);
        LOGGER.i("Rejected preview sizes: [" + TextUtils.join(", ", arrayList2) + "]", new Object[0]);
        if (z) {
            LOGGER.i("Exact size match found.", new Object[0]);
            return size;
        } else if (arrayList.size() > 0) {
            Size size3 = new Size(800, 450);
            LOGGER.i("Chosen size: " + size3.getWidth() + "x" + size3.getHeight(), new Object[0]);
            return size3;
        } else {
            LOGGER.e("Couldn't find any suitable preview size", new Object[0]);
            return sizeArr[0];
        }
    }

    public static CameraConnectionFragment newInstance(ConnectionCallback connectionCallback, ImageReader.OnImageAvailableListener onImageAvailableListener, int i, Size size) {
        return new CameraConnectionFragment(connectionCallback, onImageAvailableListener, i, size);
    }

    /* access modifiers changed from: private */
    public void showToast(final String str) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, str, 0).show();
                }
            });
        }
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(this.layout, viewGroup, false);
    }

    public void onViewCreated(View view, Bundle bundle) {
        this.textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (this.textureView.isAvailable()) {
            openCamera(this.textureView.getWidth(), this.textureView.getHeight());
        } else {
            this.textureView.setSurfaceTextureListener(this.surfaceTextureListener);
        }
    }

    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void setCamera(String str) {
        this.cameraId = str;
    }

    private void setUpCameraOutputs() {
        try {
            CameraCharacteristics cameraCharacteristics = ((CameraManager) getActivity().getSystemService("camera")).getCameraCharacteristics(this.cameraId);
            this.sensorOrientation = (Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            this.previewSize = chooseOptimalSize(((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(SurfaceTexture.class), this.inputSize.getWidth(), this.inputSize.getHeight());
            if (getResources().getConfiguration().orientation == 2) {
                this.textureView.setAspectRatio(this.previewSize.getWidth(), this.previewSize.getHeight());
            } else {
                this.textureView.setAspectRatio(this.previewSize.getHeight(), this.previewSize.getWidth());
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        } catch (NullPointerException unused) {
            ErrorDialog.newInstance(getString(R.string.tfe_od_camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
            throw new IllegalStateException(getString(R.string.tfe_od_camera_error));
        }
        this.cameraConnectionCallback.onPreviewSizeChosen(this.previewSize, this.sensorOrientation.intValue());
    }

    /* access modifiers changed from: private */
    public void openCamera(int i, int i2) {
        setUpCameraOutputs();
        configureTransform(i, i2);
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService("camera");
        try {
            if (this.cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                cameraManager.openCamera(this.cameraId, this.stateCallback, this.backgroundHandler);
                return;
            }
            throw new RuntimeException("Time out waiting to lock camera opening.");
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        } catch (InterruptedException e2) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e2);
        }
    }

    private void closeCamera() {
        try {
            this.cameraOpenCloseLock.acquire();
            if (this.captureSession != null) {
                this.captureSession.close();
                this.captureSession = null;
            }
            if (this.cameraDevice != null) {
                this.cameraDevice.close();
                this.cameraDevice = null;
            }
            if (this.previewReader != null) {
                this.previewReader.close();
                this.previewReader = null;
            }
            this.cameraOpenCloseLock.release();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } catch (Throwable th) {
            this.cameraOpenCloseLock.release();
            throw th;
        }
    }

    private void startBackgroundThread() {
        HandlerThread handlerThread = new HandlerThread("ImageListener");
        this.backgroundThread = handlerThread;
        handlerThread.start();
        this.backgroundHandler = new Handler(this.backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        this.backgroundThread.quitSafely();
        try {
            this.backgroundThread.join();
            this.backgroundThread = null;
            this.backgroundHandler = null;
        } catch (InterruptedException e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = this.textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(this.previewSize.getWidth(), this.previewSize.getHeight());
            Surface surface = new Surface(surfaceTexture);
            CaptureRequest.Builder createCaptureRequest = this.cameraDevice.createCaptureRequest(1);
            this.previewRequestBuilder = createCaptureRequest;
            createCaptureRequest.addTarget(surface);
            Logger logger = LOGGER;
            logger.i("Opening camera preview: " + this.previewSize.getWidth() + "x" + this.previewSize.getHeight(), new Object[0]);
            ImageReader newInstance = ImageReader.newInstance(this.previewSize.getWidth(), this.previewSize.getHeight(), 35, 2);
            this.previewReader = newInstance;
            newInstance.setOnImageAvailableListener(this.imageListener, this.backgroundHandler);
            this.previewRequestBuilder.addTarget(this.previewReader.getSurface());
            this.cameraDevice.createCaptureSession(Arrays.asList(new Surface[]{surface, this.previewReader.getSurface()}), new CameraCaptureSession.StateCallback() {
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (CameraConnectionFragment.this.cameraDevice != null) {
                        CameraCaptureSession unused = CameraConnectionFragment.this.captureSession = cameraCaptureSession;
                        try {
                            CameraConnectionFragment.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, 4);
                            CameraConnectionFragment.this.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, 2);
                            CaptureRequest unused2 = CameraConnectionFragment.this.previewRequest = CameraConnectionFragment.this.previewRequestBuilder.build();
                            CameraConnectionFragment.this.captureSession.setRepeatingRequest(CameraConnectionFragment.this.previewRequest, CameraConnectionFragment.this.captureCallback, CameraConnectionFragment.this.backgroundHandler);
                        } catch (CameraAccessException e) {
                            CameraConnectionFragment.LOGGER.e(e, "Exception!", new Object[0]);
                        }
                    }
                }

                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    CameraConnectionFragment.this.showToast("Failed");
                }
            }, (Handler) null);
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public void configureTransform(int i, int i2) {
        Activity activity = getActivity();
        if (this.textureView != null && this.previewSize != null && activity != null) {
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            float f = (float) i;
            float f2 = (float) i2;
            RectF rectF = new RectF(0.0f, 0.0f, f, f2);
            RectF rectF2 = new RectF(0.0f, 0.0f, (float) this.previewSize.getHeight(), (float) this.previewSize.getWidth());
            float centerX = rectF.centerX();
            float centerY = rectF.centerY();
            LOGGER.i("String.valueOf(rotation)", new Object[0]);
            LOGGER.i(String.valueOf(rotation), new Object[0]);
            if (1 == rotation || 3 == rotation) {
                rectF2.offset(centerX - rectF2.centerX(), centerY - rectF2.centerY());
                matrix.setRectToRect(rectF, rectF2, Matrix.ScaleToFit.FILL);
                float max = Math.max(f2 / ((float) this.previewSize.getHeight()), f / ((float) this.previewSize.getWidth()));
                matrix.postScale(max, max, centerX, centerY);
                matrix.postRotate((float) ((rotation - 2) * 90), centerX, centerY);
            } else if (2 == rotation) {
                matrix.postRotate(180.0f, centerX, centerY);
            }
            this.textureView.setTransform(matrix);
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        CompareSizesByArea() {
        }

        public int compare(Size size, Size size2) {
            return Long.signum((((long) size.getWidth()) * ((long) size.getHeight())) - (((long) size2.getWidth()) * ((long) size2.getHeight())));
        }
    }

    public static class ErrorDialog extends DialogFragment {
        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String str) {
            ErrorDialog errorDialog = new ErrorDialog();
            Bundle bundle = new Bundle();
            bundle.putString(ARG_MESSAGE, str);
            errorDialog.setArguments(bundle);
            return errorDialog;
        }

        public Dialog onCreateDialog(Bundle bundle) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity).setMessage(getArguments().getString(ARG_MESSAGE)).setPositiveButton(17039370, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity.finish();
                }
            }).create();
        }
    }
}
