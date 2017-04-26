# AndroidUMLoader
#AndroidUMLoader图片加载库
##2017/4/24
##一.设计图片SDK需要的东西
1.url
2.imageView
3.加载配置(占位图、错误图、是否缓存、加载图形(接口))  DisplayImageOptions 使用建造者模式
4.ImageLoadingListener 监听回调
5.ImageLoadingProgressListener 进度回调(接口分离)
6.下载器
7.解码器
8.缓存
9.线程池等配置项ImageLoaderConfiguration类

##二、新增aware包(防止内存泄露)
1.新增ImageAware接口，主要负责获取图片的宽高、是否被回收、得到自身图片、设置图片
2.新增ViewAware类(抽象类)，该类实现ImageAware接口
   <1>获取传递过来的ImageView将其包装成WeakReference<View>
   <2>在获取图片宽高的时候，先判断WeakReference<View>是否存在，然后通过getLayoutParams得到宽高
   <3>在复写setImageDrawable的时候要先判断是否在主线程，否则抛出异常
   <4>抽象setImageDrawableInto和setImageBitmapInto
3.新增ImageViewAware类,继承ViewAware类，再次复写得到宽高等方法，判断如果宽高如果等于0的时候，通过反射得到其最大值

##三、新增download包(下载类)
1.新增ImageDownloader接口，返回一个InputStream
2.新增BaseImageDownloader类，实现ImageDownloader接口，完成从网络、Assects、file、drawable的流
   <1>ContentLengthInputStream 是InputStream的装饰者，可通过available()函数得到 InputStream 对应数据源的长度(总字节数)。主要用于计算文件存储进度即图片下载进度时的总进度。
   <2>增加对网络、Assects、file、drawable获取流

##四、新增decode包(通过流解析成图片)
1.新增ImageDecoder接口，通过ImageDecodingInfo得到Bitmap对象
2.新增BaseImageDecoder类，实现ImageDecoder接口获取Bitmap
   <1>拿到流以后 defineImageSizeAndRotation 定义文件大小和获取图片旋转方向

   <2>重置流 ExifInfo保存了图片的一些旋转、翻转等信息,接下来因为已经读了一次流InputStream取到了图片大小信息,后面还要读流
      1.首先判断流是否支持标记mark,如果支持就可以调用reset()方法重置,默认的InputStream是不支持的,也就是只能读一次,输入管道内容就没了,但是当前这个流是
      ContentLengthInputStream这个包装对象,它重写了markSupported。
      2.退回到获取流的时候,最后ContentLengthInputStream包裹的是BufferedInputStream缓冲输入流,而她是支持reset()的
      当然如果包裹的是其它不支持标记的流,这里 往下执行,就要重新建立连接获得流对象了,getImageStream(decodingInfo)再次完成建立连接,获取Inputstream!

   <3>获取缩放比例Option中的Scale
      prepareDecodingOptions
      1.如果scaleType等于ImageScaleType.NONE，则缩放比例为 1；
      2.如果scaleType等于ImageScaleType.NONE_SAFE，则缩放比例为 (int)Math.ceil(Math.max((float)srcWidth / maxWidth, (float)srcHeight / maxHeight))；
      3.否则，调用ImageSizeUtils.computeImageSampleSize(…)计算缩放比例。
      在 computeImageSampleSize(…) 中
      4.如果viewScaleType等于ViewScaleType.FIT_INSIDE；
      1.1 如果scaleType等于ImageScaleType.IN_SAMPLE_POWER_OF_2，则缩放比例从 1 开始不断 *2 直到宽或高小于最大尺寸；
      1.2 否则取宽和高分别与最大尺寸比例中较大值，即Math.max(srcWidth / targetWidth, srcHeight / targetHeight)。
      5.如果scaleType等于ViewScaleType.CROP；
      2.1 如果scaleType等于ImageScaleType.IN_SAMPLE_POWER_OF_2，则缩放比例从 1 开始不断 *2 直到宽和高都小于最大尺寸。
      2.2 否则取宽和高分别与最大尺寸比例中较小值，即Math.min(srcWidth / targetWidth, srcHeight / targetHeight)。
      6.最后判断宽和高是否超过最大值，如果是 *2 或是 +1 缩放。

