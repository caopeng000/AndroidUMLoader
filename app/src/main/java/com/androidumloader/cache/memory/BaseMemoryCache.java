package com.androidumloader.cache.memory;

import android.graphics.Bitmap;

import java.lang.ref.Reference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/******************************************
 * 类名称：BaseMemoryCache
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 17:04
 ******************************************/
public abstract class BaseMemoryCache implements MemoryCache {
    private final Map<String, Reference<Bitmap>> softMap = Collections.synchronizedMap(new HashMap<String, Reference<Bitmap>>());

    @Override
    public boolean put(String key, Bitmap value) {
        softMap.put(key, createReference(value));
        return true;
    }

    @Override
    public Bitmap get(String key) {
        Bitmap result = null;
        Reference<Bitmap> reference = softMap.get(key);
        if (reference != null) {
            result = reference.get();
        }
        return result;
    }

    @Override
    public Bitmap remove(String key) {
        Reference<Bitmap> bmpRef = softMap.remove(key);
        return bmpRef == null ? null : bmpRef.get();
    }

    @Override
    public Collection<String> keys() {
        synchronized (softMap) {
            return new HashSet<String>(softMap.keySet());
        }
    }

    @Override
    public void clear() {
        softMap.clear();
    }

    protected abstract Reference<Bitmap> createReference(Bitmap value);
}
