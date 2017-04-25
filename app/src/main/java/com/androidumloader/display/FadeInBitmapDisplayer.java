package com.androidumloader.display;

import android.graphics.Bitmap;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;

import com.androidumloader.assist.LoadedFrom;
import com.androidumloader.imageaware.ImageAware;

/******************************************
 * 类名称：FadeInBitmapDisplayer
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/25 14:24
 ******************************************/
public class FadeInBitmapDisplayer implements BitmapDisplayer {
    private final int durationMillis;
    private final boolean animateFromNetwork;
    private final boolean animateFromDisk;
    private final boolean animateFromMemory;

    public FadeInBitmapDisplayer(int durationMillis) {
        this(durationMillis, true, true, true);
    }

    public FadeInBitmapDisplayer(int durationMillis, boolean animateFromNetwork, boolean animateFromDisk,
                                 boolean animateFromMemory) {
        this.durationMillis = durationMillis;
        this.animateFromNetwork = animateFromNetwork;
        this.animateFromDisk = animateFromDisk;
        this.animateFromMemory = animateFromMemory;
    }

    @Override
    public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
        imageAware.setImageBitmap(bitmap);
        if ((animateFromNetwork && loadedFrom == LoadedFrom.NETWORK) ||
                (animateFromDisk && loadedFrom == LoadedFrom.DISC_CACHE) ||
                (animateFromMemory && loadedFrom == LoadedFrom.MEMORY_CACHE)) {
            animate(imageAware.getWrappedView(), durationMillis);
        }
    }

    public static void animate(View imageView, int durationMillis) {
        if (imageView != null) {
            AlphaAnimation fadeImage = new AlphaAnimation(0, 1);
            fadeImage.setDuration(durationMillis);
            fadeImage.setInterpolator(new DecelerateInterpolator());
            imageView.startAnimation(fadeImage);
        }
    }
}
