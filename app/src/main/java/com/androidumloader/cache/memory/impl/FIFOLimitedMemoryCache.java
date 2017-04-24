package com.androidumloader.cache.memory.impl;

import android.graphics.Bitmap;

import com.androidumloader.cache.memory.LimitedMemoryCache;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/******************************************
 * 类名称：FIFOLimitedMemoryCache
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 17:32
 ******************************************/
public class FIFOLimitedMemoryCache extends LimitedMemoryCache {
    private final List<Bitmap> queue = Collections.synchronizedList(new LinkedList<Bitmap>());

    public FIFOLimitedMemoryCache(int sizeLimit) {
        super(sizeLimit);
    }

    @Override
    public boolean put(String key, Bitmap value) {
        if (super.put(key, value)) {
            queue.add(value);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Bitmap remove(String key) {
        Bitmap value = super.get(key);
        if (value != null) {
            queue.remove(value);
        }
        return super.remove(key);
    }

    @Override
    public void clear() {
        queue.clear();
        super.clear();
    }

    @Override
    protected int getSize(Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    @Override
    protected Bitmap removeNext() {
        return queue.remove(0);
    }

    @Override
    protected Reference<Bitmap> createReference(Bitmap value) {
        return new WeakReference<>(value);
    }
}
