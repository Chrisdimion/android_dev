# Universal-Image-Loader完全解析（上）
#### 基本介绍及使用
大家平时做项目的时候，或多或少都会接触到异步加载图片，或者大量加载图片的问题，而加载图片时候经常会遇到各种问题，如oom，图片加载混乱等。对于刚入门的新手来说，这些问题目前解决起来还比较因难，因此放多开源图片加载的框架就应运而生，其中**Universal-Image-Loader**就是里面的佼佼者。今天我们主要是针对这个开源框架进行解析，此框架的源码存在在Github上面，具体地址：[https://github.com/nostra13/Android-Universal-Image-Loader](https://github.com/nostra13/Android-Universal-Image-Loader)，我们可以先看看这个框架有哪些特点：

1. 多线程图片下载，图片可来自网络、项目文件夹assets中以及drawable中等
2. 支持随意配置ImageLoader，例如线程池，内存缓存策略，硬盘缓存策略等
3. 支持图片的内存缓存、文件缓存以及SD卡缓存等
4. 支持加载图片过程中的各种事件的监听
5. 能根据ImageView的大小对Bitmap进行裁剪，减少Bitmap占用过多的内存
6. 支持控制图片的加载过程，例如暂停图片加载、重新加载图片，一般在ListView或者	GridView等滑动时暂停加载图片，停止滑动时再去加载图片
7. 提供在较慢的网络下对图片进行加载
***
接下来我们进行这种开源框架的简单使用吧！
#### 添加Jar包
新建一个Android项目，并在上面的地址中下载框架项目的jar包，然后导入到libs工程目录下    
可以新建立一个MyApplication继承Application,并在OnCreate中创建ImageLoader的配置参数，并进行初始化：

	package com.example.myapplication;

	import android.app.Application;
	import com.nostra13.universalimageloader.core.ImageLoader;
	import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

	public class MyApplication extends Application {
    	@Override
   		public void onCreate() {
        	super.onCreate();
        	//创建默认的ImageLoader配置参数
        	ImageLoaderConfiguration configuration = 	ImageLoaderConfiguration.createDefault(this);
        	//初始化ImageLoader参数
        	ImageLoader.getInstance().init(configuration);
    	}
	}
	
ImageLoaderConfiguration是ImageLoader的配置参数，我们可以由代码看出其使用了单例模式和建造者模式，这里直接用createDefault()方法直接创建一个默认的ImageLoaderConfiguration，当然我们也可以直接设置自己的ImageLoaderConfiguration，设置如下
	
	File cacheDir = StorageUtils.getCacheDirectory(context);
	ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
        .memoryCacheExtraOptions(480, 800) // default = device screen dimensions
        .diskCacheExtraOptions(480, 800, CompressFormat.JPEG, 75, null)
        .taskExecutor(...)
        .taskExecutorForCachedImages(...)
        .threadPoolSize(3) // default
        .threadPriority(Thread.NORM_PRIORITY - 1) // default
        .tasksProcessingOrder(QueueProcessingType.FIFO) // default
        .denyCacheImageMultipleSizesInMemory()
        .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
        .memoryCacheSize(2 * 1024 * 1024)
        .memoryCacheSizePercentage(13) // default
        .diskCache(new UnlimitedDiscCache(cacheDir)) // default
        .diskCacheSize(50 * 1024 * 1024)
        .diskCacheFileCount(100)
        .diskCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
        .imageDownloader(new BaseImageDownloader(context)) // default
        .imageDecoder(new BaseImageDecoder()) // default
        .defaultDisplayImageOptions(DisplayImageOptions.createSimple()) // default
        .writeDebugLogs()
        .build();	
       
这里是所有的选项参数，我们在项目中根据自己需求去设置即可，一般来说，使用默认的createDefault()方法创建configuration创建即可。之后调用ImageLoader的init()方法将ImageLoaderConfiguration参数传递进去。
#### 进行Android Manifest配置

	<manifest>
    	<uses-permission android:name="android.permission.INTERNET" />
    	<!-- Include next permission if you want to allow UIL to cache imageson SD card -－>
    	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    	...
    	<application android:name="MyApplication">
        ...
    	</application>
	</manifest>
	
接下来我们可以进行加载图片了，首先我们可以定义好Activity的布局文件

	<?xml version="1.0" encoding="utf-8"?>
	<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    	android:layout_width="match_parent"
    	android:layout_height="match_parent">

    	<ImageView
        	android:id="@+id/image"
        	android:layout_width="wrap_content"
        	android:layout_height="wrap_content"
        	android:layout_gravity="center"
        	android:src="@drawable/ic_picture" />
	</FrameLayout>
	
里面只包含了一个ImageView控件，接下来我们去加载图片，我们可以观察ImageLoader项目源码可以看出，其提供了几个图片加载的方法，主要有**dispalyImage()、loadImage()、loadImageSync()，**其中，我们可以由方法名可以看出**loadImageSync()**方法是同步的，因为我们知道Android4.0版本以上有个特点，网络操作不能在主线程操作，因此，一般来说,**loadImageSync()**方法我们不去使用。
#### LoadImage()加载图片
我们可以使用ImageLoader的loadImage()方法来加载网络上的图片资源
	
	private void initData() {

        String imgUrl = "http://img4.imgtn.bdimg.com/it/u=1326316882,880909110&fm=21&gp=0.jpg";

        ImageLoader.getInstance().loadImage(imgUrl, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {
                //开始加载图片
            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                //图片加载失败
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                //图片加载完成
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
                //图片加载取消
            }
        });
    }
    
