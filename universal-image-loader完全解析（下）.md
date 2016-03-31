# Universal-Image-Loader完全解析（下）

在这篇文章中，我会继续跟大家分享有关于Universal-Image-Loader框架的相关知识，这次主要分享的是框架中图片缓存方面的知识。当然，如果对这个框架还完全不了解的话，可以先看看之前写的有关于这个框架的入门篇[Universal-Image-Loader完全解析（上）](http://www.cnblogs.com/Mz-Chris/p/4603409.html)。好了，言归正传，一般来说，我们去加载比较多的图片的时候，大多数情况下都会采用图片的缓存策略，而进行图片的缓存，我们又可以分为内存缓存与文件缓存（硬盘缓存），针对于内存缓存，有过项目经验的人都知道，大部分情况下是采用**LruCache**这个类，我们可以为这个**LruCache**设定规定的一个缓存图片的最大值，之后它就会自动去检测图片缓存总数是否达到我们设定的条件的最大值，当超过这个最大值后，**LruCache**类就会管理删除近期最少使用的图片。而作为一个以图片加载管理为主要功能的框架，它也自然提供了多种图片的缓存策略，接下来，我们就来谈谈框架中的这些缓存策略。

#### 内存缓存策略

首先，关于内存缓存，一定会涉及到的就是弱引用和强引用，现在我们来了解这两个概念：

* 弱引用：是通过weakReference类所实现的，它具有很强的不稳定性，一旦有垃圾回收器扫描到有着WeakReference的对象，就不管此对象如何，都会将其进行回收，以便来释放内存，它一般用来防止内存泄漏，保证内存被VM回收
* 强引用：是指创建一个新的对象时，我们将其赋值为一个引用变量，这时这个对象就是具有强引用性，强引用的的引用的变量指向时不会自动地被垃圾回收器回收。即使当内存不足的时候，手机宁愿发生OOM现象也不会被回收掉。一般来说，我们平时创建创建对象采用new的方式都是强引用

当然，还有软引用和虚引用的概念，既然谈到了，就顺便一起来了解了解吧

* 软引用：主要是通过SoftReference类所实现的，它具有较强的引用功能，只有当内存不足的时候，垃圾回收器会回收掉它所指向的对象，而内存足够时，通常是不会被回收的，一般多用来作实现Cache机制
* 虚引用：通过PhantomReference类所实现，它主要是用来跟踪对象被垃圾回收器回收的活动。一个对象持有虚引用，就相当于没有任何引用一样。而虚引用与软引用和弱引用的一个重要区别在于：虚引用必须和引用队列 （ReferenceQueue）联合使用。当垃圾回收器准备回收一个对象时，如果发现它还有虚引用，就会在回收对象的内存之前，把这个虚引用加入到与之关联的引用队列中。而程序可以通过判断引用队列中是否已经加入了虚引用，来了解被引用的对象是否将要被垃圾回收。程序如果发现某个虚引用已经被加入到引用队列，那么就可以在所引用的对象的内存被回收之前采取必要的行动

***

通过上面的讲解，相信各位朋友已经了解到了"四大引用"的基本概念。接下来，让我们来进入正题，来看看Universal-Image-Loader框架中存在着哪些内存缓存策略

**1. 仅使用强引用进行缓存**

* *LruMemoryCache*，这个类也是这个开源框架默认的内存缓存类，采用的是Lru算法进行缓存，缓存中使用的是bitmap的强引用

**2. 仅使用弱引用进行缓存**

* *WeakMemoryCache*，这个类缓存的bitmap的总大小是没有进行限制的，但不足的是不稳定，很容易被系统回收，当垃圾回收器一旦扫描到这个类缓存的对象bitmap，则会被回收掉

**3. 同时使用强引用和弱引用相结合**

* *UsingFreqLimitedMemoryCache*，这个类传入图片的总量最大值，当缓存的图片总量超过这个最大值，则会先删除使用频率最小的那个bitmap
* *FIFOLimitedMemoryCache*，这个类是采用先进先出的缓存策略，设定图片大小限定值后，当超过这个值时，会先删除最先入缓存的那个bitmap
* *LRULimitedMemoryCache*，这个类采用的也是Lru算法进行缓存，但与LruMemeoryCache不同的是，这个缓存使用的是bitmap的弱引用
* *LargestLimitedMemoryCache*，这个类传入图片总大小作为最大值，当超过缓存所设置的最大值时，会先删除最大的那个bitmap对象
* *LimitedAgeMemoryCache*，这个类传入的两个参数，第一个参数是MemoryCache类，第二个是以long类型的时间值，当我们加入缓存的bitmap时间超过我们所设定的值，就会将其进行删除

***

上面主要介绍的是Universal-Image-Loader框架中主要所提供的所有有关于图片内存缓存的相关类，当我们上面的类不满足项目需求的时候，这时我们也可以自定义使用自己写的内存缓存类，当写完自己的内存缓存类后，加入到我们的项目中，具体是需要配置ImageLoaderConfiguration.memoryCache(...)，比如我们自定义了一个缓存类，名为myWeakMemoryCache，则采取如下配置：

	ImageLoaderConfiguration configuration = new ImageLoaderConfiguration.Build(this)
		.memoryCache(new MyWeakMemoryCache())
		.build();
		
接下来，让我们来学习一下Universal-Image-Loader默认的框架缓存类*LruMemoryCache*的相关源码

	package com.nostra13.universalimageloader.cache.memory.impl;

	import android.graphics.Bitmap;
	import com.nostra13.universalimageloader.cache.memory.MemoryCache;
	import java.util.Collection;
	import java.util.HashSet;
	import java.util.LinkedHashMap;
	import java.util.Map.Entry;

	public class LruMemoryCache implements MemoryCache {
		//以LinkedHashMap容器来存储bitmap引用
    	private final LinkedHashMap<String, Bitmap> map;
    	
    	//定义缓存的限制值
    	private final int maxSize;
    	
    	//计算缓存的大小
    	private int size;

    	public LruMemoryCache(int maxSize) {
        	if(maxSize <= 0) {
         	   throw new IllegalArgumentException("maxSize <= 0");
        	} else {
           	 this.maxSize = maxSize;
            	 this.map = new LinkedHashMap(0, 0.75F, true);
        	}
    	}
		
		//若缓存中存在此bitmap，则返回，若没缓存，则将此bitmap插入到缓存中并返回null值
    	public final Bitmap get(String key) {
        	if(key == null) {
            	throw new NullPointerException("key == null");
        	} else {
            	synchronized(this) {
                	return (Bitmap)this.map.get(key);
            	}
        	}
    	}

		//将bitmap放入到LinkedHashMap对象容器内
    	public final boolean put(String key, Bitmap value) {
        	if(key != null && value != null) {
            	synchronized(this) {
                	this.size += this.sizeOf(key, value);
                	Bitmap previous = (Bitmap)this.map.put(key, value);
                	if(previous != null) {
                    	this.size -= this.sizeOf(key, previous);
                	}
            	}

            	this.trimToSize(this.maxSize);
            	return true;
        	} else {
            	throw new NullPointerException("key == null || value == null");
        	}
    	}

    	private void trimToSize(int maxSize) {
        	while(true) {
            	synchronized(this) {
                	if(this.size < 0 || this.map.isEmpty() && this.size != 0) {
                    	throw new IllegalStateException(this.getClass().getName() + ".sizeOf() is reporting inconsistent results!");
               	 }

                	if(this.size > maxSize && !this.map.isEmpty()) {
                		//移除容器中最先访问的bitmap
                    	Entry toEvict = (Entry)this.map.entrySet().iterator().next();
                    	if(toEvict != null) {
                        	String key = (String)toEvict.getKey();
                        	Bitmap value = (Bitmap)toEvict.getValue();
                        	this.map.remove(key);
                        	this.size -= this.sizeOf(key, value);
                        	continue;
                    	}
                	}

                	return;
            	}
        	}
    	}

    	public final Bitmap remove(String key) {
        	if(key == null) {
            	throw new NullPointerException("key == null");
        	} else {
            	synchronized(this) {
                	Bitmap previous = (Bitmap)this.map.remove(key);
                	if(previous != null) {
                    	this.size -= this.sizeOf(key, previous);
                	}

                	return previous;
            	}
        	}
    	}

    	public Collection<String> keys() {
        	synchronized(this) {
            	return new HashSet(this.map.keySet());
        	}
    	}

    	public void clear() {
        	this.trimToSize(-1);
    	}
		
		//缓存大小的计算方式
    	private int sizeOf(String key, Bitmap value) {
        	return value.getRowBytes() * value.getHeight();
    	}

    	public final synchronized String toString() {
        	return String.format("LruCache[maxSize=%d]", new Object[]{Integer.valueOf(this.maxSize)});
    	}
	}
	
从源码可以看出，此LruMemoryCache类缓存图片是由一个LinkedHashMap来进行维护的，要弄懂LruMemoryCache首先要分析一下LinkedHashMap这个类。一些刚入门的朋友可能还不了解这个类，他们可能会存在疑问，到底什么是LinkedHashMap，它又有哪些特点？下面就顺道简单分析一下，**已熟悉了解的朋友请直接忽略**。LinkedHashMap是HashMap的一个子类，它保留着插入的顺序，当我们需要输出的顺序和输入时的顺序相同，并且允许使用null键和null值时，就可考虑选用LinkedHashMap，该类默认情况下是按插入的顺序进行排序，当我们在此类构造函数中第三个参数传入true时，则会按访问顺序进行排序，最新访问的元素则会放在队尾。    
在此LruMemoryCache类的构造函数中，我们看到往其设置了一个缓存图片的最大值maxSize，并且实例化LinkedHashMap，而在LinkedHashMap构造函数中，第三个参数传入的是true，说明它内部是按照访问顺序进行排序的。   
我们接着看LruMemroyCache的**put(String key, Bitmap value)**方法，其方法中的**SizeOf()**计算的是每张图片所占有缓存大小，以byte为单位，而变量名**Size**是记录当前缓存的bitmap的总大小，如果我们当前已经将该key之前就已经缓存了bitmap，那就应该将之前缓存的bitmap的大小减掉，防止重复缓存相同的bitmap，接下来我们来看一看**trimToSize(int maxSize)**方法，方法中将当前缓存图片的总大小跟我们在构造函数中传入的限制值进行比较，若缓存的bitmap总数大小小于设定值，不作任何操作，直接返回；若大于设定值，则会获取LinkedHashMap容器中的第一个实体完素，并且在size中减掉该删除的bitmap对应的byte数。   
只要有一点Java基础，再通过认真分析源码，我们很容易了解到LruMemoryCache类的图片缓存逻辑。其次，系统也提供了其他图片的缓存逻辑，这里我也顺便简单的讲一下**LimitedAgeMemoryCache**类的实现逻辑，该类缓存图片是采用我们自己设置的继承于MemoryCache的缓存类，由构造函数第一个参数传入。其次，它使用是用HashMap来存储缓存图片时的时间值，当缓存的图片存在时间超过我们的设定值，就会在我们传入的缓存类中直接移除。至于其它的缓存策略比较相似，比如说**FIFOLimitedMemoryCache**类就是利用HashMap与LinkedList来实现先进先出的缓存策略，有兴趣的朋友们可以去看看。

#### 文件缓存策略（硬盘缓存）

我们知道，像一些新浪微博等需要加载很多图片的应用，本来图片的加载就已经很慢了，如果加载完再次打开时还需要继续下载上次加载的图片，相信很多用户会直接把它卸了，因为这样浪费的流量是巨大且无意义的。对于一个加载图片比较多的应用，一个好的硬盘缓存必不可少。辛运的是，Universal-Image-loader框架里正好提供了几个常见的硬盘缓存策略，接下来，让我们了解了解这几个常见的硬盘缓存类：

* *FileCountLimitedDiscCache*，这个类可以自己设置缓存的图片的个数，当超过我们所设置的值时，就会删除最先加入到硬盘的那些文件
* *LimitedAgeDiscCache*，这个类可以设置文件存活的最长时间，当文件中超过这个设定值时，就会删除这个文件
* *TotalSizeLimitedDiscCache*，这个类主要可以设置缓存图片总大小的最大值，当我们缓存的图片总大小超过这个值，就会删除最先加入到硬盘的文件
* *UnLimitedDiscCache*，这个缓存类比较特殊，它没有任何的限值，所以执行起来比任何硬盘缓存类都要快，框架默认是采用这个类，除非自己手动删除缓存的图片，不然图片缓存不会自动被删除。

其中，FileCountLimitedDiscCache与TotalSizeLimitedDiscCache这两个缓存类已经在最新的框架源码中删掉了，而加入了新的LruDiscCache类，如果大家想要了解，可以看看LruDiscCache相关源码

***

上面主要介绍的是Universal-image-loader框架的有关图片硬盘缓存的相关类。当然，当这些策略不满足我们的项目需求时候，我们也可以自定义自己的硬盘缓存类。  
下面就让我们来学习一下框架中默认的**UnLimitedDiscCache**类的源码实现逻辑   

	package com.nostra13.universalimageloader.cache.disc.impl;

	import com.nostra13.universalimageloader.cache.disc.impl.BaseDiskCache;
	import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
	import java.io.File;

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
	
由上面源代码可以知道，UnlimitedDiskCache是继承的是BaseDiskCache类，而自己这个类内部并没有实现自己什么独特的方法，也没有重写什么函数，只是在构造函数中传入参数，然后直接传给父类处理。接下来看一下其父类BaseDiskCache的源码

	package com.nostra13.universalimageloader.cache.disc.impl;

	import android.graphics.Bitmap;
	import android.graphics.Bitmap.CompressFormat;
	import com.nostra13.universalimageloader.cache.disc.DiskCache;
	import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
	import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
	import com.nostra13.universalimageloader.utils.IoUtils;
	import com.nostra13.universalimageloader.utils.IoUtils.CopyListener;
	import java.io.BufferedOutputStream;
	import java.io.File;
	import java.io.FileOutputStream;
	import java.io.IOException;
	import java.io.InputStream;

	public abstract class BaseDiskCache implements DiskCache {
		//默认的缓存大小
    	public static final int DEFAULT_BUFFER_SIZE = 32768;
    	//默认的文件压缩格式
    	public static final CompressFormat DEFAULT_COMPRESS_FORMAT;
    	//默认文件压缩质量
    	public static final int DEFAULT_COMPRESS_QUALITY = 100;
    	private static final String ERROR_ARG_NULL = " argument must be not null";
    	private static final String TEMP_IMAGE_POSTFIX = ".tmp";
    	protected final File cacheDir;
    	protected final File reserveCacheDir;
    	protected final FileNameGenerator fileNameGenerator;
    	protected int bufferSize;
    	protected CompressFormat compressFormat;
    	protected int compressQuality;

    	public BaseDiskCache(File cacheDir) {
        	this(cacheDir, (File)null);
    	}

    	public BaseDiskCache(File cacheDir, File reserveCacheDir) {
        	this(cacheDir, reserveCacheDir, DefaultConfigurationFactory.createFileNameGenerator());
    	}
		
		/**
		*@cacheDir，指的是文件的缓存目录
		*@reserveCacheDir，备用的文件缓存目录，可设为null，当cacheDir不可用时才用
		*@fileNameGenerator，文件名生成器，为缓存的文件生成文件名
		*/
    	public BaseDiskCache(File cacheDir, File reserveCacheDir, FileNameGenerator fileNameGenerator) {
        	this.bufferSize = '耀';
        	this.compressFormat = DEFAULT_COMPRESS_FORMAT;
        	this.compressQuality = 100;
        	if(cacheDir == null) {
            	throw new IllegalArgumentException("cacheDir argument must be not null");
        	} else if(fileNameGenerator == null) {
            	throw new IllegalArgumentException("fileNameGenerator argument must be not null");
        	} else {
            	this.cacheDir = cacheDir;
            	this.reserveCacheDir = reserveCacheDir;
            	this.fileNameGenerator = fileNameGenerator;
        	}
    	}

    	public File getDirectory() {
        	return this.cacheDir;
    	}
		
		//根据文件的Uri地址得到指向缓存目录所对应的文件
    	public File get(String imageUri) {
        	return this.getFile(imageUri);
    	}
		
								
    	public boolean save(String imageUri, InputStream imageStream, CopyListener listener) throws IOException {
    		//根据文件的Uri地址获取指向缓存目录中相对应的文件
        	File imageFile = this.getFile(imageUri);
        	//主要用来写入的临时文件
        	File tmpFile = new File(imageFile.getAbsolutePath() + ".tmp");
        	boolean loaded = false;

        	try {
            	BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile), this.bufferSize);

            	try {
            		//将文件写入到临时文件当中
                	loaded = IoUtils.copyStream(imageStream, os, listener, this.bufferSize);
            	} finally {
                	IoUtils.closeSilently(os);
            	}
        	} finally {
            	if(loaded && !tmpFile.renameTo(imageFile)) {
                	loaded = false;
            	}

            	if(!loaded) {
                	tmpFile.delete();
            	}

        	}

        	return loaded;
    	}

    	public boolean save(String imageUri, Bitmap bitmap) throws IOException {
        	File imageFile = this.getFile(imageUri);
        	File tmpFile = new File(imageFile.getAbsolutePath() + ".tmp");
        	BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile), this.bufferSize);
        	boolean savedSuccessfully = false;

        	try {
            	savedSuccessfully = bitmap.compress(this.compressFormat, this.compressQuality, os);
        	} finally {
            	IoUtils.closeSilently(os);
            	if(savedSuccessfully && !tmpFile.renameTo(imageFile)) {
                	savedSuccessfully = false;
            	}

            	if(!savedSuccessfully) {
                	tmpFile.delete();
            	}

        	}

        	bitmap.recycle();
        	return savedSuccessfully;
    	}

    	public boolean remove(String imageUri) {
        	return this.getFile(imageUri).delete();
    	}

    	public void close() {
    	}

    	public void clear() {
        	File[] files = this.cacheDir.listFiles();
        	if(files != null) {
            	File[] arr$ = files;
            	int len$ = files.length;

            	for(int i$ = 0; i$ < len$; ++i$) {
                	File f = arr$[i$];
                	f.delete();
            	}
        	}

    	}
		
		//根据文件的Uri地址生成一个指向缓存目录的文件
    	protected File getFile(String imageUri) {
        	String fileName = this.fileNameGenerator.generate(imageUri);
        	File dir = this.cacheDir;
        	if(!this.cacheDir.exists() && !this.cacheDir.mkdirs() && this.reserveCacheDir != null && (this.reserveCacheDir.exists() || this.reserveCacheDir.mkdirs())) {
            	dir = this.reserveCacheDir;
        	}

        	return new File(dir, fileName);
    	}

    	public void setBufferSize(int bufferSize) {
        	this.bufferSize = bufferSize;
    	}

    	public void setCompressFormat(CompressFormat compressFormat) {
        	this.compressFormat = compressFormat;
    	}

    	public void setCompressQuality(int compressQuality) {
        	this.compressQuality = compressQuality;
    	}

    	static {
        	DEFAULT_COMPRESS_FORMAT = CompressFormat.PNG;
    	}
	}
	