假设原图是1500x700的，我们给缩略图留出的空间是100x100的。那么inSampleSize=min(1500/100, 700/100)=7。
我们可以得到的缩略图是原图的1/7。这里如果你要问15:7的图片怎么显示到1:1的区域内，请去看ImageView的scaleType属性。
但是事实没有那么完美，虽然设置了inSampleSize=7，但是得到的缩略图却是原图的1/4，原因是inSampleSize只能是2的整数次幂，如果不是的话，向下取得最大的2的整数次幂，7向下寻找2的整数次幂，就是4。

   <3>considerExactScaleAndOrientatiton  根据参数将图片放大、翻转、旋转为合适的样子返回。

##五、新增cache包和memory包
1.新增MemoryCache接口，用于对内存缓存的增、删、查
2.新增BaseMemoryCache(抽象)类，实现MemoryCache接口 (基本的内存缓存)
  <1>通过保存 Map<String, Reference<Bitmap>> 将强引用保存为弱引用
  <2>抽象方法createReference
3.新增LimitedMemoryCache(抽象)类，继承BaseMemoryCache类(判断缓存值超过最大值的情况)
  <1>构造方法传入内存最大值
  <2>通过AtomicInteger 来时刻查询当然缓存的最大值
  <3>通过List<Bitmap>保存图片
  <4>在每次put的时候，判断当前内存+新增的一个图片的内存如果大于最大值，就从集合中删除Bitmap然后AtomicInteger减去第一次存放的bitmap的大小，否则添加集合、添加AtomicInteger，然后调用super添加进入
4.新增FIFOLimitedMemoryCache类，继承LimitedMemoryCache类(先进先出内存缓存类)
  <1>内部保存了一个集合队列
  <2>put的时候调用super.put如果成功了则添加到集合中
5.新增WeakMemoryCache类继承BaseMemoryCache类
  <1>实现了createReference方法，将Bitmap转化成WeakReference
6.新增LruMemoryCache类实现MemoryCache(Least recently used 最近最少使用算法这个是重点)
  <1>通过LinkedHashMap<String, Bitmap>来管理图片
  <2>LinkedHashMap accessOrder设为true以后，最近访问的数据将会被放到前面，
  <3>清理缓存，如果超出最大值，则需要删除老的数据

##六、新增naming包(文件命名)
1.新增FileNameGenerator接口 判断给文件设置名称
2.新增HashCodeFileNameGenerator类，实现FileNameGenerator接口

##七、新增disc包(硬盘缓存)
1.新增DiskCache接口 增、删、查
2.新增BaseDiskCache类，实现DiskCache接口
  <1>save的时候先创建了一个临时文件，当缓存完毕的时候通过重命名改成之前的文件renameto
  <2>clear的时候统一删除文件夹下的所有文件
3.新增LimitedAgeDiskCache类，继承BaseDiskCache(判断文件缓存过期时间)
  <1>save的时候修改文件的最后修改日期，然后放到Map里
  <2>get的时候先从Map中读取文件得到最后修改日期，如果当前时间减去最后修改时间大于设定的最大过期时间，就删除，同事移出Map中的对象
4.新增UnlimitedDiskCache类，不对缓存做出限制
5.新增LruDiskCache类，通过DiskLruCache类
限制总字节大小的内存缓存，会在缓存满时优先删除最近最少使用的元素，实现了DiskCache。
内部有个DiskLruCache cache属性，缓存的存、取操作基本都是由该属性代理完成。
6.新增DiskLruCache类
限制总字节大小的内存缓存，会在缓存满时优先删除最近最少使用的元素。
通过缓存目录下名为journal的文件记录缓存的所有操作，并在缓存open时读取journal的文件内容存储到LinkedHashMap<String, Entry> lruEntries中，后面get(String key)获取缓存内容时，会先从lruEntries中得到图片文件名返回文件。
LRU 的实现跟上面内存缓存类似，lruEntries为new LinkedHashMap<String, Entry>(0, 0.75f, true)，LinkedHashMap 第三个参数表示是否需要根据访问顺序(accessOrder)排序，true 表示根据accessOrder排序，最近访问的跟最新加入的一样放到最后面，
false 表示根据插入顺序排序。这里为 true 且缓存满时trimToSize()函数始终删除第一个元素，即始终删除最近最少访问的文件。

