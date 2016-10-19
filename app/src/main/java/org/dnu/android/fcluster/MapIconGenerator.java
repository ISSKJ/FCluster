package org.dnu.android.fcluster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.SparseArray;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.dnu.android.testfcluster.R;

import java.util.HashMap;

/**
 * Created by isskj on 16/10/13.
 */
public class MapIconGenerator {

    private static final int[] CLUSTER_IMAGE_PATH = {
            R.mipmap.cluster5000,
            R.mipmap.cluster2500,
            R.mipmap.cluster1000,
            R.mipmap.cluster500,
            R.mipmap.cluster250,
            R.mipmap.cluster100,
            R.mipmap.cluster50,
            R.mipmap.cluster25,
            R.mipmap.cluster
    };
    private static final int[] PIN_IMAGE_PATH = {
            R.mipmap.pin01, R.mipmap.pin02
    };

    private final SparseArray<Bitmap> mCache = new SparseArray<>();
    private final HashMap<String, Bitmap> mCacheFromURL = new HashMap<>();

    public MapIconGenerator(Context context) {
        for (int resID : CLUSTER_IMAGE_PATH) {
            mCache.put(resID, BitmapFactory.decodeResource(context.getResources(), resID));
        }
        for (int resID : PIN_IMAGE_PATH) {
            mCache.put(resID, BitmapFactory.decodeResource(context.getResources(), resID));
        }
    }

    public Bitmap getClusterImage(int num) {
        int resID;
        String text;
        if (num > 5000) {
            text = "5000+";
            resID = R.mipmap.cluster5000;
        } else if (num > 2500) {
            text = "2500+";
            resID = R.mipmap.cluster2500;
        } else if (num > 1000) {
            text = "1000+";
            resID = R.mipmap.cluster1000;
        } else if (num > 500) {
            text = "500+";
            resID = R.mipmap.cluster500;
        } else if (num > 250) {
            text = "250+";
            resID = R.mipmap.cluster250;
        } else if (num > 100) {
            text = "100+";
            resID = R.mipmap.cluster100;
        } else if (num > 50) {
            text = "50+";
            resID = R.mipmap.cluster50;
        } else if (num > 25) {
            text = "25+";
            resID = R.mipmap.cluster25;
        } else {
            text = String.valueOf(num);
            resID = R.mipmap.cluster;
        }
        Bitmap out = mCache.get(resID).copy(Bitmap.Config.ARGB_8888, true);
        final Canvas canvas = new Canvas(out);
        paintDefaultText(canvas, text, 30, 0);
        return out;
    }

    public BitmapDescriptor getClusterImageDescriptor(int num) {
        return BitmapDescriptorFactory.fromBitmap(getClusterImage(num));
    }

    public void put(String url, Bitmap bmp) {
        mCacheFromURL.put(url, bmp);
    }

    public Bitmap get(String url) {
        return mCacheFromURL.get(url);
    }

    public BitmapDescriptor getDescriptor(String url) {
        final Bitmap bmp = get(url);
        if (bmp != null) {
            return BitmapDescriptorFactory.fromBitmap(bmp);
        }
        return getPinImageDescriptor(null);
    }

    public Bitmap getPinImage(String text) {
        Bitmap out = mCache.get(PIN_IMAGE_PATH[0]).copy(Bitmap.Config.ARGB_8888, true);
        final Canvas canvas = new Canvas(out);
        paintDefaultText(canvas, text, 20, 20);
        return out;
    }

    public BitmapDescriptor getPinImageDescriptor(String text) {
        return BitmapDescriptorFactory.fromBitmap(getPinImage(text));
    }

    private void paintDefaultText(Canvas canvas, String text, int fontSize, int offset) {
        int x, y;
        if (!TextUtils.isEmpty(text)) {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextSize(fontSize);
            paint.setFakeBoldText(true);
            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            x = (canvas.getWidth() - bounds.width()) / 2;
            y = ((canvas.getHeight() + bounds.height()) / 2) - offset;

            canvas.drawText(text, x, y, paint);
        }
    }
}
