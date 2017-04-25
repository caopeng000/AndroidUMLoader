package com.androidumloader;


import android.widget.ImageView;

import com.androidumloader.imageaware.ImageAware;
import com.androidumloader.imageaware.ImageViewAware;
import com.androidumloader.listener.ImageLoadingListener;
import com.androidumloader.listener.ImageLoadingProgressListener;

/******************************************
 * 类名称：ImageLoader
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 11:01
 ******************************************/
public class ImageLoader {
    public static final String TAG = ImageLoader.class.getSimpleName();
    private volatile static ImageLoader instance;

    public static ImageLoader getInstance() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) {
                    instance = new ImageLoader();
                }
            }
        }
        return instance;
    }

    public void displayImage(String uri, ImageView imageView, DisplayImageOptions options,
                             ImageLoadingListener listener) {
        displayImage(uri, imageView, options, listener, null);
    }

    public void displayImage(String uri, ImageView imageView, DisplayImageOptions options,
                             ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        displayImage(uri, new ImageViewAware(imageView), options, listener, progressListener);
    }

    public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options,
                             ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        displayImage(uri, imageAware, options, null, listener, progressListener);
    }
    public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options,
                             ImageSize targetSize, ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {

    }
}
