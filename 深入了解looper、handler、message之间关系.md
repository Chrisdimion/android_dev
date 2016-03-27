# 深入了解Looper、Handler、Message之间关系

#### 前言及简介
上个星期我们整个项目组趁着小假期，驱车去了江门市的台山猛虎峡玩了两个多钟左右极限勇士全程漂流，感觉真得不错，夏天就应该多多玩水，多亲近一下大自然，不要整天埋头工作。刚回来时发现手因为抓了那个充气艇过久，现在都挺疼的。但是应该坚持自己上篇所说的，要保持每周的频度更新博文，上周没有时间写，这周一起补上，让朋友们一起相互分享学习，共同进步。   
好了，言归正题，今天我们要讲的主题是关于Android中的异步消息处理机制的内容。有一点Android基础的朋友们都知道，在Android中，主线程（也就是UI线程）是不安全的，当在主线程处理消息过长时，非常容易发生ANR（Application Not Responding）现象，这样对于用户体验是非常不好的；其次，如果我们在子线程中尝试进行UI的操作，程序就可能还会直接崩溃。我相信，对于大多刚入门的朋友，在日常工作当中会经常遇到这个问题，而解决的方法大多已经通过google已了解清楚，也就是在子线程中创建一个消息**Message**对象，然后利用在主线程中创建的**Handler**对象进行发送，之后我们可以在这个Handler对象的**handlerMessage()**方法中获取刚刚发送的**Message**对象，取出里面所存储的值，就可以在这里进行UI的操作。这种方法就称为异步消息处理线程。
总的来说，异步消息处理线程，说得比较通俗一点就是，当我们启动此方法后，会进入到一个无限的循环当中，每循环一次，我们就其对应的内部消息队列(Message Queue)中取出一个消息(Message)，然后回调好相应的消息处理函数，当执行完一个消息后则继续循环，若当消息队列中消息为空，则线程会被阻塞等待，直到有消息进入时再被唤醒。   
好吧，说了那么多，现在，就让我们来看一下这种处理机制的庐山真面目吧。

#### 分析Handler
首先我们来分析分析一下Handler的用法，我们知道，要创建一个Handler对象非常的简单明了，直接进行new一个对象即可，但是你有没有想过，这里会隐藏着什么注意点呢。现在可以试着写下面的一小段代码，然后自己运行看看：   
	
	public class MainActivity extends ActionBarActivity {

    	private Handler mHandler0;

    	private Handler mHandler1;

    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	setContentView(R.layout.activity_main);

        	mHandler0 = new Handler();
        	new Thread(new Runnable() {
            	@Override
            	public void run() {
                	mHandler1 = new Handler();
            	}
        	}).start();
    	}
    	
这一小段程序代码主要创建了两个Handler对象，其中，一个在主线程中创建，而另外一个则在子线程中创建，现在运行一下程序，则你会发现，在子线程创建的Handler对象竟然会导致程序直接崩溃，提示的错误是**Can't create handler inside thread that has not called Looper.prepare()**
    	

于是我们按照logcat中所说，在子线程中加入Looper.prepare(),即代码如下：

	new Thread(new Runnable(){
		@override
		public void run(){
			Looper.prepare();
			mHandler1 = new Handler()l
		}
	}).start();
	
再次运行一下程序，发现程序不会再崩溃了，可是，单单只加这句Looper.prepare()是否就能解决问题了。我们探讨问题，就要知其然，才能了解得更多。我们还是先分析一下源码吧，看看为什么在子线程中没有加Looper.prepare()就会出现崩溃，而主线程中为什么不用加这句代码？我们看下Handler()构造函数：

	public Handler() {
        this(null, false);
    }
    
构造函数直接调用**this(null, false)**，于是接着看其调用的函数，

	    public Handler(Callback callback, boolean async) {
        	if (FIND_POTENTIAL_LEAKS) {
            	final Class<? extends Handler> klass = getClass();
            	if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                	Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            	}
        	}

        	mLooper = Looper.myLooper();
        	if (mLooper == null) {
            	throw new RuntimeException(
                	"Can't create handler inside thread that has not called Looper.prepare()");
        	}
        	mQueue = mLooper.mQueue;
        	mCallback = callback;
        	mAsynchronous = async;
    	}
不难看出，源码中调用了**mLooper = Looper.myLooper()**方法获取一个Looper对象，若此时Looper对象为null，则会直接抛出一个“Can't create handler inside thread that has not called Looper.prepare()”异常，那什么时候造成mLooper是为空呢？那就接着分析**Looper.myLooper()**，

	   public static Looper myLooper() {
        	return sThreadLocal.get();
       }
       
