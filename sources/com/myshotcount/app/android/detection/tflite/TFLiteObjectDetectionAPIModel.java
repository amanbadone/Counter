package com.myshotcount.app.android.detection.tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;
import com.myshotcount.app.android.BuildConfig;
import com.myshotcount.app.android.detection.env.Logger;
import com.myshotcount.app.android.detection.tflite.Classifier;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.tensorflow.lite.Interpreter;

public class TFLiteObjectDetectionAPIModel implements Classifier {
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final Logger LOGGER = new Logger();
    private static final int NUM_DETECTIONS = 10;
    private static final int NUM_THREADS = 4;
    private ByteBuffer imgData;
    private int inputSize;
    private int[] intValues;
    private boolean isModelQuantized;
    private Vector<String> labels = new Vector<>();
    private float[] numDetections;
    private float[][] outputClasses;
    private float[][][] outputLocations;
    private float[][] outputScores;
    private Interpreter tfLite;

    public void close() {
    }

    public void enableStatLogging(boolean z) {
    }

    public String getStatString() {
        return BuildConfig.FLAVOR;
    }

    private TFLiteObjectDetectionAPIModel() {
    }

    private static MappedByteBuffer loadModelFile(AssetManager assetManager, String str) throws IOException {
        AssetFileDescriptor openFd = assetManager.openFd(str);
        return new FileInputStream(openFd.getFileDescriptor()).getChannel().map(FileChannel.MapMode.READ_ONLY, openFd.getStartOffset(), openFd.getDeclaredLength());
    }

    public static Classifier create(AssetManager assetManager, String str, String str2, int i, boolean z) throws IOException {
        Class<float> cls = float.class;
        TFLiteObjectDetectionAPIModel tFLiteObjectDetectionAPIModel = new TFLiteObjectDetectionAPIModel();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(str2.split("file:///android_asset/")[1])));
        while (true) {
            String readLine = bufferedReader.readLine();
            if (readLine == null) {
                break;
            }
            LOGGER.w(readLine, new Object[0]);
            tFLiteObjectDetectionAPIModel.labels.add(readLine);
        }
        bufferedReader.close();
        tFLiteObjectDetectionAPIModel.inputSize = i;
        try {
            tFLiteObjectDetectionAPIModel.tfLite = new Interpreter(loadModelFile(assetManager, str));
            tFLiteObjectDetectionAPIModel.isModelQuantized = z;
            int i2 = z ? 1 : 4;
            int i3 = tFLiteObjectDetectionAPIModel.inputSize;
            ByteBuffer allocateDirect = ByteBuffer.allocateDirect(i3 * 1 * i3 * 3 * i2);
            tFLiteObjectDetectionAPIModel.imgData = allocateDirect;
            allocateDirect.order(ByteOrder.nativeOrder());
            int i4 = tFLiteObjectDetectionAPIModel.inputSize;
            tFLiteObjectDetectionAPIModel.intValues = new int[(i4 * i4)];
            tFLiteObjectDetectionAPIModel.tfLite.setNumThreads(4);
            tFLiteObjectDetectionAPIModel.outputLocations = (float[][][]) Array.newInstance(cls, new int[]{1, 10, 4});
            tFLiteObjectDetectionAPIModel.outputClasses = (float[][]) Array.newInstance(cls, new int[]{1, 10});
            tFLiteObjectDetectionAPIModel.outputScores = (float[][]) Array.newInstance(cls, new int[]{1, 10});
            tFLiteObjectDetectionAPIModel.numDetections = new float[1];
            return tFLiteObjectDetectionAPIModel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Classifier.Recognition> recognizeImage(Bitmap bitmap) {
        Class<float> cls = float.class;
        Trace.beginSection("recognizeImage");
        Trace.beginSection("preprocessBitmap");
        bitmap.getPixels(this.intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        this.imgData.rewind();
        for (int i = 0; i < this.inputSize; i++) {
            int i2 = 0;
            while (true) {
                int i3 = this.inputSize;
                if (i2 >= i3) {
                    break;
                }
                int i4 = this.intValues[(i3 * i) + i2];
                if (this.isModelQuantized) {
                    this.imgData.put((byte) ((i4 >> 16) & 255));
                    this.imgData.put((byte) ((i4 >> 8) & 255));
                    this.imgData.put((byte) (i4 & 255));
                } else {
                    this.imgData.putFloat((((float) ((i4 >> 16) & 255)) - 128.0f) / 128.0f);
                    this.imgData.putFloat((((float) ((i4 >> 8) & 255)) - 128.0f) / 128.0f);
                    this.imgData.putFloat((((float) (i4 & 255)) - 128.0f) / 128.0f);
                }
                i2++;
            }
        }
        Trace.endSection();
        Trace.beginSection("feed");
        this.outputLocations = (float[][][]) Array.newInstance(cls, new int[]{1, 10, 4});
        this.outputClasses = (float[][]) Array.newInstance(cls, new int[]{1, 10});
        this.outputScores = (float[][]) Array.newInstance(cls, new int[]{1, 10});
        this.numDetections = new float[1];
        Object[] objArr = {this.imgData};
        HashMap hashMap = new HashMap();
        hashMap.put(0, this.outputLocations);
        hashMap.put(1, this.outputClasses);
        hashMap.put(2, this.outputScores);
        hashMap.put(3, this.numDetections);
        Trace.endSection();
        Trace.beginSection("run");
        this.tfLite.runForMultipleInputsOutputs(objArr, hashMap);
        Trace.endSection();
        int min = Math.min(10, (int) this.numDetections[0]);
        ArrayList arrayList = new ArrayList(min);
        for (int i5 = 0; i5 < min; i5++) {
            float[][][] fArr = this.outputLocations;
            float f = fArr[0][i5][1];
            int i6 = this.inputSize;
            arrayList.add(new Classifier.Recognition(BuildConfig.FLAVOR + i5, this.labels.get(((int) this.outputClasses[0][i5]) + 0), Float.valueOf(this.outputScores[0][i5]), new RectF(f * ((float) i6), fArr[0][i5][0] * ((float) i6), fArr[0][i5][3] * ((float) i6), fArr[0][i5][2] * ((float) i6))));
        }
        Trace.endSection();
        return arrayList;
    }

    public void setNumThreads(int i) {
        Interpreter interpreter = this.tfLite;
        if (interpreter != null) {
            interpreter.setNumThreads(i);
        }
    }

    public void setUseNNAPI(boolean z) {
        Interpreter interpreter = this.tfLite;
        if (interpreter != null) {
            interpreter.setUseNNAPI(z);
        }
    }
}
