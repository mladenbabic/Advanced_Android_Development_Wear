package com.example.mladen.wear.util;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mladen on 4/26/2015.
 */
public class BitmapUtil {


    public static Bitmap getResizedBitmap(Bitmap bitmap, int i, int j) {
        int k = bitmap.getWidth();
        int l = bitmap.getHeight();
        float f = (float) i / (float) k;
        float f1 = (float) j / (float) l;
        Matrix matrix = new Matrix();
        matrix.postScale(f, f1);
        return Bitmap.createBitmap(bitmap, 0, 0, k, l, matrix, false);
    }


    public static Bitmap getBitmap(
            Drawable drawable, boolean scaleBitmap, int width, int height) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        if (scaleBitmap) {
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        }
        return bitmap;
    }


    public static void scaleBitmaps(Map<Integer, Bitmap> bitmapMap, float scale) {
        for (Integer key: bitmapMap.keySet()) {
            Bitmap bitmap = bitmapMap.get(key);
            bitmapMap.put(key, scaleBitmap(bitmap, scale));
        }
    }



    public static void scaleBitmaps(SparseArray<Bitmap> bitmapMap, float scale) {
        for (int i = 0; i < bitmapMap.size(); i++) {
            int key = bitmapMap.keyAt(i);
            Log.d("BitmapUtil", "scaleBitmaps: " + key);
            Bitmap bitmap = bitmapMap.get(key);
            bitmapMap.put(i, scaleBitmap(bitmap, scale));
        }
    }


    public static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        int width = (int) ((float) bitmap.getWidth() * scale);
        int height = (int) ((float) bitmap.getHeight() * scale);
        if (bitmap.getWidth() != width
                || bitmap.getHeight() != height) {
            return Bitmap.createScaledBitmap(bitmap,
                    width, height, true /* filter */);
        } else {
            return bitmap;
        }
    }

    public static int[] getIntArray(int resId, Resources mResources) {
        TypedArray array = mResources.obtainTypedArray(resId);
        int[] rc = new int[array.length()];
        TypedValue value = new TypedValue();
        for (int i = 0; i < array.length(); i++) {
            array.getValue(i, value);
            rc[i] = value.resourceId;
        }
        return rc;
    }


    public static Map<Integer, Bitmap> loadBitmaps(int arrayId, Resources resources) {
        Map<Integer, Bitmap> bitmapsMaps = new HashMap<>();
        int[] bitmapIds = getIntArray(arrayId, resources);
        for (int i = 0; i < bitmapIds.length; i++) {
            Drawable backgroundDrawable = resources.getDrawable(bitmapIds[i]);
            Bitmap bitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            bitmapsMaps.put(bitmapIds[i], bitmap);
        }
        return bitmapsMaps;
    }

    public static void recycleBitmaps(SparseArray<Bitmap> bitmapSparseArray) {
        for (int i = 0; i < bitmapSparseArray.size(); i++) {
            Bitmap mBitmap = bitmapSparseArray.get(i);
            if (mBitmap != null) {
                mBitmap.recycle();
                mBitmap = null;
            }
        }
    }

}
