package com.nextos.launcher.cache;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

/**
 * LRU icon cache. Icons are rasterized once at the grid size to keep RAM low
 * and avoid repeated PackageManager binder calls while scrolling.
 */
public final class IconCache {

    private static volatile IconCache instance;

    public static IconCache get(Context context) {
        if (instance == null) {
            synchronized (IconCache.class) {
                if (instance == null) {
                    instance = new IconCache(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private final Context app;
    private final PackageManager pm;
    private final LruCache<String, Bitmap> cache;
    private final int sizePx;
    private final float cornerPx;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private IconCache(Context context) {
        this.app = context;
        this.pm = context.getPackageManager();
        int memKb = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int maxKb = Math.max(2048, memKb / 10);
        this.cache = new LruCache<String, Bitmap>(maxKb) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
        this.sizePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60f, context.getResources().getDisplayMetrics()));
        this.cornerPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16f, context.getResources().getDisplayMetrics());
    }

    public Drawable load(String packageName, @Nullable Integer plateRes) {
        Bitmap bmp = getBitmap(packageName, plateRes);
        RoundedBitmapDrawable rd = RoundedBitmapDrawableFactory.create(app.getResources(), bmp);
        rd.setCornerRadius(cornerPx);
        rd.setAntiAlias(true);
        return rd;
    }

    public Bitmap getBitmap(String packageName, @Nullable Integer plateRes) {
        String key = packageName + "|" + (plateRes == null ? 0 : plateRes);
        Bitmap hit = cache.get(key);
        if (hit != null && !hit.isRecycled()) {
            return hit;
        }
        Bitmap made = render(packageName, plateRes);
        cache.put(key, made);
        return made;
    }

    public Drawable fromVector(int vectorRes, int plateRes) {
        String key = "vec|" + vectorRes + "|" + plateRes;
        Bitmap hit = cache.get(key);
        if (hit == null || hit.isRecycled()) {
            hit = renderVector(vectorRes, plateRes);
            cache.put(key, hit);
        }
        RoundedBitmapDrawable rd = RoundedBitmapDrawableFactory.create(app.getResources(), hit);
        rd.setCornerRadius(cornerPx);
        rd.setAntiAlias(true);
        return rd;
    }

    public void evictPackage(String packageName) {
        // Snapshot keys then remove — LruCache has no key prefix API.
        for (String k : cache.snapshot().keySet()) {
            if (k.startsWith(packageName + "|")) {
                cache.remove(k);
            }
        }
    }

    public void clear() {
        cache.evictAll();
    }

    public int sizeKb() {
        return cache.size();
    }

    private Bitmap render(String packageName, @Nullable Integer plateRes) {
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        if (plateRes != null && plateRes != 0) {
            Drawable plate = ContextCompat.getDrawable(app, plateRes);
            if (plate != null) {
                plate.setBounds(0, 0, sizePx, sizePx);
                plate.draw(c);
            }
        }
        Drawable d;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            d = pm.getApplicationIcon(ai);
        } catch (Exception e) {
            d = ContextCompat.getDrawable(app, android.R.drawable.sym_def_app_icon);
        }
        if (d != null) {
            int inset = plateRes != null && plateRes != 0 ? Math.round(sizePx * 0.18f) : 0;
            d.setBounds(inset, inset, sizePx - inset, sizePx - inset);
            d.draw(c);
        }
        return out;
    }

    private Bitmap renderVector(int vectorRes, int plateRes) {
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Drawable plate = ContextCompat.getDrawable(app, plateRes);
        if (plate != null) {
            plate.setBounds(0, 0, sizePx, sizePx);
            plate.draw(c);
        }
        Drawable icon = ContextCompat.getDrawable(app, vectorRes);
        if (icon != null) {
            int inset = Math.round(sizePx * 0.22f);
            icon.setBounds(inset, inset, sizePx - inset, sizePx - inset);
            icon.draw(c);
        }
        return out;
    }

    public static Bitmap round(Bitmap src, float radius) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF r = new RectF(0, 0, src.getWidth(), src.getHeight());
        c.drawRoundRect(r, radius, radius, p);
        p.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        c.drawBitmap(src, 0, 0, p);
        return out;
    }

    public static Bitmap drawableToBitmap(Drawable d, int w, int h) {
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (b != null) {
                return Bitmap.createScaledBitmap(b, w, h, true);
            }
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        d.setBounds(0, 0, w, h);
        d.draw(c);
        return out;
    }
}
