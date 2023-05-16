package com.myshotcount.app.android.detection.customview;

import com.myshotcount.app.android.detection.tflite.Classifier;
import java.util.List;

public interface ResultsView {
    void setResults(List<Classifier.Recognition> list);
}
