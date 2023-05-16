package com.myshotcount.app.android.detection.env;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import androidx.core.view.MotionEventCompat;
import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {
    private static final Logger LOGGER = new Logger();
    static final int kMaxChannelValue = 262143;

    private static int YUV2RGB(int i, int i2, int i3) {
        int i4 = i - 16;
        int i5 = 0;
        if (i4 < 0) {
            i4 = 0;
        }
        int i6 = i2 - 128;
        int i7 = i3 - 128;
        int i8 = i4 * 1192;
        int i9 = (i7 * 1634) + i8;
        int i10 = (i8 - (i7 * 833)) - (i6 * 400);
        int i11 = i8 + (i6 * 2066);
        if (i9 > kMaxChannelValue) {
            i9 = kMaxChannelValue;
        } else if (i9 < 0) {
            i9 = 0;
        }
        if (i10 > kMaxChannelValue) {
            i10 = kMaxChannelValue;
        } else if (i10 < 0) {
            i10 = 0;
        }
        if (i11 > kMaxChannelValue) {
            i5 = kMaxChannelValue;
        } else if (i11 >= 0) {
            i5 = i11;
        }
        return -16777216 | ((i9 << 6) & 16711680) | ((i10 >> 2) & MotionEventCompat.ACTION_POINTER_INDEX_MASK) | ((i5 >> 10) & 255);
    }

    public static int getYUVByteSize(int i, int i2) {
        return (i * i2) + (((i + 1) / 2) * ((i2 + 1) / 2) * 2);
    }

    public static void saveBitmap(Bitmap bitmap) {
        saveBitmap(bitmap, "preview.png");
    }

    public static void saveBitmap(Bitmap bitmap, String str) {
        String str2 = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow";
        LOGGER.i("Saving %dx%d bitmap to %s.", Integer.valueOf(bitmap.getWidth()), Integer.valueOf(bitmap.getHeight()), str2);
        File file = new File(str2);
        if (!file.mkdirs()) {
            LOGGER.i("Make dir failed", new Object[0]);
        }
        File file2 = new File(file, str);
        if (file2.exists()) {
            file2.delete();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            bitmap.compress(Bitmap.CompressFormat.PNG, 99, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            LOGGER.e(e, "Exception!", new Object[0]);
        }
    }

    public static void convertYUV420SPToARGB8888(byte[] bArr, int i, int i2, int[] iArr) {
        int i3 = i * i2;
        int i4 = 0;
        for (int i5 = 0; i5 < i2; i5++) {
            int i6 = ((i5 >> 1) * i) + i3;
            int i7 = 0;
            byte b = 0;
            byte b2 = 0;
            while (i7 < i) {
                byte b3 = bArr[i4] & 255;
                if ((i7 & 1) == 0) {
                    int i8 = i6 + 1;
                    b2 = bArr[i6] & 255;
                    i6 = i8 + 1;
                    b = bArr[i8] & 255;
                }
                iArr[i4] = YUV2RGB(b3, b, b2);
                i7++;
                i4++;
            }
        }
    }

    public static void convertYUV420ToARGB8888(byte[] bArr, byte[] bArr2, byte[] bArr3, int i, int i2, int i3, int i4, int i5, int[] iArr) {
        int i6 = i2;
        int i7 = 0;
        for (int i8 = 0; i8 < i6; i8++) {
            int i9 = i3 * i8;
            int i10 = (i8 >> 1) * i4;
            int i11 = i;
            int i12 = 0;
            while (i12 < i11) {
                int i13 = ((i12 >> 1) * i5) + i10;
                iArr[i7] = YUV2RGB(bArr[i9 + i12] & 255, bArr2[i13] & 255, bArr3[i13] & 255);
                i12++;
                i7++;
            }
        }
    }

    public static Matrix getTransformationMatrix(int i, int i2, int i3, int i4, int i5, boolean z) {
        Matrix matrix = new Matrix();
        boolean z2 = true;
        if (i5 != 0) {
            if (i5 % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", Integer.valueOf(i5));
            }
            matrix.postTranslate(((float) (-i)) / 2.0f, ((float) (-i2)) / 2.0f);
            matrix.postRotate((float) i5);
        }
        if ((Math.abs(i5) + 90) % 180 != 0) {
            z2 = false;
        }
        int i6 = z2 ? i2 : i;
        if (!z2) {
            i = i2;
        }
        if (!(i6 == i3 && i == i4)) {
            float f = ((float) i3) / ((float) i6);
            float f2 = ((float) i4) / ((float) i);
            if (z) {
                float max = Math.max(f, f2);
                matrix.postScale(max, max);
            } else {
                matrix.postScale(f, f2);
            }
        }
        if (i5 != 0) {
            matrix.postTranslate(((float) i3) / 2.0f, ((float) i4) / 2.0f);
        }
        return matrix;
    }
}
