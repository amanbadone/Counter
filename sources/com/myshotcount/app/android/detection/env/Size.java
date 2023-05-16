package com.myshotcount.app.android.detection.env;

import android.graphics.Bitmap;
import android.text.TextUtils;
import com.myshotcount.app.android.BuildConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Size implements Comparable<Size>, Serializable {
    public static final long serialVersionUID = 7689808733290872361L;
    public final int height;
    public final int width;

    public Size(int i, int i2) {
        this.width = i;
        this.height = i2;
    }

    public Size(Bitmap bitmap) {
        this.width = bitmap.getWidth();
        this.height = bitmap.getHeight();
    }

    public static Size getRotatedSize(Size size, int i) {
        return i % 180 != 0 ? new Size(size.height, size.width) : size;
    }

    public static Size parseFromString(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String[] split = str.trim().split("x");
        if (split.length == 2) {
            try {
                return new Size(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            } catch (NumberFormatException unused) {
            }
        }
        return null;
    }

    public static List<Size> sizeStringToList(String str) {
        ArrayList arrayList = new ArrayList();
        if (str != null) {
            for (String parseFromString : str.split(",")) {
                Size parseFromString2 = parseFromString(parseFromString);
                if (parseFromString2 != null) {
                    arrayList.add(parseFromString2);
                }
            }
        }
        return arrayList;
    }

    public static String sizeListToString(List<Size> list) {
        if (list == null || list.size() <= 0) {
            return BuildConfig.FLAVOR;
        }
        String size = list.get(0).toString();
        for (int i = 1; i < list.size(); i++) {
            size = size + "," + list.get(i).toString();
        }
        return size;
    }

    public static final String dimensionsAsString(int i, int i2) {
        return i + "x" + i2;
    }

    public final float aspectRatio() {
        return ((float) this.width) / ((float) this.height);
    }

    public int compareTo(Size size) {
        return (this.width * this.height) - (size.width * size.height);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Size)) {
            return false;
        }
        Size size = (Size) obj;
        if (this.width == size.width && this.height == size.height) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.width * 32713) + this.height;
    }

    public String toString() {
        return dimensionsAsString(this.width, this.height);
    }
}