##八、新增assist包（助手包）
1.新增ContentLengthInputStream类
InputStream的装饰者，可通过available()函数得到 InputStream 对应数据源的长度(总字节数)。主要用于计算文件存储进度即图片下载进度时的总进度。
2.新增FailReason类
  图片下载及显示时的错误原因，目前包括：
  IO_ERROR 网络连接或是磁盘存储错误。
  DECODING_ERROR decode image 为 Bitmap 时错误。
  NETWORK_DENIED 当图片不在缓存中，且设置不允许访问网络时的错误。
  OUT_OF_MEMORY 内存溢出错误。
  UNKNOWN 未知错误。
3.新增FlushedInputStream类
  为了解决早期 Android 版本BitmapFactory.decodeStream(…)在慢网络情况下 decode image 异常的 Bug。
  主要通过重写FilterInputStream的 skip(long n) 函数解决，确保 skip(long n) 始终跳过了 n 个字节。
  如果返回结果即跳过的字节数小于 n，则不断循环直到 skip(long n) 跳过 n 字节或到达文件尾。
4.新增ImageScaleType类
  Image 的缩放类型，目前包括：
  NONE不缩放。
  NONE_SAFE根据需要以整数倍缩小图片，使得其尺寸不超过 Texture 可接受最大尺寸。
  IN_SAMPLE_POWER_OF_2根据需要以 2 的 n 次幂缩小图片，使其尺寸不超过目标大小，比较快的缩小方式。
  IN_SAMPLE_INT根据需要以整数倍缩小图片，使其尺寸不超过目标大小。
  EXACTLY根据需要缩小图片到宽或高有一个与目标尺寸一致。
  EXACTLY_STRETCHED根据需要缩放图片到宽或高有一个与目标尺寸一致。
5.新增ImageSize类
  表示图片宽高的类。
  scaleDown(…) 等比缩小宽高。
  scale(…) 等比放大宽高。
6.新增LoadedFrom包
  图片来源枚举类，包括网络、磁盘缓存、内存缓存。
7.新增 QueueProcessingType类
  任务队列的处理类型，包括FIFO先进先出、LIFO后进先出。
8.新增ViewScaleType类
  ImageAware的 ScaleType。
  将 ImageView 的 ScaleType 简化为两种FIT_INSIDE和CROP两种。FIT_INSIDE表示将图片缩放到至少宽度和高度有一个小于等于 View 的对应尺寸
  CROP表示将图片缩放到宽度和高度都大于等于 View 的对应尺寸。
##九、新增deque包（双端队列）
1.新增Deque接口
2.新增BlockingDeque接口
3.新增LIFOLinkedBlockingDeque类
  后进先出阻塞队列。重写LinkedBlockingDeque的offer(…)函数如下：
  @Override
  public boolean offer(T e) {
      return super.offerFirst(e);
  }
  让LinkedBlockingDeque插入总在最前，而remove()本身始终删除第一个元素，所以就变为了后进先出阻塞队列。
  实际一般情况只重写offer(…)函数是不够的，但因为ThreadPoolExecutor默认只用到了BlockingQueue的offer(…)函数，所以这种简单重写后做为ThreadPoolExecutor的任务队列没问题。
##十、新增StorageUtils类
  得到图片 SD 卡缓存目录路径。
  缓存目录优先选择/Android/data/[app_package_name]/cache；若无权限或不可用，则选择 App 在文件系统的缓存目录context.getCacheDir()；若无权限或不可用，则选择/data/data/[app_package_name]/cache。
  如果缓存目录选择了/Android/data/[app_package_name]/cache，则新建.nomedia文件表示不允许类似 Galley 这些应用显示此文件夹下图片。不过在 4.0 系统有 Bug 这种方式不生效。
##十一、新增display包（展示图片）
1.新增BitmapDisplayer接口
  void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom);
2.新增FadeInBitmapDisplayer类(图片渐隐类，实现了动画)，实现BitmapDisplayer接口
  复写display方法,给ImageView设置Bitmap,然后给View设置动画
3.新增SimpleBitmapDisplayer类，简单的图片加载类
  直接给ImageView设置Bitmap
