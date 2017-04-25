package com.androidumloader;


import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.androidumloader.assist.ImageSize;
import com.androidumloader.assist.LoadedFrom;
import com.androidumloader.cache.disc.DiskCache;
import com.androidumloader.cache.memory.MemoryCache;
import com.androidumloader.imageaware.ImageAware;
import com.androidumloader.imageaware.ImageViewAware;
import com.androidumloader.imageaware.NonViewAware;
import com.androidumloader.imageaware.ViewScaleType;
import com.androidumloader.listener.ImageLoadingListener;
import com.androidumloader.listener.ImageLoadingProgressListener;
import com.androidumloader.listener.SimpleImageLoadingListener;
import com.androidumloader.utils.ImageSizeUtils;
import com.androidumloader.utils.L;
import com.androidumloader.utils.MemoryCacheUtils;

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

    static final String LOG_INIT_CONFIG = "Initialize ImageLoader with configuration";
    static final String LOG_DESTROY = "Destroy ImageLoader";
    static final String LOG_LOAD_IMAGE_FROM_MEMORY_CACHE = "Load image from memory cache [%s]";

    private static final String WARNING_RE_INIT_CONFIG = "Try to initialize ImageLoader which had already been initialized before. " + "To re-init ImageLoader with new configuration call ImageLoader.destroy() at first.";
    private static final String ERROR_WRONG_ARGUMENTS = "Wrong arguments were passed to displayImage() method (ImageView reference must not be null)";
    private static final String ERROR_NOT_INIT = "ImageLoader must be init with configuration before using";
    private static final String ERROR_INIT_CONFIG_WITH_NULL = "ImageLoader configuration can not be initialized with null";

    private ImageLoaderConfiguration configuration;
    private ImageLoaderEngine engine;

    private ImageLoadingListener defaultListener = new SimpleImageLoadingListener();

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


    protected ImageLoader() {
    }

    public synchronized void init(ImageLoaderConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
        }
        if (this.configuration == null) {
            L.d(LOG_INIT_CONFIG);
            engine = new ImageLoaderEngine(configuration);
            this.configuration = configuration;
        } else {
            L.w(WARNING_RE_INIT_CONFIG);
        }
    }

    public boolean isInited() {
        return configuration != null;
    }


    public void displayImage(String uri, ImageAware imageAware) {
        displayImage(uri, imageAware, null, null, null);
    }


    public void displayImage(String uri, ImageAware imageAware, ImageLoadingListener listener) {
        displayImage(uri, imageAware, null, listener, null);
    }


    public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options) {
        displayImage(uri, imageAware, options, null, null);
    }

    public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options,
                             ImageLoadingListener listener) {
        displayImage(uri, imageAware, options, listener, null);
    }


    public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options,
                             ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        displayImage(uri, imageAware, options, null, listener, progressListener);
    }


    public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options,
                             ImageSize targetSize, ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        checkConfiguration();
        if (imageAware == null) {
            throw new IllegalArgumentException(ERROR_WRONG_ARGUMENTS);
        }
        if (listener == null) {
            listener = defaultListener;
        }
        if (options == null) {
            options = configuration.defaultDisplayImageOptions;
        }
        if (TextUtils.isEmpty(uri)) {
            engine.cancelDisplayTaskFor(imageAware);
            listener.onLoadingStarted(uri, imageAware.getWrappedView());
            if (options.shouldShowImageForEmptyUri()) {
                imageAware.setImageDrawable(options.getImageForEmptyUri(configuration.resources));
            } else {
                imageAware.setImageDrawable(null);
            }
            listener.onLoadingComplete(uri, imageAware.getWrappedView(), null);
            return;
        }
        if (targetSize == null) {
            targetSize = ImageSizeUtils.defineTargetSizeForView(imageAware, configuration.getMaxImageSize());
        }

        String memoryCacheKey = MemoryCacheUtils.generateKey(uri, targetSize);
        engine.prepareDisplayTaskFor(imageAware, memoryCacheKey);

        listener.onLoadingStarted(uri, imageAware.getWrappedView());
        Bitmap bmp = configuration.memoryCache.get(memoryCacheKey);
        if (bmp != null && !bmp.isRecycled()) {
            L.d(LOG_LOAD_IMAGE_FROM_MEMORY_CACHE, memoryCacheKey);
            if (options.shouldPostProcess()) {
                ImageLoadingInfo imageLoadingInfo = new ImageLoadingInfo(uri, imageAware, targetSize, memoryCacheKey,
                        options, listener, progressListener, engine.getLockForUri(uri));
                ProcessAndDisplayImageTask displayTask = new ProcessAndDisplayImageTask(engine, bmp, imageLoadingInfo,
                        defineHandler(options));
                if (options.isSyncLoading()) {
                    displayTask.run();
                } else {
                    engine.submit(displayTask);
                }
            } else {
                options.getDisplayer().display(bmp, imageAware, LoadedFrom.MEMORY_CACHE);
                listener.onLoadingComplete(uri, imageAware.getWrappedView(), bmp);
            }
        } else {
            if (options.shouldShowImageOnLoading()) {
                imageAware.setImageDrawable(options.getImageOnLoading(configuration.resources));
            } else if (options.isResetViewBeforeLoading()) {
                imageAware.setImageDrawable(null);
            }

            ImageLoadingInfo imageLoadingInfo = new ImageLoadingInfo(uri, imageAware, targetSize, memoryCacheKey,
                    options, listener, progressListener, engine.getLockForUri(uri));
            LoadAndDisplayImageTask displayTask = new LoadAndDisplayImageTask(engine, imageLoadingInfo,
                    defineHandler(options));
            if (options.isSyncLoading()) {
                displayTask.run();
            } else {
                engine.submit(displayTask);
            }
        }
    }

    public void displayImage(String uri, ImageView imageView) {
        displayImage(uri, new ImageViewAware(imageView), null, null, null);
    }


    public void displayImage(String uri, ImageView imageView, ImageSize targetImageSize) {
        displayImage(uri, new ImageViewAware(imageView), null, targetImageSize, null, null);
    }


    public void displayImage(String uri, ImageView imageView, DisplayImageOptions options) {
        displayImage(uri, new ImageViewAware(imageView), options, null, null);
    }

    public void displayImage(String uri, ImageView imageView, ImageLoadingListener listener) {
        displayImage(uri, new ImageViewAware(imageView), null, listener, null);
    }


    public void displayImage(String uri, ImageView imageView, DisplayImageOptions options,
                             ImageLoadingListener listener) {
        displayImage(uri, imageView, options, listener, null);
    }


    public void displayImage(String uri, ImageView imageView, DisplayImageOptions options,
                             ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        displayImage(uri, new ImageViewAware(imageView), options, listener, progressListener);
    }

    public void loadImage(String uri, ImageLoadingListener listener) {
        loadImage(uri, null, null, listener, null);
    }


    public void loadImage(String uri, ImageSize targetImageSize, ImageLoadingListener listener) {
        loadImage(uri, targetImageSize, null, listener, null);
    }


    public void loadImage(String uri, DisplayImageOptions options, ImageLoadingListener listener) {
        loadImage(uri, null, options, listener, null);
    }


    public void loadImage(String uri, ImageSize targetImageSize, DisplayImageOptions options,
                          ImageLoadingListener listener) {
        loadImage(uri, targetImageSize, options, listener, null);
    }

    public void loadImage(String uri, ImageSize targetImageSize, DisplayImageOptions options,
                          ImageLoadingListener listener, ImageLoadingProgressListener progressListener) {
        checkConfiguration();
        if (targetImageSize == null) {
            targetImageSize = configuration.getMaxImageSize();
        }
        if (options == null) {
            options = configuration.defaultDisplayImageOptions;
        }

        NonViewAware imageAware = new NonViewAware(uri, targetImageSize, ViewScaleType.CROP);
        displayImage(uri, imageAware, options, listener, progressListener);
    }


    private void checkConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException(ERROR_NOT_INIT);
        }
    }

    private static Handler defineHandler(DisplayImageOptions options) {
        Handler handler = options.getHandler();
        if (options.isSyncLoading()) {
            handler = null;
        } else if (handler == null && Looper.myLooper() == Looper.getMainLooper()) {
            handler = new Handler();
        }
        return handler;
    }

    public void setDefaultLoadingListener(ImageLoadingListener listener) {
        defaultListener = listener == null ? new SimpleImageLoadingListener() : listener;
    }

    public MemoryCache getMemoryCache() {
        checkConfiguration();
        return configuration.memoryCache;
    }

    public void clearMemoryCache() {
        checkConfiguration();
        configuration.memoryCache.clear();
    }

    @Deprecated
    public DiskCache getDiscCache() {
        return getDiskCache();
    }


    public DiskCache getDiskCache() {
        checkConfiguration();
        return configuration.diskCache;
    }

    @Deprecated
    public void clearDiscCache() {
        clearDiskCache();
    }

    public void clearDiskCache() {
        checkConfiguration();
        configuration.diskCache.clear();
    }

    public String getLoadingUriForView(ImageAware imageAware) {
        return engine.getLoadingUriForView(imageAware);
    }


    public String getLoadingUriForView(ImageView imageView) {
        return engine.getLoadingUriForView(new ImageViewAware(imageView));
    }

    public void cancelDisplayTask(ImageAware imageAware) {
        engine.cancelDisplayTaskFor(imageAware);
    }


    public void cancelDisplayTask(ImageView imageView) {
        engine.cancelDisplayTaskFor(new ImageViewAware(imageView));
    }


    public void denyNetworkDownloads(boolean denyNetworkDownloads) {
        engine.denyNetworkDownloads(denyNetworkDownloads);
    }

    public void handleSlowNetwork(boolean handleSlowNetwork) {
        engine.handleSlowNetwork(handleSlowNetwork);
    }


    public void pause() {
        engine.pause();
    }


    public void resume() {
        engine.resume();
    }


    public void stop() {
        engine.stop();
    }


    public void destroy() {
        if (configuration != null) L.d(LOG_DESTROY);
        stop();
        configuration.diskCache.close();
        engine = null;
        configuration = null;
    }

    private static class SyncImageLoadingListener extends SimpleImageLoadingListener {

        private Bitmap loadedImage;

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            this.loadedImage = loadedImage;
        }

        public Bitmap getLoadedBitmap() {
            return loadedImage;
        }
    }
}
