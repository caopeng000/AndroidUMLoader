package com.androidumloader.cache.disc.impl;

import android.graphics.Bitmap;

import com.androidumloader.DefaultConfigurationFactory;
import com.androidumloader.cache.disc.naming.FileNameGenerator;
import com.androidumloader.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/******************************************
 * 类名称：LimitedAgeDiskCache
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 18:40
 ******************************************/
public class LimitedAgeDiskCache extends BaseDiskCache {
    private final long maxFileAge;
    private final Map<File, Long> loadingDates = Collections.synchronizedMap(new HashMap<File, Long>());

    public LimitedAgeDiskCache(File cacheDir, long maxAge) {
        this(cacheDir, null, DefaultConfigurationFactory.createFileNameGenerator(), maxAge);
    }

    public LimitedAgeDiskCache(File cacheDir, File reserveCacheDir, long maxAge) {
        this(cacheDir, reserveCacheDir, DefaultConfigurationFactory.createFileNameGenerator(), maxAge);
    }

    public LimitedAgeDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator, long maxAge) {
        super(cacheDir, reserveCacheDir, fileNameGenerator);
        this.maxFileAge = maxAge * 1000; // to milliseconds
    }

    @Override
    public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
        boolean saved = super.save(imageUri, imageStream, listener);
        rememberUsage(imageUri);
        return saved;
    }

    @Override
    public boolean save(String imageUri, Bitmap bitmap) throws IOException {
        boolean saved = super.save(imageUri, bitmap);
        rememberUsage(imageUri);
        return saved;
    }

    private void rememberUsage(String imageUri) {
        File file = getFile(imageUri);
        long currentTime = System.currentTimeMillis();
        file.setLastModified(currentTime);
        loadingDates.put(file, currentTime);
    }

    @Override
    public File get(String imageUri) {
        File file = super.get(imageUri);
        if (file != null && file.exists()) {
            boolean cached;
            Long loadingDate = loadingDates.get(file);
            if (loadingDate == null) {
                cached = false;
                loadingDate = file.lastModified();
            } else {
                cached = true;
            }

            if (System.currentTimeMillis() - loadingDate > maxFileAge) {
                file.delete();
                loadingDates.remove(file);
            } else if (!cached) {
                loadingDates.put(file, loadingDate);
            }
        }
        return file;
    }

    @Override
    public boolean remove(String imageUri) {
        loadingDates.remove(getFile(imageUri));
        return super.remove(imageUri);
    }


    @Override
    public void clear() {
        super.clear();
        loadingDates.clear();
    }
}
