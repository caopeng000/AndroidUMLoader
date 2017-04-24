package com.androidumloader.cache.disc.impl;


import android.graphics.Bitmap;

import com.androidumloader.DefaultConfigurationFactory;
import com.androidumloader.cache.disc.DiskCache;
import com.androidumloader.cache.disc.naming.FileNameGenerator;
import com.androidumloader.utils.IoUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/******************************************
 * 类名称：BaseDiskCache
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/24 18:26
 ******************************************/
public class BaseDiskCache implements DiskCache {
    /**
     * {@value
     */
    public static final int DEFAULT_BUFFER_SIZE = 32 * 1024; // 32 Kb
    /**
     * {@value
     */
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    /**
     * {@value
     */
    public static final int DEFAULT_COMPRESS_QUALITY = 100;
    private static final String ERROR_ARG_NULL = " argument must be not null";
    private static final String TEMP_IMAGE_POSTFIX = ".tmp";

    protected final File cacheDir;
    protected final File reserveCacheDir;

    protected final FileNameGenerator fileNameGenerator;
    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    protected Bitmap.CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
    protected int compressQuality = DEFAULT_COMPRESS_QUALITY;

    public BaseDiskCache(File cacheDir) {
        this(cacheDir, null);
    }

    public BaseDiskCache(File cacheDir, File reserveCacheDir) {
        this(cacheDir, reserveCacheDir, DefaultConfigurationFactory.createFileNameGenerator());
    }


    public BaseDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator) {
        if (cacheDir == null) {
            throw new IllegalArgumentException("cacheDir" + ERROR_ARG_NULL);
        }
        if (fileNameGenerator == null) {
            throw new IllegalArgumentException("fileNameGenerator" + ERROR_ARG_NULL);
        }

        this.cacheDir = cacheDir;
        this.reserveCacheDir = reserveCacheDir;
        this.fileNameGenerator = fileNameGenerator;
    }

    @Override
    public File getDirectory() {
        return cacheDir;
    }

    @Override
    public File get(String imageUri) {
        return getFile(imageUri);
    }

    protected File getFile(String imageUri) {
        String fileName = fileNameGenerator.generate(imageUri);
        File dir = cacheDir;
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            if (reserveCacheDir != null && (reserveCacheDir.exists() || reserveCacheDir.mkdirs())) {
                dir = reserveCacheDir;
            }
        }
        return new File(dir, fileName);
    }

    @Override
    public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
        File imageFile = getFile(imageUri);
        File tmpFile = new File(imageFile.getAbsolutePath() + TEMP_IMAGE_POSTFIX);
        boolean loaded = false;
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile), bufferSize);
            try {
                loaded = IoUtils.copyStream(imageStream, os, listener, bufferSize);
            } finally {
                IoUtils.closeSilently(os);
            }
        } finally {
            if (loaded && !tmpFile.renameTo(imageFile)) {
                loaded = false;
            }
            if (!loaded) {
                tmpFile.delete();
            }
        }
        return loaded;
    }

    @Override
    public boolean save(String imageUri, Bitmap bitmap) throws IOException {
        File imageFile = getFile(imageUri);
        File tmpFile = new File(imageFile.getAbsolutePath() + TEMP_IMAGE_POSTFIX);
        OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile), bufferSize);
        boolean savedSuccessfully = false;
        try {
            savedSuccessfully = bitmap.compress(compressFormat, compressQuality, os);
        } finally {
            IoUtils.closeSilently(os);
            if (savedSuccessfully && !tmpFile.renameTo(imageFile)) {
                savedSuccessfully = false;
            }
            if (!savedSuccessfully) {
                tmpFile.delete();
            }
        }
        bitmap.recycle();
        return savedSuccessfully;
    }

    @Override
    public boolean remove(String imageUri) {
        return getFile(imageUri).delete();
    }

    @Override
    public void close() {

    }

    @Override
    public void clear() {
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setCompressFormat(Bitmap.CompressFormat compressFormat) {
        this.compressFormat = compressFormat;
    }

    public void setCompressQuality(int compressQuality) {
        this.compressQuality = compressQuality;
    }
}
