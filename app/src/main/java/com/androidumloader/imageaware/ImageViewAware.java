package com.androidumloader.imageaware;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.androidumloader.utils.L;

import java.lang.reflect.Field;

/******************************************
 * 类名称：ImageViewAware
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 11:28
 ******************************************/
public class ImageViewAware extends ViewAware {
    public ImageViewAware(ImageView imageView) {
        super(imageView);
    }

    public ImageViewAware(ImageView imageView, boolean checkActualViewSize) {
        super(imageView, checkActualViewSize);
    }

    @Override
    public int getWidth() {
        int width = super.getWidth();
        if (width <= 0) {
            ImageView imageView = (ImageView) viewRef.get();
            if (imageView != null) {
                width = getImageViewFieldValue(imageView, "mMaxWidth"); // Check maxWidth parameter
            }
        }
        return width;
    }

    @Override
    public int getHeight() {
        int height = super.getHeight();
        if (height <= 0) {
            ImageView imageView = (ImageView) viewRef.get();
            if (imageView != null) {
                height = getImageViewFieldValue(imageView, "mMaxHeight"); // Check maxHeight parameter
            }
        }
        return height;
    }

    @Override
    public ImageView getWrappedView() {
        return (ImageView) super.getWrappedView();
    }


    @Override
    public ViewScaleType getScaleType() {
        ImageView imageView = (ImageView) viewRef.get();
        if (imageView != null) {
            return ViewScaleType.fromImageView(imageView);
        }
        return super.getScaleType();
    }

    @Override
    protected void setImageDrawableInto(Drawable drawable, View view) {
        ((ImageView) view).setImageDrawable(drawable);
        if (drawable instanceof AnimationDrawable) {
            ((AnimationDrawable) drawable).start();
        }
    }

    @Override
    protected void setImageBitmapInto(Bitmap bitmap, View view) {
        ((ImageView) view).setImageBitmap(bitmap);
    }

    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = (Integer) field.get(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            L.e(e);
        }
        return value;
    }
}