这个方法在sThreadLocal变量中直接取出Looper对象，若sThreadLocal变量中存在Looper对象，则直接返回，若不存在，则直接返回null，而sThreadLocal变量是什么呢？
	
	static final ThreadLocat<Looper> sThreadLocal = new ThreadLocal<Looper>();

它是本地线程变量，存放在Looper对象，由这也可看出，每个线程只有存有一个Looper对象，可是，是在哪里给sThreadLocal设置Looper的呢，通过前面的试验，我们不难猜到，应该是在Looper.prepare()方法中，现在来看看它的源码：
	
    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }
    
由此看到，我们的判断是正确的，在Looper.prepare()方法中给sThreadLocal变量设置Looper对象，这样也就理解了为什么要先调用Looper.prepare()方法，才能创建Handler对象，才不会导致崩溃。但是，仔细想想，为什么主线程就不用调用呢？不要急，我们接着分析一下主线程，我们查看一下ActivityThread中的main()方法，代码如下：

    public static void main(String[] args) {
        SamplingProfilerIntegration.start();

        // CloseGuard defaults to true and can be quite spammy.  We
        // disable it here, but selectively enable it later (via
        // StrictMode) on debug builds, but using DropBox, not logs.
        CloseGuard.setEnabled(false);

        Environment.initForCurrentUser();

        // Set the reporter for event logging in libcore
        EventLogger.setReporter(new EventLoggingReporter());

        Security.addProvider(new AndroidKeyStoreProvider());

        // Make sure TrustedCertificateStore looks in the right place for CA certificates
        final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
        TrustedCertificateStore.setDefaultUserDirectory(configDir);

        Process.setArgV0("<pre-initialized>");

        Looper.prepareMainLooper();

        ActivityThread thread = new ActivityThread();
        thread.attach(false);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }

        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }

        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
    
代码中调用了Looper.prepareMainLooper()方法，而这个方法又会继续调用了Looper.prepare()方法，代码如下：

    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }
    
分析到这里已经真相大白，主线程中google工程师已经自动帮我们创建了一个Looper对象了，因而我们不再需要手动再调用Looper.prepare()再创建，而子线程中，因为没有自动帮我们创建Looper对象，因此需要我们手动添加，调用方法是Looper.prepare()，这样，我们才能正确地创建Handler对象。

#### 发送消息
当我们正确的创建Handler对象后，接下来我们来了解一下怎么发送消息，有一点基础的朋友肯定对这个方法已经了如指掌了。具体是先创建出一个Message对象，然后可以利用一些方法，如setData()或者使用arg参数等方式来存放数据于消息中，再借助Handler对象将消息发送出去就可以了。

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = Message.obtain();
                msg.arg1 = 1;
                msg.arg2 = 2;
                Bundle bundle = new Bundle();
                bundle.putChar("key", 'v');
                bundle.putString("key","value");
                msg.setData(bundle);
                mHandler0.sendMessage(msg);
            }
        }).start();
        
通过Message对象进行传递消息，在消息中添加各种数据，之后再消息通过mHandler0进行传递，之后我们再利用Handler中的handleMessage()方法将此时传递的Message进行捕获出来，再分析得到存储在msg中的数据。但是，这个流程到底是怎么样的呢？具体我们还是来分析一下源码。首先分析一下发送方法**sendMessage()**:

	 public final boolean sendMessage(Message msg)
    {
        return sendMessageDelayed(msg, 0);
    }

通过调用**sendMessageDelayed(msg, 0)**方法

    public final boolean sendMessageDelayed(Message msg, long delayMillis)
    {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
    }
    
再能过调用**sendMessageDelayed(Message msg, long delayMillis)**,方法中第一个参数是指发送的消息msg,第二个参数是指延迟多少毫秒发送，我们着重看一下此方法：

    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                    this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, uptimeMillis);
    }
    
由这里可以分析得出，原来消息Message对象是建立一个消息队列**MessageQueue**,这个对象MessageQueue由**mQueue**赋值，而由源码分析得出**mQueue = mLooper.mQueue**，而**mLooper**则是Looper对象，我们由上面已经知道，每个线程只有一个Looper，因此，一个Looper也就对应了一个MessageQueue对象，之后调用**enqueueMessage(queue, msg, uptimeMillis)**直接入队操作：

    private boolean enqueueMessage(MessageQueue queue, Message msg, long uptimeMillis) {
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
    
方法通过调用MessageQueue对**enqueueMessage(Message msg, long uptimeMills)**方法：

    boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }

        synchronized (this) {
            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w("MessageQueue", e.getMessage(), e);
                msg.recycle();
                return false;
            }

            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // Inserted within the middle of the queue.  Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }
    