4.新增CircleBitmapDisplayer类，加载圆形类
5.新增RoundedBitmapDisplayer类，加载圆角类
6.新增RoundedVignetteBitmapDisplayer类，加载圆角并且有影晕效果
##十二、新增ImageLoadingInfo类
  加载和显示图片任务需要的信息。
  String uri 图片 url。
  String memoryCacheKey 图片缓存 key。
  ImageAware imageAware 需要加载图片的对象。
  ImageSize targetSize 图片的显示尺寸。
  DisplayImageOptions options 图片显示的配置项。
  ImageLoadingListener listener 图片加载各种时刻的回调接口。
  ImageLoadingProgressListener progressListener 图片加载进度的回调接口。
  ReentrantLock loadFromUriLock 图片加载中的重入锁。
##十三、新增MemoryCacheUtils类
  内存缓存工具类。可用于根据 uri 生成内存缓存 key，缓存 key 比较，根据 uri 得到所有相关的 key 或图片，删除某个 uri 的内存缓存。
  generateKey(String imageUri, ImageSize targetSize)
  根据 uri 生成内存缓存 key，key 规则为[imageUri]_[width]x[height]。
##十四、新增FuzzyKeyMemoryCache类
  如果内存缓存不允许缓存一张图片的多个尺寸，则用FuzzyKeyMemoryCache做封装，同一个图片新的尺寸会覆盖缓存中该图片老的尺寸。
##十五、新增LoadAndDisplayImageTask类
  加载并显示图片的Task，实现了Runnable接口，用于从网络、文件系统或内存获取图片并解析，然后调用DisplayBitmapTask在ImageAware中显示图片。
  主要函数：
  (1) run()
  获取图片并显示，核心代码如下：
  bmp = configuration.memoryCache.get(memoryCacheKey);
  if (bmp == null || bmp.isRecycled()) {
      bmp = tryLoadBitmap();
      ...
      ...
      ...
      if (bmp != null && options.isCacheInMemory()) {
          L.d(LOG_CACHE_IMAGE_IN_MEMORY, memoryCacheKey);
          configuration.memoryCache.put(memoryCacheKey, bmp);
      }
  }
  ……
  DisplayBitmapTask displayBitmapTask = new DisplayBitmapTask(bmp, imageLoadingInfo, engine, loadedFrom);
  runTask(displayBitmapTask, syncLoading, handler, engine);

  从上面代码段中可以看到先是从内存缓存中去读取 bitmap 对象，若 bitmap 对象不存在，则调用 tryLoadBitmap() 函数获取 bitmap 对象，获取成功后若在 DisplayImageOptions.Builder 中设置了 cacheInMemory(true), 同时将 bitmap 对象缓存到内存中。
  最后新建DisplayBitmapTask显示图片。
  (2) tryLoadBitmap()
  从磁盘缓存或网络获取图片，核心代码如下：
  File imageFile = configuration.diskCache.get(uri);
  if (imageFile != null && imageFile.exists()) {
      ...
      bitmap = decodeImage(Scheme.FILE.wrap(imageFile.getAbsolutePath()));
  }
  if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
      ...
      String imageUriForDecoding = uri;
      if (options.isCacheOnDisk() && tryCacheImageOnDisk()) {
          imageFile = configuration.diskCache.get(uri);
          if (imageFile != null) {
              imageUriForDecoding = Scheme.FILE.wrap(imageFile.getAbsolutePath());
          }
      }
      checkTaskNotActual();
      bitmap = decodeImage(imageUriForDecoding);
      ...
  }
  首先根据 uri 看看磁盘中是不是已经缓存了这个文件，如果已经缓存，调用 decodeImage 函数，将图片文件 decode 成 bitmap 对象；
  如果 bitmap 不合法或缓存文件不存在，判断是否需要缓存在磁盘，需要则调用tryCacheImageOnDisk()函数去下载并缓存图片到本地磁盘，
  再通过decodeImage(imageUri)函数将图片文件 decode 成 bitmap 对象，否则直接通过decodeImage(imageUriForDecoding)下载图片并解析。
(3) tryCacheImageOnDisk()
  下载图片并存储在磁盘内，根据磁盘缓存图片最长宽高的配置处理图片。
      loaded = downloadImage();
  主要就是这一句话，调用下载器下载并保存图片。
  如果你在ImageLoaderConfiguration中还配置了maxImageWidthForDiskCache或者maxImageHeightForDiskCache，还会调用resizeAndSaveImage()函数，调整图片尺寸，并保存新的图片文件。
