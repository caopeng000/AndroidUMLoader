package com.androidumloader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/******************************************
 * 类名称：ImageLoaderEngine
 * 类描述：
 *
 * @version: 2.3.1
 * @author: caopeng
 * @time: 2017/4/25 11:10
 ******************************************/
public class ImageLoaderEngine {
    final ImageLoaderConfiguration configuration;
    private Executor taskExecutor;
    private Executor taskExecutorForCachedImages;
    private Executor taskDistributor;
    private final Map<Integer, String> cacheKeysForImageAwares = Collections
            .synchronizedMap(new HashMap<Integer, String>());
    private final Map<String, ReentrantLock> uriLocks = new WeakHashMap<>();
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean networkDenied = new AtomicBoolean(false);
    private final AtomicBoolean slowNetwork = new AtomicBoolean(false);

    private final Object pauseLock = new Object();

    ImageLoaderEngine(ImageLoaderConfiguration configuration) {
        this.configuration = configuration;

        taskExecutor = configuration.taskExecutor;
        taskExecutorForCachedImages = configuration.taskExecutorForCachedImages;

        taskDistributor = DefaultConfigurationFactory.createTaskDistributor();
    }
}
