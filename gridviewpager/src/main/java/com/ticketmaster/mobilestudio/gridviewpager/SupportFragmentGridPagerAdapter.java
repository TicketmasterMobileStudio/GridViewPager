package com.ticketmaster.mobilestudio.gridviewpager;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;

import com.ticketmaster.mobilestudio.gridviewpager.GridPageOptions.BackgroundListener;

import java.util.HashMap;
import java.util.Map;

public abstract class SupportFragmentGridPagerAdapter extends GridPagerAdapter {
    private static final int MAX_ROWS = 65535;
    private final FragmentManager mFragmentManager;
    private final Map<String, Point> mFragmentPositions;
    private final Map<Point, String> mFragmentTags;
    private FragmentTransaction mCurTransaction;
    private static final BackgroundListener NOOP_BACKGROUND_OBSERVER = new BackgroundListener() {
        public void notifyBackgroundChanged() {
        }
    };

    public SupportFragmentGridPagerAdapter(FragmentManager fm) {
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
        if(fragment instanceof GridPageOptions) {
            GridPageOptions backgroundProvider = (GridPageOptions)fragment;
            backgroundProvider.setBackgroundListener(new SupportFragmentGridPagerAdapter.BackgroundObserver(tag));
        }

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
        if(fragment instanceof GridPageOptions) {
            ((GridPageOptions)fragment).setBackgroundListener(NOOP_BACKGROUND_OBSERVER);
        }

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
        if(f instanceof GridPageOptions) {
            bg = ((GridPageOptions)f).getBackground();
        }

        return bg;
    }

    public Drawable getBackgroundForPage(int row, int column) {
        return this.getFragmentBackground(row, column);
    }

    public void finishUpdate(ViewGroup container) {
        if(this.mFragmentManager.isDestroyed()) {
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

    private class BackgroundObserver implements BackgroundListener {
        private final String mTag;

        private BackgroundObserver(String tag) {
            this.mTag = tag;
        }

        public void notifyBackgroundChanged() {
            Point pos = SupportFragmentGridPagerAdapter.this.mFragmentPositions.get(this.mTag);
            if(pos != null) {
                SupportFragmentGridPagerAdapter.this.notifyPageBackgroundChanged(pos.y, pos.x);
            }

        }
    }
}