(4) downloadImage()
  下载图片并存储在磁盘内。调用getDownloader()得到ImageDownloader去下载图片。
(5) resizeAndSaveImage(int maxWidth, int maxHeight)
  从磁盘缓存中得到图片，重新设置大小及进行一些处理后保存。
(6) getDownloader()
  根据ImageLoaderEngine配置得到下载器。
  如果不允许访问网络，则使用不允许访问网络的图片下载器NetworkDeniedImageDownloader；如果是慢网络情况，则使用慢网络情况下的图片下载器SlowNetworkImageDownloader；否则直接使用ImageLoaderConfiguration中的downloader。
##十六、新增ProcessAndDisplayImageTask类
   处理并显示图片的Task，实现了Runnable接口。
   主要函数：
   (1) run()

   主要通过 imageLoadingInfo 得到BitmapProcessor处理图片，并用处理后的图片和配置新建一个DisplayBitmapTask在ImageAware中显示图片
##十七、新增ImageLoaderConfiguration
   ImageLoader的配置信息，包括图片最大尺寸、线程池、缓存、下载器、解码器等等。
   主要属性：
   (1). Resources resources
   程序本地资源访问器，用于加载DisplayImageOptions中设置的一些 App 中图片资源。
   (2). int maxImageWidthForMemoryCache

   内存缓存的图片最大宽度。
   (3). int maxImageHeightForMemoryCache

   内存缓存的图片最大高度。
   (4). int maxImageWidthForDiskCache

   磁盘缓存的图片最大宽度。
   (5). int maxImageHeightForDiskCache

   磁盘缓存的图片最大高度。
   (6). BitmapProcessor processorForDiskCache

   图片处理器，用于处理从磁盘缓存中读取到的图片。
   (7). Executor taskExecutor

   ImageLoaderEngine中用于执行从源获取图片任务的 Executor。
   (18). Executor taskExecutorForCachedImages

   ImageLoaderEngine中用于执行从缓存获取图片任务的 Executor。
   (19). boolean customExecutor

   用户是否自定义了上面的 taskExecutor。
   (20). boolean customExecutorForCachedImages

   用户是否自定义了上面的 taskExecutorForCachedImages。
   (21). int threadPoolSize

   上面两个默认线程池的核心池大小，即最大并发数。
   (22). int threadPriority

   上面两个默认线程池的线程优先级。
   (23). QueueProcessingType tasksProcessingType

   上面两个默认线程池的线程队列类型。目前只有 FIFO, LIFO 两种可供选择。
   (24). MemoryCache memoryCache

   图片内存缓存。
   (25). DiskCache diskCache

   图片磁盘缓存，一般放在 SD 卡。
   (26). ImageDownloader downloader

   图片下载器。
   (27). ImageDecoder decoder

   图片解码器，内部可使用我们常用的BitmapFactory.decode(…)将图片资源解码成Bitmap对象。
   (28). DisplayImageOptions defaultDisplayImageOptions

   图片显示的配置项。比如加载前、加载中、加载失败应该显示的占位图片，图片是否需要在磁盘缓存，是否需要在内存缓存等。
   (29). ImageDownloader networkDeniedDownloader

   不允许访问网络的图片下载器。
   (30). ImageDownloader slowNetworkDownloader

   慢网络情况下的图片下载器。