传入图片的Url地址和监听图片加载情况的ImageLoaderListener监听器，在回调方法的OnLoadingComplete()中将LoadImage()设置到所在的ImageView控件上就行了，如果你觉得传入的监听器中要实现的方法太多，那么也可以选择SimpleImageLoadingListener类，此类提供了ImageLoadingListener类中方法的实现，我们可根据需要选择实现的方法即可。
	
	ImageLoader.getInstance().loadImage(imgUrl,new SimpleImageLoadingListener(){
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
            }
    });
    
如果我们要指定图片的大小，则就应该在开始加载图片前指定一个ImageSize对象，并指定其宽高,之后再传入给loadImage()即可。

	ImageSize mImageSize = new ImageSize(100,100);
    ImageLoader.getInstance().loadImage(imgUrl,mImageSize,new SimpleImageLoadingListener(){
         @Override
         public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
              super.onLoadingComplete(imageUri, view, loadedImage);
            }
    });
    
上面只是简单的加载网络图片，但在实际的开发中，因为涉及到是否需要使用内存缓存、是否使用文件缓存等等。这时我们就会用到**DisplayImageOptions()**来配置这些相关图片的选项。具体如下

	DisplayImageOptions options = new DisplayImageOptions.Builder()
        .showImageOnLoading(R.drawable.ic_loading) // resource or drawable
        .showImageForEmptyUri(R.drawable.ic_empty) // resource or drawable
        .showImageOnFail(R.drawable.ic_fail) // resource or drawable
        .resetViewBeforeLoading(false)  // default
        .delayBeforeLoading(1000)
        .cacheInMemory(false) // default
        .cacheOnDisk(false) // default
        .preProcessor(...)
        .postProcessor(...)
        .extraForDownloader(...)
        .considerExifParams(false) // default
        .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2) // default
        .bitmapConfig(Bitmap.Config.ARGB_8888) // default
        .decodingOptions(...)
        .displayer(new SimpleBitmapDisplayer()) // default
        .handler(new Handler()) // default
        .build();
        
因此我们可以进行进一步对图片进行配置

	private void initData() {

        String imgUrl = "http://img4.imgtn.bdimg.com/it/u=1326316882,880909110&fm=21&gp=0.jpg";

        final ImageSize mImageSize = new ImageSize(100, 100);

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        ImageLoader.getInstance().loadImage(imgUrl, mImageSize,options, new SimpleImageLoadingListener(){
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                mImageView.setImageBitmap(loadedImage);
            }
        });
    }
    
我们使用了**DisplayImageOptions()**来配置图片的一些选项，上面的代码将图片进行内存缓存、硬盘缓存，这样我们就不用每次加载图片就从网络上加载。但是一些选项对于loadImage()方法是无效的，如showImageOnLoading()、showImageForEmptyUri()等。
#### DisplayImage()加载图片
我们也可以选用**DisplayImage()**方法来加载网络图片

	DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_loading)
                .showImageOnFail(R.drawable.ic_fail)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        
    ImageLoader.getInstance().displayImage(imgUrl, mImageView, options);
    
