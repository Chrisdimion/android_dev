### Android经验知识库

写这份知识库的原因，主要是记载了从学习和工作以来所遇到的知识点，方便以后查看回顾。此知识库会随着工作与学习不断更新。

* **Android Studio如何更新**

  单击F5(Step Over)，单行一个个方法执行  
  单击F6(Step Into)，单行执行  
  单击F7(Step Out)，不往下执行，回到上一行  
  单击F8(Resume Program)，跳出当前断点
    
* **警告栏设置相关按键颜色**

	setButtonTextColor(builder, builder.BUTTON_POSITIVE, R.color.mz_button_text_limegreen)


	    public static void setButtonTextColor(Dialog dialog, int whitchButton,int color){
        	try {
            	Method method = Class.forName("android.app.AlertDialog").getMethod(
                    "setButtonTextColor", new Class[] { int.class, int.class });
            	try {
                	method.invoke(dialog, whitchButton, color);
            	} catch (IllegalArgumentException | InvocationTargetException
            	  |IllegalAccessException e) {
                	e.printStackTrace();
            	}
        	} catch (SecurityException | ClassNotFoundException | NoSuchMethodException e) {
            	e.printStackTrace();
        	}
    	}
    	
* **Activity生命周期回调**

	当前Activity为Ａ，如果这里打开另一新的Activity B,则执行流程为 A.onPause ---> B.onCreate ---> B.onStart ---> B.onResume ---> A.onStop ---> A.Destory
	
* **AIDL中注意点**

	在跨进程通信时，Binder会把客户端传递过来的对象重新转化成新的一个对象，而我们可能采用观察者模式时，进行注册与解注册要注意使用的是同一个客户端对象，而进行Binder传递到服务端后，是产生两个全新的对象，因此不可由客户端传过去的对象解注册原先我们注册的对象，而此时应该采用**RemoteCallbackList**接口，这个是系统专门用来删除跨进程listener的接口的。
	

* **getParent().requestDisallowInterceptTouchEvent(true)**

	剥夺父View对touch事件的处理权，使父类设置onInterceptTouchEvent无效，正常到达子View。
	
* **ArgbEvaluator.evaluate(float fraction, Object startValue, Object endValue)**

	用于根据一个起始颜色值和一个结束颜色值以及一个偏移量生成一个新的颜色，分分钟实现类似于微信底部栏滑动颜色渐变。
	
* **Canvas中clipRect、clipPath和clipRegion 剪切区域的API**

* **Bitmap.extractAlpha()**

	返回一个新的Bitmap，capture原始图片的alpha 值。有的时候我们需要动态的修改一个元素的背景图片又不希望使用多张图片的时候，通过这个方法，结合Canvas 和Paint 可以动态的修改一个纯色Bitmap的颜色。
	
* **HandlerThread**

	代替不停new Thread 开子线程的重复体力写法。
	
* **IntentService**

	一个Service可以自己做完工作后，自动关闭自己的线程，不必要我们去控制管理关闭它。
	
* **WeakHashMap和SparseArray**

	直接使用HashMap有时候会带来内存溢出的风险，使用WaekHashMap实例化Map。当使用者不再有对象引用的时候，WeakHashMap将自动被移除对应Key值的对象，谷歌推荐的还有SparseArray代替ＨHashMap等，SparseArray内部实现了压缩算法，进行矩阵压缩，大大减少了存储空间，节约内存。
	
* **ImageSwitcher和ViewFlipper**

	ImageSwitcher，可用来做图片切换的一个类，类似于幻灯片；而ViewFlipper，实现多个view的切换(循环)，可自定义动画效果，且可针对单个切换指定动画。
	
* **AsyncQueryHandler**

	如果做系统工具类的开发，比如联系人短信辅助工具等，肯定免不了和ContentProvider打交道，如果数据量不是很大的情况下，随便搞，如果数据量大的情况下，可以去试着使用这个类。
	
* **PointF和Pair**

	graphics包中的一个类，我们经常见到在处理Touch事件的时候分别定义一个downX，一个downY用来存储一个坐标，如果坐标少还好，如果要记录的坐标过多那代码就不好看了，用PointF(float x, float y)，来描述一个坐标点会清楚很多，而android util包中的Pair类，可以方便的用来存储一”组”数据。
	
* **android:descendantFocusability**

	ListView的item中CheckBox等元素抢焦点导致item点击事件无法响应时，除了给对应的元素设置 focusable,更简单的是在item根布局加上android:descendantFocusability=”blocksDescendants”。
	
* **android:duplicateParentState=”true”**

	让子View跟随其Parent的状态，如pressed等。常见的使用场景是某些时候一个按钮很小，我们想要扩大其点击区域的时候通常会再给其包裹一层布局，将点击事件写到Parent上，这时候如果希望被包裹按钮的点击效果对应的Selector继续生效的话，这时候duplicateParentState就派上用场了。
	
* **includeFontPadding=”false”**

	TextView默认上下是有一定的padding的，有时候我们可能不需要上下这部分留白，加上它即可。
	
* **ViewConfiguration.getScaledTouchSlop()**

	触发移动事件的最小距离，自定义View处理touch事件的时候，有的时候需要判断用户是否真的存在movie，系统提供了这样的方法。
	
* **onTrimMemory**

	在Activity中重写此方法，会在内存紧张的时候回调（支持多个级别），便于我们主动的进行资源释放，避免OOM。
	
* **adapter.getPositionForSelection()和getSectionForPosition()**

	getPositionForSection()根据分类列的索引号获得该序列的首个位置；
getSectionForPosition()通过该项的位置，获得所在分类组的索引号。

* **获取arrt的值**

	不同主题下需要把颜色，数值写成attr属性，在xml里，我们可以简单的引用attr属性值。
		
		android:background="?attr/colorPrimary"
		
	代码获取
	
		TypedValue typedValue = new TypedValue();
		mContext.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
		int colorPrimary = typedValue.data;//value.data里面存储着的就是获取到的colorPrimary的值
		
* **ImageView.ScaleType**

    ImageView.ScaleType.center:图片位于视图中间，但不执行缩放。   
    ImageView.ScaleType.CENTER_CROP 按统一比例缩放图片（保持图片的尺寸比例）便于图片的两维（宽度和高度）等于或者大于相应的视图的维度。   
    ImageView.ScaleType.CENTER_INSIDE按统一比例缩放图片（保持图片的尺寸比例）便于图片的两维（宽度和高度）等于或者小于相应的视图的维度。   
    ImageView.ScaleType.FIT_CENTER缩放图片使用center。   
    ImageView.ScaleType.FIT_END缩放图片使用END。     
    ImageView.ScaleType.FIT_START缩放图片使用START。     
    ImageView.ScaleType.FIT_XY缩放图片使用XY。     
    ImageView.ScaleType.MATRIX当绘制时使用图片矩阵缩放。








	


	








	
	