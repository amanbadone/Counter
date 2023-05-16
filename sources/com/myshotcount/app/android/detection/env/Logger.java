package com.myshotcount.app.android.detection.env;

import android.util.Log;
import java.util.HashSet;
import java.util.Set;

public final class Logger {
    private static final int DEFAULT_MIN_LOG_LEVEL = 3;
    private static final String DEFAULT_TAG = "tensorflow";
    private static final Set<String> IGNORED_CLASS_NAMES;
    private final String messagePrefix;
    private int minLogLevel;
    private final String tag;

    static {
        HashSet hashSet = new HashSet(3);
        IGNORED_CLASS_NAMES = hashSet;
        hashSet.add("dalvik.system.VMStack");
        IGNORED_CLASS_NAMES.add("java.lang.Thread");
        IGNORED_CLASS_NAMES.add(Logger.class.getCanonicalName());
    }

    public Logger(Class<?> cls) {
        this(cls.getSimpleName());
    }

    public Logger(String str) {
        this(DEFAULT_TAG, str);
    }

    public Logger(String str, String str2) {
        this.minLogLevel = 3;
        this.tag = str;
        str2 = str2 == null ? getCallerSimpleName() : str2;
        if (str2.length() > 0) {
            str2 = str2 + ": ";
        }
        this.messagePrefix = str2;
    }

    public Logger() {
        this(DEFAULT_TAG, (String) null);
    }

    public Logger(int i) {
        this(DEFAULT_TAG, (String) null);
        this.minLogLevel = i;
    }

    private static String getCallerSimpleName() {
        for (StackTraceElement className : Thread.currentThread().getStackTrace()) {
            String className2 = className.getClassName();
            if (!IGNORED_CLASS_NAMES.contains(className2)) {
                String[] split = className2.split("\\.");
                return split[split.length - 1];
            }
        }
        return Logger.class.getSimpleName();
    }

    public void setMinLogLevel(int i) {
        this.minLogLevel = i;
    }

    public boolean isLoggable(int i) {
        return i >= this.minLogLevel || Log.isLoggable(this.tag, i);
    }

    private String toMessage(String str, Object... objArr) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.messagePrefix);
        if (objArr.length > 0) {
            str = String.format(str, objArr);
        }
        sb.append(str);
        return sb.toString();
    }

    public void v(String str, Object... objArr) {
        if (isLoggable(2)) {
            Log.v(this.tag, toMessage(str, objArr));
        }
    }

    public void v(Throwable th, String str, Object... objArr) {
        if (isLoggable(2)) {
            Log.v(this.tag, toMessage(str, objArr), th);
        }
    }

    public void d(String str, Object... objArr) {
        if (isLoggable(3)) {
            Log.d(this.tag, toMessage(str, objArr));
        }
    }

    public void d(Throwable th, String str, Object... objArr) {
        if (isLoggable(3)) {
            Log.d(this.tag, toMessage(str, objArr), th);
        }
    }

    public void i(String str, Object... objArr) {
        if (isLoggable(4)) {
            Log.i(this.tag, toMessage(str, objArr));
        }
    }

    public void i(Throwable th, String str, Object... objArr) {
        if (isLoggable(4)) {
            Log.i(this.tag, toMessage(str, objArr), th);
        }
    }

    public void w(String str, Object... objArr) {
        if (isLoggable(5)) {
            Log.w(this.tag, toMessage(str, objArr));
        }
    }

    public void w(Throwable th, String str, Object... objArr) {
        if (isLoggable(5)) {
            Log.w(this.tag, toMessage(str, objArr), th);
        }
    }

    public void e(String str, Object... objArr) {
        if (isLoggable(6)) {
            Log.e(this.tag, toMessage(str, objArr));
        }
    }

    public void e(Throwable th, String str, Object... objArr) {
        if (isLoggable(6)) {
            Log.e(this.tag, toMessage(str, objArr), th);
        }
    }
}
