package org.tensorflow.lite;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;

final class NativeInterpreterWrapper implements AutoCloseable {
    private static final int ERROR_BUFFER_SIZE = 512;
    private final List<Delegate> delegates;
    private long errorHandle;
    private long inferenceDurationNanoseconds;
    private Tensor[] inputTensors;
    private Map<String, Integer> inputsIndexes;
    private long interpreterHandle;
    private boolean isMemoryAllocated;
    private ByteBuffer modelByteBuffer;
    private long modelHandle;
    private Tensor[] outputTensors;
    private Map<String, Integer> outputsIndexes;
    private final List<AutoCloseable> ownedDelegates;

    private static native long allocateTensors(long j, long j2);

    private static native void allowBufferHandleOutput(long j, boolean z);

    private static native void allowFp16PrecisionForFp32(long j, boolean z);

    private static native void applyDelegate(long j, long j2, long j3);

    private static native long createErrorReporter(int i);

    private static native long createInterpreter(long j, long j2, int i);

    private static native long createModel(String str, long j);

    private static native long createModelWithBuffer(ByteBuffer byteBuffer, long j);

    private static native void delete(long j, long j2, long j3);

    private static native int getExecutionPlanLength(long j);

    private static native int getInputCount(long j);

    private static native String[] getInputNames(long j);

    private static native int getInputTensorIndex(long j, int i);

    private static native int getOutputCount(long j);

    private static native int getOutputDataType(long j, int i);

    private static native String[] getOutputNames(long j);

    private static native int getOutputTensorIndex(long j, int i);

    private static native boolean hasUnresolvedFlexOp(long j);

    private static native void numThreads(long j, int i);

    private static native void resetVariableTensors(long j, long j2);

    private static native boolean resizeInput(long j, long j2, int i, int[] iArr, boolean z);

    private static native void run(long j, long j2);

    private static native void useNNAPI(long j, boolean z);

    NativeInterpreterWrapper(String str) {
        this(str, (Interpreter.Options) null);
    }

    NativeInterpreterWrapper(ByteBuffer byteBuffer) {
        this(byteBuffer, (Interpreter.Options) null);
    }

    NativeInterpreterWrapper(String str, Interpreter.Options options) {
        this.inferenceDurationNanoseconds = -1;
        this.isMemoryAllocated = false;
        this.delegates = new ArrayList();
        this.ownedDelegates = new ArrayList();
        TensorFlowLite.init();
        long createErrorReporter = createErrorReporter(512);
        init(createErrorReporter, createModel(str, createErrorReporter), options);
    }

    NativeInterpreterWrapper(ByteBuffer byteBuffer, Interpreter.Options options) {
        this.inferenceDurationNanoseconds = -1;
        this.isMemoryAllocated = false;
        this.delegates = new ArrayList();
        this.ownedDelegates = new ArrayList();
        TensorFlowLite.init();
        if (byteBuffer == null || (!(byteBuffer instanceof MappedByteBuffer) && (!byteBuffer.isDirect() || byteBuffer.order() != ByteOrder.nativeOrder()))) {
            throw new IllegalArgumentException("Model ByteBuffer should be either a MappedByteBuffer of the model file, or a direct ByteBuffer using ByteOrder.nativeOrder() which contains bytes of model content.");
        }
        this.modelByteBuffer = byteBuffer;
        long createErrorReporter = createErrorReporter(512);
        init(createErrorReporter, createModelWithBuffer(this.modelByteBuffer, createErrorReporter), options);
    }

    private void init(long j, long j2, Interpreter.Options options) {
        if (options == null) {
            options = new Interpreter.Options();
        }
        this.errorHandle = j;
        this.modelHandle = j2;
        long createInterpreter = createInterpreter(j2, j, options.numThreads);
        this.interpreterHandle = createInterpreter;
        this.inputTensors = new Tensor[getInputCount(createInterpreter)];
        this.outputTensors = new Tensor[getOutputCount(this.interpreterHandle)];
        if (options.allowFp16PrecisionForFp32 != null) {
            allowFp16PrecisionForFp32(this.interpreterHandle, options.allowFp16PrecisionForFp32.booleanValue());
        }
        if (options.allowBufferHandleOutput != null) {
            allowBufferHandleOutput(this.interpreterHandle, options.allowBufferHandleOutput.booleanValue());
        }
        applyDelegates(options);
        allocateTensors(this.interpreterHandle, j);
        this.isMemoryAllocated = true;
    }

