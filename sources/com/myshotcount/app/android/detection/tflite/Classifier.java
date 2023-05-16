package com.myshotcount.app.android.detection.tflite;

import android.graphics.Bitmap;
import android.graphics.RectF;
import com.myshotcount.app.android.BuildConfig;
import java.util.List;

public interface Classifier {
    void close();

    void enableStatLogging(boolean z);

    String getStatString();

    List<Recognition> recognizeImage(Bitmap bitmap);

    void setNumThreads(int i);

    void setUseNNAPI(boolean z);

    public static class Recognition {
        private final Float confidence;
        private final String id;
        private RectF location;
        private final String title;

        public Recognition(String str, String str2, Float f, RectF rectF) {
            this.id = str;
            this.title = str2;
            this.confidence = f;
            this.location = rectF;
        }

        public String getId() {
            return this.id;
        }

        public String getTitle() {
            return this.title;
        }

        public Float getConfidence() {
            return this.confidence;
        }

        public RectF getLocation() {
            return new RectF(this.location);
        }

        public void setLocation(RectF rectF) {
            this.location = rectF;
        }

        public String toString() {
            String str = this.id;
            String str2 = BuildConfig.FLAVOR;
            if (str != null) {
                str2 = str2 + "[" + this.id + "] ";
            }
            if (this.title != null) {
                str2 = str2 + this.title + " ";
            }
            if (this.confidence != null) {
                str2 = str2 + String.format("(%.1f%%) ", new Object[]{Float.valueOf(this.confidence.floatValue() * 100.0f)});
            }
            if (this.location != null) {
                str2 = str2 + this.location + " ";
            }
            return str2.trim();
        }
    }
}
