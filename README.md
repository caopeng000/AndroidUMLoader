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


