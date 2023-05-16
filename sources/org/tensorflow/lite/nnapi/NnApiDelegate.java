package org.tensorflow.lite.nnapi;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.TensorFlowLite;

public class NnApiDelegate implements Delegate, AutoCloseable {
    private static final long INVALID_DELEGATE_HANDLE = 0;
    private long delegateHandle;

    private static native long createDelegate(int i, String str, String str2, String str3, int i2, boolean z, boolean z2);

    private static native void deleteDelegate(long j);

    private static native int getNnapiErrno(long j);

    public static final class Options {
        public static final int EXECUTION_PREFERENCE_FAST_SINGLE_ANSWER = 1;
        public static final int EXECUTION_PREFERENCE_LOW_POWER = 0;
        public static final int EXECUTION_PREFERENCE_SUSTAINED_SPEED = 2;
        public static final int EXECUTION_PREFERENCE_UNDEFINED = -1;
        /* access modifiers changed from: private */
        public String acceleratorName = null;
        /* access modifiers changed from: private */
        public String cacheDir = null;
        /* access modifiers changed from: private */
        public int executionPreference = -1;
        /* access modifiers changed from: private */
        public Integer maxDelegatedPartitions = null;
        /* access modifiers changed from: private */
        public String modelToken = null;
        /* access modifiers changed from: private */
        public Boolean useNnapiCpu = null;

        public Options setExecutionPreference(int i) {
            this.executionPreference = i;
            return this;
        }

        public Options setAcceleratorName(String str) {
            this.acceleratorName = str;
            return this;
        }

        public Options setCacheDir(String str) {
            this.cacheDir = str;
            return this;
        }

        public Options setModelToken(String str) {
            this.modelToken = str;
            return this;
        }

        public Options setMaxNumberOfDelegatedPartitions(int i) {
            this.maxDelegatedPartitions = Integer.valueOf(i);
            return this;
        }

        public Options setUseNnapiCpu(boolean z) {
            this.useNnapiCpu = Boolean.valueOf(!z);
            return this;
        }
    }

    public NnApiDelegate(Options options) {
        TensorFlowLite.init();
        boolean z = true;
        this.delegateHandle = createDelegate(options.executionPreference, options.acceleratorName, options.cacheDir, options.modelToken, options.maxDelegatedPartitions != null ? options.maxDelegatedPartitions.intValue() : -1, options.useNnapiCpu != null, (options.useNnapiCpu == null || options.useNnapiCpu.booleanValue()) ? false : z);
    }

    public NnApiDelegate() {
        this(new Options());
    }

    public long getNativeHandle() {
        return this.delegateHandle;
    }

    public void close() {
        long j = this.delegateHandle;
        if (j != 0) {
            deleteDelegate(j);
            this.delegateHandle = 0;
        }
    }

    public int getNnapiErrno() {
        checkNotClosed();
        return getNnapiErrno(this.delegateHandle);
    }

    public boolean hasErrors() {
        return getNnapiErrno(this.delegateHandle) != 0;
    }

    private void checkNotClosed() {
        if (this.delegateHandle == 0) {
            throw new IllegalStateException("Should not access delegate after it has been closed.");
        }
    }
}