首先要知道，源码中用mMessages代表当前等待处理的消息，MessageQueue也没有使用一个集合保存所有的消息。观察中间的代码部分，队列中根据时间**when**来时间排序，这个时间也就是我们传进来延迟的时间uptimeMills参数，之后再根据时间的顺序调用**msg.next**,从而指定下一个将要处理的消息是什么。如果只是通过**sendMessageAtFrontOfQueue()**方法来发送消息

    public final boolean sendMessageAtFrontOfQueue(Message msg) {
        MessageQueue queue = mQueue;
        if (queue == null) {
            RuntimeException e = new RuntimeException(
                this + " sendMessageAtTime() called with no mQueue");
            Log.w("Looper", e.getMessage(), e);
            return false;
        }
        return enqueueMessage(queue, msg, 0);
    }
    
它也是直接调用**enqueueMessage()**进行入队，但没有延迟时间，此时会将传递的此消息直接添加到队头处，现在入队操作已经了解得差不多了，接下来应该来了解一下出队操作，那么出队在哪里进行的呢，不要忘记**MessageQueue**对象是在Looper中赋值，因此我们可以在Looper类中找，来看一看Looper.loop()方法：

    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        final MessageQueue queue = me.mQueue;

        // Make sure the identity of this thread is that of the local process,
        // and keep track of what that identity token actually is.
        Binder.clearCallingIdentity();
        final long ident = Binder.clearCallingIdentity();

        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            // This must be in a local variable, in case a UI event sets the logger
            Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + " " +
                        msg.callback + ": " + msg.what);
            }

            msg.target.dispatchMessage(msg);

            if (logging != null) {
                logging.println("<<<<< Finished to " + msg.target + " " + msg.callback);
            }

            // Make sure that during the course of dispatching the
            // identity of the thread wasn't corrupted.
            final long newIdent = Binder.clearCallingIdentity();
            if (ident != newIdent) {
                Log.wtf(TAG, "Thread identity changed from 0x"
                        + Long.toHexString(ident) + " to 0x"
                        + Long.toHexString(newIdent) + " while dispatching to "
                        + msg.target.getClass().getName() + " "
                        + msg.callback + " what=" + msg.what);
            }

            msg.recycleUnchecked();
        }
    }
    
代码比较多，我们只挑重要的分析一下，我们可以看到下面的代码用**for(;;)**进入了一个死循环，之后不断的从MessageQueue对象queue中取出消息msg，而我们不难知道，此时的next()就是进行队列的出队方法，**next()**方法代码有点长，有兴趣的话可以自行翻阅查看，主要逻辑是判断当前的MessageQueue是否存在待处理的mMessages消息，如果有，则将这个消息出队，然后让下一个消息成为mMessages，否则就进入一个阻塞状态，一直等到有新的消息入队唤醒。回看loop()方法，可以发现当执行next()方法后会执行**msg.target.dispatchMessage(msg)**方法，而不难看出，此时msg.target就是Handler对象，继续看一下**dispatchMessage()**方法：

    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }
   
先进行判断mCallback是否为空，若不为空则调用mCallback的handleMessage()方法，否则直接调用handleMessage()方法，并将消息作为参数传出去。这样我们就完全一目了然，为什么我们要使用handleMessage()来捕获我们之前传递过去的信息。       
现在我们根据上面的理解，不难写出异步消息处理机制的线程了。

    class myThread extends Thread{
        public Handler myHandler;

        @Override
        public void run() {
            Looper.prepare();
            myHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    //处理消息
                }
            };
           Looper.loop();
        }
    }

当然除了发送消息外，还有以下几个方法可以在子线程中进行UI操作：

* View的post()方法
* Handler的post()方法
* Activity的runOnUiThread()方法

其实这几个方法的本质都是一样的，只要我们勤于查看这几个方法的源码，不难看出最后调用的也是Handler机制，也是借用了异步消息处理机制来实现的。

#### 总结
通过上面对异步消息处理线程的讲解，我们不难真正地理解到了Handler、Looper以及Message之间的关系，概括性来说，Looper负责的是创建一个MessageQueue对象，然后进入到一个无限循环体中不断取出消息，而这些消息都是由一个或者多个Handler进行创建处理。    
**接下来朋友们想要了解哪方面的东西或者有什么好的想法，可以在下面留言交流，我会尽自己的能力选择分享给朋友们，当然，如果有什么分享错误或者不懂的地方，可以相互交流，期待每个朋友在我的博文中都能学到东西，如果觉得好的话，麻烦各位兄弟关注一下，谢谢！！**














