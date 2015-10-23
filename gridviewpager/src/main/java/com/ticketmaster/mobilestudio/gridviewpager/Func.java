package com.ticketmaster.mobilestudio.gridviewpager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.view.View;

@TargetApi(20)
class Func {
    Func() {
    }

    static float clamp(float value, int min, int max) {
        return value < (float)min?(float)min:(value > (float)max?(float)max:value);
    }

    static int clamp(int value, int min, int max) {
        return value < min?min:(value > max?max:value);
    }

    static boolean getWindowOverscan(View v) {
        Context ctx = v.getContext();
        boolean res = false;
        if(ctx instanceof Activity) {
            Activity act = (Activity)ctx;
            int windowFlags = act.getWindow().getAttributes().flags;
            res = (windowFlags & 33554432) != 0;
        }

        return res;
    }
}
