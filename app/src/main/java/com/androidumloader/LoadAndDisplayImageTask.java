package com.androidumloader;

import android.graphics.Bitmap;
import android.os.Handler;

import com.androidumloader.assist.FailReason;
import com.androidumloader.assist.ImageScaleType;
import com.androidumloader.assist.ImageSize;
import com.androidumloader.assist.LoadedFrom;
import com.androidumloader.decode.ImageDecoder;
import com.androidumloader.decode.ImageDecodingInfo;
import com.androidumloader.download.ImageDownloader;
import com.androidumloader.imageaware.ImageAware;
import com.androidumloader.imageaware.ViewScaleType;
import com.androidumloader.listener.ImageLoadingListener;
import com.androidumloader.listener.ImageLoadingProgressListener;
import com.androidumloader.utils.IoUtils;
import com.androidumloader.utils.L;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.androidumloader.assist.FailReason.FailType.DECODING_ERROR;
import static com.androidumloader.assist.FailReason.FailType.IO_ERROR;
import static com.androidumloader.assist.FailReason.FailType.NETWORK_DENIED;
import static com.androidumloader.assist.FailReason.FailType.OUT_OF_MEMORY;
import static com.androidumloader.assist.FailReason.FailType.UNKNOWN;

/******************************************
 * 类名称：LoadAndDisplayImageTask
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/25 11:04
 ******************************************/
public class LoadAndDisplayImageTask implements Runnable, IoUtils.CopyListener {
    private static final String LOG_WAITING_FOR_RESUME = "ImageLoader is paused. Waiting...  [%s]";
    private static final String LOG_RESUME_AFTER_PAUSE = ".. Resume loading [%s]";
    private static final String LOG_DELAY_BEFORE_LOADING = "Delay %d ms before loading...  [%s]";
    private static final String LOG_START_DISPLAY_IMAGE_TASK = "Start display image task [%s]";
    private static final String LOG_WAITING_FOR_IMAGE_LOADED = "Image already is loading. Waiting... [%s]";
    private static final String LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING = "...Get cached bitmap from memory after waiting. [%s]";
    private static final String LOG_LOAD_IMAGE_FROM_NETWORK = "Load image from network [%s]";
    private static final String LOG_LOAD_IMAGE_FROM_DISK_CACHE = "Load image from disk cache [%s]";
    private static final String LOG_RESIZE_CACHED_IMAGE_FILE = "Resize image in disk cache [%s]";
    private static final String LOG_PREPROCESS_IMAGE = "PreProcess image before caching in memory [%s]";
    private static final String LOG_POSTPROCESS_IMAGE = "PostProcess image before displaying [%s]";
    private static final String LOG_CACHE_IMAGE_IN_MEMORY = "Cache image in memory [%s]";
    private static final String LOG_CACHE_IMAGE_ON_DISK = "Cache image on disk [%s]";
    private static final String LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK = "Process image before cache on disk [%s]";
    private static final String LOG_TASK_CANCELLED_IMAGEAWARE_REUSED = "ImageAware is reused for another image. Task is cancelled. [%s]";
    private static final String LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED = "ImageAware was collected by GC. Task is cancelled. [%s]";
    private static final String LOG_TASK_INTERRUPTED = "Task was interrupted [%s]";

    private static final String ERROR_NO_IMAGE_STREAM = "No stream for image [%s]";
    private static final String ERROR_PRE_PROCESSOR_NULL = "Pre-processor returned null [%s]";
    private static final String ERROR_POST_PROCESSOR_NULL = "Post-processor returned null [%s]";
    private static final String ERROR_PROCESSOR_FOR_DISK_CACHE_NULL = "Bitmap processor for disk cache returned null [%s]";

    private final ImageLoaderEngine engine;
    private final ImageLoadingInfo imageLoadingInfo;
    private final Handler handler;

    // Helper references
    private final ImageLoaderConfiguration configuration;
    private final ImageDownloader downloader;
    private final ImageDownloader networkDeniedDownloader;
    private final ImageDownloader slowNetworkDownloader;
    private final ImageDecoder decoder;
    final String uri;
    private final String memoryCacheKey;
    final ImageAware imageAware;
    private final ImageSize targetSize;
    final DisplayImageOptions options;
    final ImageLoadingListener listener;
    final ImageLoadingProgressListener progressListener;
    private final boolean syncLoading;

