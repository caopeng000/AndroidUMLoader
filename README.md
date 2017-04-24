# AndroidUMLoader
AndroidUMLoader图片加载库
2017/4/24
一.设计图片SDK需要的东西
1.url
2.imageView
3.加载配置(占位图、错误图、是否缓存、加载图形(接口))  DisplayImageOptions 使用建造者模式
4.ImageLoadingListener 监听回调
5.ImageLoadingProgressListener 进度回调(接口分离)

二、新增aware包(防止内存泄露)
1.新增ImageAware接口，主要负责获取图片的宽高、是否被回收、得到自身图片、设置图片
2.新增ViewAware类(抽象类)，该类继承ImageAware接口
   <1>获取传递过来的ImageView将其包装成WeakReference<View>
   <2>在获取图片宽高的时候，先判断WeakReference<View>是否存在，然后通过getLayoutParams得到宽高
   <3>在复写setImageDrawable的时候要先判断是否在主线程，否则抛出异常
   <4>抽象setImageDrawableInto和setImageBitmapInto
3.新增ImageViewAware类,继承ViewAware类，再次复写得到宽高等方法，判断如果宽高如果等于0的时候，通过反射得到其最大值

三、新增download包(下载类)
1.新增ImageDownloader接口，返回一个InputStream
2.新增BaseImageDownloader类，实现ImageDownloader接口，完成从网络、Assects、file、drawable的流
   <1>InputStream的装饰者，可通过available()函数得到 InputStream 对应数据源的长度(总字节数)。主要用于计算文件存储进度即图片下载进度时的总进度。
   <2>增加对网络、Assects、file、drawable获取流

四、新增decode包(通过流解析成图片)
1.新增ImageDecoder接口，通过ImageDecodingInfo得到Bitmap对象
2.新增BaseImageDecoder类，实现ImageDecoder接口获取Bitmap
   <1>拿到流以后 defineImageSizeAndRotation 定义文件大小和图片旋转方向

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

