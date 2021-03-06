package com.ticketmaster.mobilestudio.gridviewpager;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

public abstract class FragmentGridPagerAdapter extends GridPagerAdapter {
    private static final int MAX_ROWS = 65535;
    private final FragmentManager mFragmentManager;
    private final Map<String, Point> mFragmentPositions;
    private final Map<Point, String> mFragmentTags;
    private FragmentTransaction mCurTransaction;

    public FragmentGridPagerAdapter(FragmentManager fm) {
        this.mFragmentManager = fm;
        this.mFragmentPositions = new HashMap();
        this.mFragmentTags = new HashMap();
    }

    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }

    public abstract Fragment getFragment(int var1, int var2);

    public long getFragmentId(int row, int column) {
        return (long)(column * '\uffff' + row);
    }

    public Fragment instantiateItem(ViewGroup container, int row, int column) {
        if(this.mCurTransaction == null) {
            this.mCurTransaction = this.mFragmentManager.beginTransaction();
        }

        long itemId = this.getFragmentId(row, column);
        String tag = makeFragmentName(container.getId(), itemId);
        Fragment fragment = this.mFragmentManager.findFragmentByTag(tag);
        if(fragment == null) {
            fragment = this.getFragment(row, column);
            this.mCurTransaction.add(container.getId(), fragment, tag);
        } else {
            this.restoreFragment(fragment, this.mCurTransaction);
        }

        Point position = new Point(column, row);
        this.mFragmentTags.put(position, tag);
        this.mFragmentPositions.put(tag, position);

        return fragment;
    }

    protected void restoreFragment(Fragment fragment, FragmentTransaction transaction) {
    }

    public boolean isViewFromObject(View view, Object object) {
        return view == ((Fragment)object).getView();
    }

    public void destroyItem(ViewGroup container, int row, int column, Object object) {
        if(this.mCurTransaction == null) {
            this.mCurTransaction = this.mFragmentManager.beginTransaction();
        }

        Fragment fragment = (Fragment)object;

        this.removeFragment(fragment, this.mCurTransaction);
    }

    protected void removeFragment(Fragment fragment, FragmentTransaction transaction) {
        transaction.remove(fragment);
    }

    protected void applyItemPosition(Object object, Point position) {
        if(position != GridPagerAdapter.POSITION_UNCHANGED) {
            Fragment fragment = (Fragment)object;
            if(fragment.getTag().equals(this.mFragmentTags.get(position))) {
                this.mFragmentTags.remove(position);
            }

            if(position == GridPagerAdapter.POSITION_NONE) {
                this.mFragmentPositions.remove(fragment.getTag());
            } else {
                this.mFragmentPositions.put(fragment.getTag(), position);
                this.mFragmentTags.put(position, fragment.getTag());
            }

        }
    }

    public final Drawable getFragmentBackground(int row, int column) {
        String tag = this.mFragmentTags.get(new Point(column, row));
        Fragment f = this.mFragmentManager.findFragmentByTag(tag);
        Drawable bg = GridPagerAdapter.BACKGROUND_NONE;

        return bg;
    }

    public Drawable getBackgroundForPage(int row, int column) {
        return this.getFragmentBackground(row, column);
    }

    public void finishUpdate(ViewGroup container) {
        if(VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1 && this.mFragmentManager.isDestroyed()) {
            this.mCurTransaction = null;
        } else {
            if(this.mCurTransaction != null) {
                this.mCurTransaction.commitAllowingStateLoss();
                this.mCurTransaction = null;
                this.mFragmentManager.executePendingTransactions();
            }

        }
    }

    public Fragment findExistingFragment(int row, int column) {
        String tag = (String)this.mFragmentTags.get(new Point(column, row));
        return tag != null?this.mFragmentManager.findFragmentByTag(tag):null;
    }
}
