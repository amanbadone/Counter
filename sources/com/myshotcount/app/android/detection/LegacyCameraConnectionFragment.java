package com.myshotcount.app.android.detection;

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import com.myshotcount.app.android.R;
import com.myshotcount.app.android.detection.customview.AutoFitTextureView;
import com.myshotcount.app.android.detection.env.ImageUtils;
import com.myshotcount.app.android.detection.env.Logger;
import java.io.IOException;
import java.util.List;

public class LegacyCameraConnectionFragment extends Fragment {
    private static final Logger LOGGER = new Logger();
    private static final SparseIntArray ORIENTATIONS;
    private HandlerThread backgroundThread;
    /* access modifiers changed from: private */
    public Camera camera;
    /* access modifiers changed from: private */
    public Size desiredSize;
    /* access modifiers changed from: private */
    public Camera.PreviewCallback imageListener;
    private int layout;
    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
            Camera unused = LegacyCameraConnectionFragment.this.camera = Camera.open(LegacyCameraConnectionFragment.this.getCameraId());
            try {
                Camera.Parameters parameters = LegacyCameraConnectionFragment.this.camera.getParameters();
                List<String> supportedFocusModes = parameters.getSupportedFocusModes();
                if (supportedFocusModes != null && supportedFocusModes.contains("continuous-picture")) {
                    parameters.setFocusMode("continuous-picture");
                }
                List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
                Size[] sizeArr = new Size[supportedPreviewSizes.size()];
                int i3 = 0;
                for (Camera.Size next : supportedPreviewSizes) {
                    sizeArr[i3] = new Size(next.width, next.height);
                    i3++;
                }
                Size chooseOptimalSize = CameraConnectionFragment.chooseOptimalSize(sizeArr, LegacyCameraConnectionFragment.this.desiredSize.getWidth(), LegacyCameraConnectionFragment.this.desiredSize.getHeight());
                parameters.setPreviewSize(chooseOptimalSize.getWidth(), chooseOptimalSize.getHeight());
                LegacyCameraConnectionFragment.this.camera.setDisplayOrientation(0);
                LegacyCameraConnectionFragment.this.camera.setParameters(parameters);
                LegacyCameraConnectionFragment.this.camera.setPreviewTexture(surfaceTexture);
            } catch (IOException unused2) {
                LegacyCameraConnectionFragment.this.camera.release();
            }
            LegacyCameraConnectionFragment.this.camera.setPreviewCallbackWithBuffer(LegacyCameraConnectionFragment.this.imageListener);
            Camera.Size previewSize = LegacyCameraConnectionFragment.this.camera.getParameters().getPreviewSize();
            LegacyCameraConnectionFragment.this.camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(previewSize.height, previewSize.width)]);
            LegacyCameraConnectionFragment.this.textureView.setAspectRatio(previewSize.width, previewSize.height);
            LegacyCameraConnectionFragment.this.camera.startPreview();
        }
    };
    /* access modifiers changed from: private */
    public AutoFitTextureView textureView;

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        ORIENTATIONS = sparseIntArray;
        sparseIntArray.append(0, 90);
        ORIENTATIONS.append(1, 0);
        ORIENTATIONS.append(2, 270);
        ORIENTATIONS.append(3, 180);
    }

    public LegacyCameraConnectionFragment(Camera.PreviewCallback previewCallback, int i, Size size) {
        this.imageListener = previewCallback;
        this.layout = i;
        this.desiredSize = size;
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
            this.camera.startPreview();
        } else {
            this.textureView.setSurfaceTextureListener(this.surfaceTextureListener);
        }
    }

    public void onPause() {
        stopCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        HandlerThread handlerThread = new HandlerThread("CameraBackground");
        this.backgroundThread = handlerThread;
        handlerThread.start();
    }

    private void stopBackgroundThread() {
        this.backgroundThread.quitSafely();
        try {
            this.backgroundThread.join();
            this.backgroundThread = null;
        } catch (InterruptedException e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        }
    }

    /* access modifiers changed from: protected */
    public void stopCamera() {
        Camera camera2 = this.camera;
        if (camera2 != null) {
            camera2.stopPreview();
            this.camera.setPreviewCallback((Camera.PreviewCallback) null);
            this.camera.release();
            this.camera = null;
        }
    }

    /* access modifiers changed from: private */
    public int getCameraId() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == 0) {
                return i;
            }
        }
        return -1;
    }
}
