package com.ticketmaster.mobilestudio.gridviewpager;

import android.graphics.drawable.Drawable;

public interface GridPageOptions {
    Drawable getBackground();

    void setBackgroundListener(BackgroundListener var1);

    interface BackgroundListener {
        void notifyBackgroundChanged();
    }
}
