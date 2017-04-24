package com.androidumloader.decode;

import android.annotation.TargetApi;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.graphics.BitmapFactory.Options;

import com.androidumloader.DisplayImageOptions;
import com.androidumloader.ImageScaleType;
import com.androidumloader.ImageSize;
import com.androidumloader.download.ImageDownloader;
import com.androidumloader.imageaware.ViewScaleType;

/******************************************
 * 类名称：ImageDecodingInfo
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 14:20
 ******************************************/
public class ImageDecodingInfo {
    private final String imageKey;
    private final String imageUri;
    private final String originalImageUri;
    private final ImageSize targetSize;

    private final ImageScaleType imageScaleType;
    private final ViewScaleType viewScaleType;

    private final ImageDownloader downloader;
    private final Object extraForDownloader;

    private final boolean considerExifParams;
    private final BitmapFactory.Options decodingOptions;

    public ImageDecodingInfo(String imageKey, String imageUri, String originalImageUri, ImageSize targetSize, ViewScaleType viewScaleType,
                             ImageDownloader downloader, DisplayImageOptions displayOptions) {
        this.imageKey = imageKey;
        this.imageUri = imageUri;
        this.originalImageUri = originalImageUri;
        this.targetSize = targetSize;

        this.imageScaleType = displayOptions.getImageScaleType();
        this.viewScaleType = viewScaleType;

        this.downloader = downloader;
        this.extraForDownloader = displayOptions.getExtraForDownloader();

        considerExifParams = displayOptions.isConsiderExifParams();
        decodingOptions = new BitmapFactory.Options();
        copyOptions(displayOptions.getDecodingOptions(), decodingOptions);
    }

    private void copyOptions(BitmapFactory.Options srcOptions, BitmapFactory.Options destOptions) {
        destOptions.inDensity = srcOptions.inDensity;
        destOptions.inDither = srcOptions.inDither;
        destOptions.inInputShareable = srcOptions.inInputShareable;
        destOptions.inJustDecodeBounds = srcOptions.inJustDecodeBounds;
        destOptions.inPreferredConfig = srcOptions.inPreferredConfig;
        destOptions.inPurgeable = srcOptions.inPurgeable;
        destOptions.inSampleSize = srcOptions.inSampleSize;
        destOptions.inScaled = srcOptions.inScaled;
        destOptions.inScreenDensity = srcOptions.inScreenDensity;
        destOptions.inTargetDensity = srcOptions.inTargetDensity;
        destOptions.inTempStorage = srcOptions.inTempStorage;
        if (Build.VERSION.SDK_INT >= 10) copyOptions10(srcOptions, destOptions);
        if (Build.VERSION.SDK_INT >= 11) copyOptions11(srcOptions, destOptions);
    }

    @TargetApi(10)
    private void copyOptions10(BitmapFactory.Options srcOptions, BitmapFactory.Options destOptions) {
        destOptions.inPreferQualityOverSpeed = srcOptions.inPreferQualityOverSpeed;
    }

    @TargetApi(11)
    private void copyOptions11(BitmapFactory.Options srcOptions, BitmapFactory.Options destOptions) {
        destOptions.inBitmap = srcOptions.inBitmap;
        destOptions.inMutable = srcOptions.inMutable;
    }

    /** @return Original {@linkplain com.nostra13.universalimageloader.utils.MemoryCacheUtils#generateKey(String, ImageSize) image key} (used in memory cache). */
    public String getImageKey() {
        return imageKey;
    }

    /** @return Image URI for decoding (usually image from disk cache) */
    public String getImageUri() {
        return imageUri;
    }

    /** @return The original image URI which was passed to ImageLoader */
    public String getOriginalImageUri() {
        return originalImageUri;
    }

    /**
     * @return Target size for image. Decoded bitmap should close to this size according to {@linkplain ImageScaleType
     * image scale type} and {@linkplain ViewScaleType view scale type}.
     */
    public ImageSize getTargetSize() {
        return targetSize;
    }

    /**
     * @return {@linkplain ImageScaleType Scale type for image sampling and scaling}. This parameter affects result size
     * of decoded bitmap.
     */
    public ImageScaleType getImageScaleType() {
        return imageScaleType;
    }

    /** @return {@linkplain ViewScaleType View scale type}. This parameter affects result size of decoded bitmap. */
    public ViewScaleType getViewScaleType() {
        return viewScaleType;
    }

    /** @return Downloader for image loading */
    public ImageDownloader getDownloader() {
        return downloader;
    }

    /** @return Auxiliary object for downloader */
    public Object getExtraForDownloader() {
        return extraForDownloader;
    }

    /** @return <b>true</b> - if EXIF params of image should be considered; <b>false</b> - otherwise */
    public boolean shouldConsiderExifParams() {
        return considerExifParams;
    }

    /** @return Decoding options */
    public Options getDecodingOptions() {
        return decodingOptions;
    }
}
