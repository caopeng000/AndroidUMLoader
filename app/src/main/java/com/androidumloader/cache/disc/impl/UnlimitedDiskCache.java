package com.androidumloader.cache.disc.impl;

import com.androidumloader.cache.disc.naming.FileNameGenerator;

import java.io.File;

/******************************************
 * 类名称：UnlimitedDiskCache
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/25 10:11
 ******************************************/
public class UnlimitedDiskCache extends BaseDiskCache {

    public UnlimitedDiskCache(File cacheDir) {
        super(cacheDir);
    }

    public UnlimitedDiskCache(File cacheDir, File reserveCacheDir) {
        super(cacheDir, reserveCacheDir);
    }

    public UnlimitedDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator) {
        super(cacheDir, reserveCacheDir, fileNameGenerator);
    }
}