    // State vars
    private LoadedFrom loadedFrom = LoadedFrom.NETWORK;

    public LoadAndDisplayImageTask(ImageLoaderEngine engine, ImageLoadingInfo imageLoadingInfo, Handler handler) {
        this.engine = engine;
        this.imageLoadingInfo = imageLoadingInfo;
        this.handler = handler;

        configuration = engine.configuration;
        downloader = configuration.downloader;
        networkDeniedDownloader = configuration.networkDeniedDownloader;
        slowNetworkDownloader = configuration.slowNetworkDownloader;
        decoder = configuration.decoder;
        uri = imageLoadingInfo.uri;
        memoryCacheKey = imageLoadingInfo.memoryCacheKey;
        imageAware = imageLoadingInfo.imageAware;
        targetSize = imageLoadingInfo.targetSize;
        options = imageLoadingInfo.options;
        listener = imageLoadingInfo.listener;
        progressListener = imageLoadingInfo.progressListener;
        syncLoading = options.isSyncLoading();
    }

    @Override
    public boolean onBytesCopied(int current, int total) {
        return syncLoading || fireProgressEvent(current, total);
    }

    private boolean fireProgressEvent(final int current, final int total) {
        if (isTaskInterrupted() || isTaskNotActual()) return false;
        if (progressListener != null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    progressListener.onProgressUpdate(uri, imageAware.getWrappedView(), current, total);
                }
            };
            runTask(r, false, handler, engine);
        }
        return true;
    }


    String getLoadingUri() {
        return uri;
    }

    @Override
    public void run() {
        if (waitIfPaused()) return;
        if (delayIfNeed()) return;
        ReentrantLock loadFromUriLock = imageLoadingInfo.loadFromUriLock;
        L.d(LOG_START_DISPLAY_IMAGE_TASK, memoryCacheKey);
        if (loadFromUriLock.isLocked()) {
            L.d(LOG_WAITING_FOR_IMAGE_LOADED, memoryCacheKey);
        }

        loadFromUriLock.lock();
        Bitmap bmp;
        try {
            checkTaskNotActual();
            bmp = configuration.memoryCache.get(memoryCacheKey);
            if (bmp == null || bmp.isRecycled()) {
                bmp = tryLoadBitmap();
                if (bmp == null) return; // listener callback already was fired
                checkTaskNotActual();
                checkTaskInterrupted();

                if (options.shouldPreProcess()) {
                    L.d(LOG_PREPROCESS_IMAGE, memoryCacheKey);
                    bmp = options.getPreProcessor().process(bmp);
                    if (bmp == null) {
                        L.e(ERROR_PRE_PROCESSOR_NULL, memoryCacheKey);
                    }
                }

                if (bmp != null && options.isCacheInMemory()) {
                    L.d(LOG_CACHE_IMAGE_IN_MEMORY, memoryCacheKey);
                    configuration.memoryCache.put(memoryCacheKey, bmp);
                }

            } else {
                loadedFrom = LoadedFrom.MEMORY_CACHE;
                L.d(LOG_GET_IMAGE_FROM_MEMORY_CACHE_AFTER_WAITING, memoryCacheKey);
            }

            if (bmp != null && options.shouldPostProcess()) {
                L.d(LOG_POSTPROCESS_IMAGE, memoryCacheKey);
                bmp = options.getPostProcessor().process(bmp);
                if (bmp == null) {
                    L.e(ERROR_POST_PROCESSOR_NULL, memoryCacheKey);
                }
            }
            checkTaskNotActual();
            checkTaskInterrupted();

        } catch (TaskCancelledException e) {
            fireCancelEvent();
            return;
        } finally {
            loadFromUriLock.unlock();
        }
        DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bmp, imageLoadingInfo, engine, loadedFrom);
        runTask(displayBitmapTask, syncLoading, handler, engine);
    }

    private void fireCancelEvent() {
        if (syncLoading || isTaskInterrupted()) return;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                listener.onLoadingCancelled(uri, imageAware.getWrappedView());
            }
        };
        runTask(r, false, handler, engine);
    }


    private void checkTaskInterrupted() throws TaskCancelledException {
        if (isTaskInterrupted()) {
            throw new TaskCancelledException();
        }
    }

    private Bitmap tryLoadBitmap() throws TaskCancelledException {
        Bitmap bitmap = null;
        try {
            File imageFile = configuration.diskCache.get(uri);
            if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
                L.d(LOG_LOAD_IMAGE_FROM_DISK_CACHE, memoryCacheKey);
                loadedFrom = LoadedFrom.DISC_CACHE;
                checkTaskNotActual();
                bitmap = decodeImage(ImageDownloader.Scheme.FILE.wrap(imageFile.getAbsolutePath()));
            }
            if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                L.d(LOG_LOAD_IMAGE_FROM_NETWORK, memoryCacheKey);
                loadedFrom = LoadedFrom.NETWORK;

                String imageUriForDecoding = uri;
                if (options.isCacheOnDisk() && tryCacheImageOnDisk()) {
                    imageFile = configuration.diskCache.get(uri);
                    if (imageFile != null) {
                        imageUriForDecoding = ImageDownloader.Scheme.FILE.wrap(imageFile.getAbsolutePath());
                    }
                }
                checkTaskNotActual();
                bitmap = decodeImage(imageUriForDecoding);
                if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                    fireFailEvent(DECODING_ERROR, null);
                }
            }
        } catch (IllegalStateException e) {
            fireFailEvent(NETWORK_DENIED, null);
        } catch (TaskCancelledException e) {
            throw e;
        } catch (IOException e) {
            L.e(e);
            fireFailEvent(IO_ERROR, e);
        } catch (OutOfMemoryError e) {
            L.e(e);
            fireFailEvent(OUT_OF_MEMORY, e);
        } catch (Throwable e) {
            L.e(e);
            fireFailEvent(UNKNOWN, e);
        }
        return bitmap;
    }

    private void fireFailEvent(final FailReason.FailType failType, final Throwable failCause) {
        if (syncLoading || isTaskInterrupted() || isTaskNotActual()) return;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (options.shouldShowImageOnFail()) {
                    imageAware.setImageDrawable(options.getImageOnFail(configuration.resources));
                }
                listener.onLoadingFailed(uri, imageAware.getWrappedView(), new FailReason(failType, failCause));
            }
        };
        runTask(r, false, handler, engine);
    }

    static void runTask(Runnable r, boolean sync, Handler handler, ImageLoaderEngine engine) {
        if (sync) {
            r.run();
        } else if (handler == null) {
            engine.fireCallback(r);
        } else {
            handler.post(r);
        }
    }

    private boolean isTaskInterrupted() {
        if (Thread.interrupted()) {
            L.d(LOG_TASK_INTERRUPTED, memoryCacheKey);
            return true;
        }
        return false;
    }

    private boolean tryCacheImageOnDisk() throws TaskCancelledException {
        L.d(LOG_CACHE_IMAGE_ON_DISK, memoryCacheKey);

        boolean loaded;
        try {
            loaded = downloadImage();
            if (loaded) {
                int width = configuration.maxImageWidthForDiskCache;
                int height = configuration.maxImageHeightForDiskCache;
                if (width > 0 || height > 0) {
                    L.d(LOG_RESIZE_CACHED_IMAGE_FILE, memoryCacheKey);
                    resizeAndSaveImage(width, height); // TODO : process boolean result
                }
            }
        } catch (IOException e) {
            L.e(e);
            loaded = false;
        }
        return loaded;
    }

    private boolean resizeAndSaveImage(int maxWidth, int maxHeight) throws IOException {
        // Decode image file, compress and re-save it
        boolean saved = false;
        File targetFile = configuration.diskCache.get(uri);
        if (targetFile != null && targetFile.exists()) {
            ImageSize targetImageSize = new ImageSize(maxWidth, maxHeight);
            DisplayImageOptions specialOptions = new DisplayImageOptions.Builder().cloneFrom(options)
                    .imageScaleType(ImageScaleType.IN_SAMPLE_INT).build();
            ImageDecodingInfo decodingInfo = new ImageDecodingInfo(memoryCacheKey,
                    ImageDownloader.Scheme.FILE.wrap(targetFile.getAbsolutePath()), uri, targetImageSize, ViewScaleType.FIT_INSIDE,
                    getDownloader(), specialOptions);
            Bitmap bmp = decoder.decode(decodingInfo);
            if (bmp != null && configuration.processorForDiskCache != null) {
                L.d(LOG_PROCESS_IMAGE_BEFORE_CACHE_ON_DISK, memoryCacheKey);
                bmp = configuration.processorForDiskCache.process(bmp);
                if (bmp == null) {
                    L.e(ERROR_PROCESSOR_FOR_DISK_CACHE_NULL, memoryCacheKey);
                }
            }
            if (bmp != null) {
                saved = configuration.diskCache.save(uri, bmp);
                bmp.recycle();
            }
        }
        return saved;
    }

    private boolean downloadImage() throws IOException {
        InputStream is = getDownloader().getStream(uri, options.getExtraForDownloader());
        if (is == null) {
            L.e(ERROR_NO_IMAGE_STREAM, memoryCacheKey);
            return false;
        } else {
            try {
                return configuration.diskCache.save(uri, is, this);
            } finally {
                IoUtils.closeSilently(is);
            }
        }
    }

    private Bitmap decodeImage(String imageUri) throws IOException {
        ViewScaleType viewScaleType = imageAware.getScaleType();
        ImageDecodingInfo decodingInfo = new ImageDecodingInfo(memoryCacheKey, imageUri, uri, targetSize, viewScaleType,
                getDownloader(), options);

        return decoder.decode(decodingInfo);
    }

    private ImageDownloader getDownloader() {
        ImageDownloader d;
        if (engine.isNetworkDenied()) {
            d = networkDeniedDownloader;
        } else if (engine.isSlowNetwork()) {
            d = slowNetworkDownloader;
        } else {
            d = downloader;
        }
        return d;
    }

    private void checkTaskNotActual() throws TaskCancelledException {
        checkViewCollected();
        checkViewReused();
    }

    private void checkViewCollected() throws TaskCancelledException {
        if (isViewCollected()) {
            throw new TaskCancelledException();
        }
    }

    private void checkViewReused() throws TaskCancelledException {
        if (isViewReused()) {
            throw new TaskCancelledException();
        }
    }

    class TaskCancelledException extends Exception {
    }

    private boolean waitIfPaused() {
        AtomicBoolean pause = engine.getPause();
        if (pause.get()) {
            synchronized (engine.getPauseLock()) {
                if (pause.get()) {
                    L.d(LOG_WAITING_FOR_RESUME, memoryCacheKey);
                    try {
                        engine.getPauseLock().wait();
                    } catch (InterruptedException e) {
                        L.e(LOG_TASK_INTERRUPTED, memoryCacheKey);
                        return true;
                    }
                    L.d(LOG_RESUME_AFTER_PAUSE, memoryCacheKey);
                }
            }
        }
        return isTaskNotActual();
    }

    private boolean isTaskNotActual() {
        return isViewCollected() || isViewReused();
    }

    private boolean isViewCollected() {
        if (imageAware.isCollected()) {
            L.d(LOG_TASK_CANCELLED_IMAGEAWARE_COLLECTED, memoryCacheKey);
            return true;
        }
        return false;
    }

    private boolean isViewReused() {
        String currentCacheKey = engine.getLoadingUriForView(imageAware);
        // Check whether memory cache key (image URI) for current ImageAware is actual.
        // If ImageAware is reused for another task then current task should be cancelled.
        boolean imageAwareWasReused = !memoryCacheKey.equals(currentCacheKey);
        if (imageAwareWasReused) {
            L.d(LOG_TASK_CANCELLED_IMAGEAWARE_REUSED, memoryCacheKey);
            return true;
        }
        return false;
    }

    private boolean delayIfNeed() {
        if (options.shouldDelayBeforeLoading()) {
            L.d(LOG_DELAY_BEFORE_LOADING, options.getDelayBeforeLoading(), memoryCacheKey);
            try {
                Thread.sleep(options.getDelayBeforeLoading());
            } catch (InterruptedException e) {
                L.e(LOG_TASK_INTERRUPTED, memoryCacheKey);
                return true;
            }
            return isTaskNotActual();
        }
        return false;
    }
}
