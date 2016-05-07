package com.example.chris.leafloadingview;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by dimion on 16/5/5.
 */
public class Utils {

    public static int getScreenWIdthPixels(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int dipToPx(Context context, int dip) {
        return (int)(dip * getScreenWIdthPixels(context) + 0.5f);
    }

    public static float getScreenDensity(Context context) {
        try {
            DisplayMetrics dm = new DisplayMetrics();
            ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getMetrics(dm);
            return dm.density;
        } catch (Exception e) {
            e.printStackTrace();
            return DisplayMetrics.DENSITY_DEFAULT;
        }
    }

}
