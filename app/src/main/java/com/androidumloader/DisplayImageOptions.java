package com.androidumloader;

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

    private DisplayImageOptions(Builder builder) {
        imageResOnLoading = builder.imageResOnLoading;
    }

    public static class Builder {
        private int imageResOnLoading = 0;

        public Builder showImageOnLoading(int imageRes) {
            imageResOnLoading = imageRes;
            return this;
        }

        public DisplayImageOptions build() {
            return new DisplayImageOptions(this);
        }
    }
}