从上面可以看出，我们利用DisplayImageOptions()方法来加载图片更加方便快捷，省去了ImageLoadingListener接口的监听，也无需手动设置显示Bitmap对象，我们直接将ImageView对象作为参数传入到displayImage()即可，在配置的参数options中，我们设置了正在加载时显示的图片，以及图片加载错误时显示的图片。
我们在加载图片的过程中，我们有需要显示图片下载进度的需求,Universal-Image-Loader当然也有这样子的功能，我们只用在displayImage()方法中传入ImageLoadingProcessListener接口，如下所示：

	ImageLoader.getInstance().displayImage(imgUrl, mImageView, options, new SimpleImageLoadingListener(), new ImageLoadingProgressListener() {
           @Override
           public void onProgressUpdate(String imageUrl, View view, int current, int total) {
               //得到图片的加载进度并进行更新
           }
    });

#### 加载其他来源的图片
使用此项目框架不仅我们可以加载网络图片，还可以加载sd卡中的图片，ContentProvider等。使用时只需要将图片的地址Url修改一下就可以了。比如下面是加载文件系统上的图片代码

	DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_loading)
                .showImageOnFail(R.drawable.ic_fail)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    String imagePath = "/mnt/sdcard/image.png";
    String imagUrl = ImageDownloader.Scheme.FILE.wrap(imagePath);
    ImageLoader.getInstance().displayImage(imagUrl, mImageView, options);
	
对于不同的图片来源，我们只要在每个图片来源的地方加上Scheme包裹起来(Content provider)除外，然后当作图片的Url传给imageLoader中，之后项目框架就会根据不同的Scheme来获取相关的输入流

	//图片来源于Content Provider
	String contentProviderUrl = "content://media/external/audio/albumart/14";
	
	//图片来源于assets资源文件夹
	String assetsUrl = Scheme.ASSENTS.wrap("image.png");
	
	//图片来源于drawable文件夹
	String drawableUrl = Scheme.DRAWABLE.wrap("R.drawable.image");
	
#### ListView、GridView加载图片
我们一般情况下要展示大量的图片时都是采用GridView、ListView组件，而我们在这些组件上滚动时，我们可以停止图片的加载，而当停止滑动时，我们就可以加载当前界面上的图片。这个框架也提供这样的功能，使用起来也很简单，这提供了**PauseOnScrollListener**这个类来控制ListView或者GridView滑动过程中停止加载图片。
	
	listView.setOnScrollListener(new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling));
	gridView.setOnScrollListener(new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling));
	
其中第一个参数就是ImageLoader对象，第二个参数是控制是否在滑动过程中暂停加载图片，如果需要暂停则传入true,第三个参数是控制快速滑动界面时是否加载图片。

#### OutOfMemoryError
这个框架有很好的缓存机制，能够有效的避免OOM现象的产生，在这个框架中，对于OutOfMemoryError只是做了简单的捕获catch，从而保证我们的程序能够避免遇到OOM现象而不被crash掉，如果使用该框架经常发生OOM,则我们可以根据下面的配置进行改善。

* 尽量减少线程池中的个数，可以在初始化的ImageLoaderConfiguration中的threadPoolSize中配置，一般是1-5线程
* 减少图片内存消耗，在DisplayImageOptions选项中配置bitmapConfig为Bitmap.Config.RGB_565，默认是ARGB_8888，这样内存至少减少一半
* 可使用软引用，在ImageLoaderConfiguration中配置图片的内存缓存为memoryCache(new WeakMemoryCache())或者不使用内存缓存
* 设置图片的大小，可在DisplayImageOptions选项中设置.imageScaleType(ImageScaleType.IN_SAMPLE_INT)或者imageScaleType(imageScaleType.EXACTLY)

***
#### 总结
通过上面的对Universal-Image-Loader框架的学习，相信对于一些简单的运用也已经来说比较了解，在使用框架的时候尽量用displayImage()方法去加载图片，loadImage()是将图片对象进行回调到ImageLoadingListener接口的onLoadingComplete()方法，从而将图片资源设置到ImageView控件上。如果我们需要裁剪图片时，可以向LoadImage()方法传递一个ImageSize对象，而displayImage()则会根据ImageView控件设置的测量值来裁剪图片，其次，displayImage()方法中对ImageView对象使用的是Weak references,方便垃圾回收器回收。

今天的分享就到这里，接下来我会针对此框架讲解图片的缓存策略，希望各位继续关注。
 