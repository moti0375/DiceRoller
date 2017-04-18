package utils;

import android.content.Context;
import android.util.TypedValue;

/**
 * Created by motibartov on 18/04/2017.
 */

public class Utils {

    public static int pixelToDp(int px, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, context.getResources().getDisplayMetrics());

    }
}