##十八、新增DefaultConfigurationFactory类
   为ImageLoaderConfiguration及ImageLoaderEngine提供一些默认配置。
   (1). createExecutor(int threadPoolSize, int threadPriority, QueueProcessingType tasksProcessingType)

   创建线程池。
   threadPoolSize表示核心池大小(最大并发数)。
   threadPriority表示线程优先级。
   tasksProcessingType表示线程队列类型，目前只有 FIFO, LIFO 两种可供选择。
   内部实现会调用createThreadFactory(…)返回一个支持线程优先级设置，并且以固定规则命名新建的线程的线程工厂类DefaultConfigurationFactory.DefaultThreadFactory。
   (2). createTaskDistributor()

   为ImageLoaderEngine中的任务分发器taskDistributor提供线程池，该线程池为 normal 优先级的无并发大小限制的线程池。
   (3). createFileNameGenerator()

   返回一个HashCodeFileNameGenerator对象，即以 uri HashCode 为文件名的文件名生成器。
   (4). createDiskCache(Context context, FileNameGenerator diskCacheFileNameGenerator, long diskCacheSize, int diskCacheFileCount)

   创建一个 Disk Cache。如果 diskCacheSize 或者 diskCacheFileCount 大于 0，返回一个LruDiskCache，否则返回无大小限制的UnlimitedDiskCache。
   (5). createMemoryCache(Context context, int memoryCacheSize)

   创建一个 Memory Cache。返回一个LruMemoryCache，若 memoryCacheSize 为 0，则设置该内存缓存的最大字节数为 App 最大可用内存的 1/8。
   这里 App 的最大可用内存也支持系统在 Honeycomb 之后(ApiLevel >= 11) application 中android:largeHeap="true"的设置。
   (6). createImageDownloader(Context context)

   创建图片下载器，返回一个BaseImageDownloader。
   (7). createImageDecoder(boolean loggingEnabled)

   创建图片解码器，返回一个BaseImageDecoder。
   (8). createBitmapDisplayer()

   创建图片显示器，返回一个SimpleBitmapDisplayer。
   DefaultConfigurationFactory.DefaultThreadFactory
   默认的线程工厂类，为
   DefaultConfigurationFactory.createExecutor(…)
   和
   DefaultConfigurationFactory.createTaskDistributor(…)
   提供线程工厂。支持线程优先级设置，并且以固定规则命名新建的线程。
   PS：重命名线程是个很好的习惯，它的一大作用就是方便问题排查，比如性能优化，用 TraceView 查看线程，根据名字很容易分辨各个线程。
##十九、新增DisplayImageOptions类
   图片显示的配置项。比如加载前、加载中、加载失败应该显示的占位图片，图片是否需要在磁盘缓存，是否需要在 memory 缓存等。
   主要属性及含义：
   (1). int imageResOnLoading

   图片正在加载中的占位图片的 resource id，优先级比下面的imageOnLoading高，当存在时，imageOnLoading不起作用。
   (2). int imageResForEmptyUri

   空 uri 时的占位图片的 resource id，优先级比下面的imageForEmptyUri高，当存在时，imageForEmptyUri不起作用。
   (3). int imageResOnFail

   加载失败时的占位图片的 resource id，优先级比下面的imageOnFail高，当存在时，imageOnFail不起作用。
   (4). Drawable imageOnLoading

   加载中的占位图片的 drawabled 对象，默认为 null。
   (5). Drawable imageForEmptyUri

   空 uri 时的占位图片的 drawabled 对象，默认为 null。
   (6). Drawable imageOnFail

   加载失败时的占位图片的 drawabled 对象，默认为 null。
   (7). boolean resetViewBeforeLoading

   在加载前是否重置 view，通过 Builder 构建的对象默认为 false。
   (8). boolean cacheInMemory

   是否缓存在内存中，通过 Builder 构建的对象默认为 false。
   (9). boolean cacheOnDisk

   是否缓存在磁盘中，通过 Builder 构建的对象默认为 false。
   (10). ImageScaleType imageScaleType

   图片的缩放类型，通过 Builder 构建的对象默认为IN_SAMPLE_POWER_OF_2。
   (11). Options decodingOptions;

   为 BitmapFactory.Options，用于BitmapFactory.decodeStream(imageStream, null, decodingOptions)得到图片尺寸等信息。
   (12). int delayBeforeLoading

   设置在开始加载前的延迟时间，单位为毫秒，通过 Builder 构建的对象默认为 0。
   (13). boolean considerExifParams

   是否考虑图片的 EXIF 信息，通过 Builder 构建的对象默认为 false。
   (14). Object extraForDownloader

   下载器需要的辅助信息。下载时传入ImageDownloader.getStream(String, Object)的对象，方便用户自己扩展，默认为 null。
   (15). BitmapProcessor preProcessor

   缓存在内存之前的处理程序，默认为 null。
   (16). BitmapProcessor postProcessor

   缓存在内存之后的处理程序，默认为 null。
   (17). BitmapDisplayer displayer

   图片的显示方式，通过 Builder 构建的对象默认为SimpleBitmapDisplayer。
   (18). Handler handler

   handler 对象，默认为 null。
   (19). boolean isSyncLoading

   是否同步加载，通过 Builder 构建的对象默认为 false。