由代码可以分析得出，该类是一个抽象类，并实现了DiskCache接口，对于方法sava(String imageUri, Bitmap bitmap)同样是建立imageUri相应的的临时文件，这个临时文件是bitmap按照我们指定的图片格式与图片质量进行写入的，若写入不成功，直接删除临时文件。   
我们在使用过程中可以不自行配置硬盘缓存的策略，直接用**DefaultConfigurationFactory**方法也可以，如果我们在ImageLoaderConfiguration中配置了diskCacheSize和diskCacheFileCount，那就是使用了LruDiskCache，否则使用的默认的UnlimitedDiscCache

#### 总结
今天的分享也已经到达了尾声，有关于Universal-Image-loader框架到目前主要的部份已经分析完了，这个框架确实写得不错，自己读完代码也学到了很多。如果大家想真正了解这个框架，我希望可以坚持看完这两篇文章，我相信，读完后你肯定对图片缓存流程有了进一步的了解，针对自己的项目对这个框架的应用也更加会得心应手。当然，如果你觉得文章写得有哪些地方错误或者不明白的地方，欢迎留言交流，共同进步。  
**以后我会大概每个星期都会更新自己的博客，主要写的是技术上、工作上、生活中的点点滴滴，欢迎大家持续关注，谢谢！**