    public void close() {
        int i = 0;
        while (true) {
            Tensor[] tensorArr = this.inputTensors;
            if (i >= tensorArr.length) {
                break;
            }
            if (tensorArr[i] != null) {
                tensorArr[i].close();
                this.inputTensors[i] = null;
            }
            i++;
        }
        int i2 = 0;
        while (true) {
            Tensor[] tensorArr2 = this.outputTensors;
            if (i2 >= tensorArr2.length) {
                break;
            }
            if (tensorArr2[i2] != null) {
                tensorArr2[i2].close();
                this.outputTensors[i2] = null;
            }
            i2++;
        }
        delete(this.errorHandle, this.modelHandle, this.interpreterHandle);
        this.errorHandle = 0;
        this.modelHandle = 0;
        this.interpreterHandle = 0;
        this.modelByteBuffer = null;
        this.inputsIndexes = null;
        this.outputsIndexes = null;
        this.isMemoryAllocated = false;
        this.delegates.clear();
        for (AutoCloseable close : this.ownedDelegates) {
            try {
                close.close();
            } catch (Exception e) {
                System.err.println("Failed to close flex delegate: " + e);
            }
        }
        this.ownedDelegates.clear();
    }

    /* access modifiers changed from: package-private */
    public void run(Object[] objArr, Map<Integer, Object> map) {
        this.inferenceDurationNanoseconds = -1;
        if (objArr == null || objArr.length == 0) {
            throw new IllegalArgumentException("Input error: Inputs should not be null or empty.");
        } else if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException("Input error: Outputs should not be null or empty.");
        } else {
            int i = 0;
            for (int i2 = 0; i2 < objArr.length; i2++) {
                int[] inputShapeIfDifferent = getInputTensor(i2).getInputShapeIfDifferent(objArr[i2]);
                if (inputShapeIfDifferent != null) {
                    resizeInput(i2, inputShapeIfDifferent);
                }
            }
            boolean z = !this.isMemoryAllocated;
            if (z) {
                allocateTensors(this.interpreterHandle, this.errorHandle);
                this.isMemoryAllocated = true;
            }
            for (int i3 = 0; i3 < objArr.length; i3++) {
                getInputTensor(i3).setTo(objArr[i3]);
            }
            long nanoTime = System.nanoTime();
            run(this.interpreterHandle, this.errorHandle);
            long nanoTime2 = System.nanoTime() - nanoTime;
            if (z) {
                while (true) {
                    Tensor[] tensorArr = this.outputTensors;
                    if (i >= tensorArr.length) {
                        break;
                    }
                    if (tensorArr[i] != null) {
                        tensorArr[i].refreshShape();
                    }
                    i++;
                }
            }
            for (Map.Entry next : map.entrySet()) {
                getOutputTensor(((Integer) next.getKey()).intValue()).copyTo(next.getValue());
            }
            this.inferenceDurationNanoseconds = nanoTime2;
        }
    }

    /* access modifiers changed from: package-private */
    public void resizeInput(int i, int[] iArr) {
        resizeInput(i, iArr, false);
    }

    /* access modifiers changed from: package-private */
    public void resizeInput(int i, int[] iArr, boolean z) {
        if (resizeInput(this.interpreterHandle, this.errorHandle, i, iArr, z)) {
            this.isMemoryAllocated = false;
            Tensor[] tensorArr = this.inputTensors;
            if (tensorArr[i] != null) {
                tensorArr[i].refreshShape();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void allocateTensors() {
        if (!this.isMemoryAllocated) {
            this.isMemoryAllocated = true;
            allocateTensors(this.interpreterHandle, this.errorHandle);
            int i = 0;
            while (true) {
                Tensor[] tensorArr = this.outputTensors;
                if (i < tensorArr.length) {
                    if (tensorArr[i] != null) {
                        tensorArr[i].refreshShape();
                    }
                    i++;
                } else {
                    return;
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setUseNNAPI(boolean z) {
        useNNAPI(this.interpreterHandle, z);
    }

    /* access modifiers changed from: package-private */
    public void setNumThreads(int i) {
        numThreads(this.interpreterHandle, i);
    }

    /* access modifiers changed from: package-private */
    public void modifyGraphWithDelegate(Delegate delegate) {
        applyDelegate(this.interpreterHandle, this.errorHandle, delegate.getNativeHandle());
        this.delegates.add(delegate);
    }

    /* access modifiers changed from: package-private */
    public void resetVariableTensors() {
        resetVariableTensors(this.interpreterHandle, this.errorHandle);
    }

    /* access modifiers changed from: package-private */
    public int getInputIndex(String str) {
        if (this.inputsIndexes == null) {
            String[] inputNames = getInputNames(this.interpreterHandle);
            this.inputsIndexes = new HashMap();
            if (inputNames != null) {
                for (int i = 0; i < inputNames.length; i++) {
                    this.inputsIndexes.put(inputNames[i], Integer.valueOf(i));
                }
            }
        }
        if (this.inputsIndexes.containsKey(str)) {
            return this.inputsIndexes.get(str).intValue();
        }
        throw new IllegalArgumentException(String.format("Input error: '%s' is not a valid name for any input. Names of inputs and their indexes are %s", new Object[]{str, this.inputsIndexes.toString()}));
    }

    /* access modifiers changed from: package-private */
    public int getOutputIndex(String str) {
        if (this.outputsIndexes == null) {
            String[] outputNames = getOutputNames(this.interpreterHandle);
            this.outputsIndexes = new HashMap();
            if (outputNames != null) {
                for (int i = 0; i < outputNames.length; i++) {
                    this.outputsIndexes.put(outputNames[i], Integer.valueOf(i));
                }
            }
        }
        if (this.outputsIndexes.containsKey(str)) {
            return this.outputsIndexes.get(str).intValue();
        }
        throw new IllegalArgumentException(String.format("Input error: '%s' is not a valid name for any output. Names of outputs and their indexes are %s", new Object[]{str, this.outputsIndexes.toString()}));
    }

    /* access modifiers changed from: package-private */
    public Long getLastNativeInferenceDurationNanoseconds() {
        long j = this.inferenceDurationNanoseconds;
        if (j < 0) {
            return null;
        }
        return Long.valueOf(j);
    }

    /* access modifiers changed from: package-private */
    public int getInputTensorCount() {
        return this.inputTensors.length;
    }

    /* access modifiers changed from: package-private */
    public Tensor getInputTensor(int i) {
        if (i >= 0) {
            Tensor[] tensorArr = this.inputTensors;
            if (i < tensorArr.length) {
                Tensor tensor = tensorArr[i];
                if (tensor != null) {
                    return tensor;
                }
                long j = this.interpreterHandle;
                Tensor fromIndex = Tensor.fromIndex(j, getInputTensorIndex(j, i));
                tensorArr[i] = fromIndex;
                return fromIndex;
            }
        }
        throw new IllegalArgumentException("Invalid input Tensor index: " + i);
    }

    /* access modifiers changed from: package-private */
    public int getOutputTensorCount() {
        return this.outputTensors.length;
    }

    /* access modifiers changed from: package-private */
    public Tensor getOutputTensor(int i) {
        if (i >= 0) {
            Tensor[] tensorArr = this.outputTensors;
            if (i < tensorArr.length) {
                Tensor tensor = tensorArr[i];
                if (tensor != null) {
                    return tensor;
                }
                long j = this.interpreterHandle;
                Tensor fromIndex = Tensor.fromIndex(j, getOutputTensorIndex(j, i));
                tensorArr[i] = fromIndex;
                return fromIndex;
            }
        }
        throw new IllegalArgumentException("Invalid output Tensor index: " + i);
    }

    /* access modifiers changed from: package-private */
    public int getExecutionPlanLength() {
        return getExecutionPlanLength(this.interpreterHandle);
    }

    private void applyDelegates(Interpreter.Options options) {
        Delegate maybeCreateFlexDelegate;
        boolean hasUnresolvedFlexOp = hasUnresolvedFlexOp(this.interpreterHandle);
        if (hasUnresolvedFlexOp && (maybeCreateFlexDelegate = maybeCreateFlexDelegate(options.delegates)) != null) {
            this.ownedDelegates.add((AutoCloseable) maybeCreateFlexDelegate);
            applyDelegate(this.interpreterHandle, this.errorHandle, maybeCreateFlexDelegate.getNativeHandle());
        }
        try {
            for (Delegate next : options.delegates) {
                applyDelegate(this.interpreterHandle, this.errorHandle, next.getNativeHandle());
                this.delegates.add(next);
            }
            if (options.useNNAPI != null && options.useNNAPI.booleanValue()) {
                NnApiDelegate nnApiDelegate = new NnApiDelegate();
                this.ownedDelegates.add(nnApiDelegate);
                applyDelegate(this.interpreterHandle, this.errorHandle, nnApiDelegate.getNativeHandle());
            }
        } catch (IllegalArgumentException e) {
            if (hasUnresolvedFlexOp && !hasUnresolvedFlexOp(this.interpreterHandle)) {
                PrintStream printStream = System.err;
                printStream.println("Ignoring failed delegate application: " + e);
                return;
            }
            throw e;
        }
    }

    private static Delegate maybeCreateFlexDelegate(List<Delegate> list) {
        try {
            Class<?> cls = Class.forName("org.tensorflow.lite.flex.FlexDelegate");
            for (Delegate isInstance : list) {
                if (cls.isInstance(isInstance)) {
                    return null;
                }
            }
            return (Delegate) cls.getConstructor(new Class[0]).newInstance(new Object[0]);
        } catch (Exception unused) {
            return null;
        }
    }
}
