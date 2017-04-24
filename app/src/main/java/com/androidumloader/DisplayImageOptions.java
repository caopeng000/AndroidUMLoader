package com.androidumloader;

import android.graphics.BitmapFactory.Options;

/******************************************
 * 类名称：DisplayImageOptions
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 10:53
 ******************************************/
public class DisplayImageOptions {

    private final int imageResOnLoading;
    private final ImageScaleType imageScaleType;
    private final Object extraForDownloader;
    private final boolean considerExifParams;
    private final Options decodingOptions;

    private DisplayImageOptions(Builder builder) {
        imageResOnLoading = builder.imageResOnLoading;
        imageScaleType = builder.imageScaleType;
        extraForDownloader = builder.extraForDownloader;
        considerExifParams = builder.considerExifParams;
        decodingOptions = builder.decodingOptions;
    }

    public ImageScaleType getImageScaleType() {
        return imageScaleType;
    }

    public Object getExtraForDownloader() {
        return extraForDownloader;
    }

    public boolean isConsiderExifParams() {
        return considerExifParams;
    }

    public Options getDecodingOptions() {
        return decodingOptions;
    }

    public static class Builder {
        private int imageResOnLoading = 0;
        private ImageScaleType imageScaleType = ImageScaleType.IN_SAMPLE_POWER_OF_2;
        private Object extraForDownloader = null;
        private boolean considerExifParams = false;
        private Options decodingOptions = new Options();

        public Builder showImageOnLoading(int imageRes) {
            imageResOnLoading = imageRes;
            return this;
        }


        public DisplayImageOptions build() {
            return new DisplayImageOptions(this);
        }
    }
}
