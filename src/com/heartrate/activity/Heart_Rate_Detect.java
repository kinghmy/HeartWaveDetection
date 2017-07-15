package com.heartrate.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


//import图表处理类
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


//import自己的工具类
import com.tools_set.trans_img2red.ImageProcessing;
import com.tools_set.FFTtrans.Compute_Result;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.hardware.Camera;
import android.hardware.Camera.*;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

public class Heart_Rate_Detect extends Activity {
	private Timer timer = new Timer(); // 一种工具，线程用其安排以后在后台线程中执行的任务。可安排任务执行一次，或者定期重复执行
	private Timer timer_fft = new Timer();
	private TimerTask task; // 计时器实例task
	private TimerTask task_fft;
	private static double gx; // 当前画面内平均红色分量值
	private MyHandler handler; // handler实例
	private MyHandler handle_fft;
	private MyHandler handle_exit;
	private static String title = "Pulse"; // 表格标题
	private XYSeries series; // pause 曲线系列数据
	private XYMultipleSeriesDataset mDataset; // 容纳series数据系列的数据集
	private GraphicalView chart; // 图表工厂
	private static XYMultipleSeriesRenderer renderer; // 渲染绘制的曲线
	private static Context context; // 应用上下文
	private int addX = -1;
	double addY;
	static int length = 64; // 一组离散数值的长度
	double[] xv = new double[length]; // 构造新点集的x值
	double[] yv = new double[length]; // 构造新点集的y值
	static double sigl_tem[] = new double[length]; // 暂存一组时域离散信号
	static double SNratio = 0; // 离散信号变换得到的信噪比
	boolean button_flag = false; // 按钮状态标志量

	private static final AtomicBoolean processing = new AtomicBoolean(false);
	/**
	 * 取了一个原子变量processing，指示当前程序的处理状态是否正确。AtomicBoolean 可以用原子方式更新的 boolean 值
	 */
	private static SurfaceView preview = null; // Android手机预览控件
	private static SurfaceHolder previewHolder = null;
	// 预览设置信息
	private static Camera camera = null; // Android手机相机句柄
	private static TextView info_heart_rate = null;
	private static TextView img_average = null;
	private static TextView num_pulse = null;
	private static Button Light = null; // 开关闪光灯
	private static Button Jump = null; // 跳转按钮
	private static WakeLock wakeLock = null;
	private static int averageIndex = 0;
	private static final int averageArraySize = 8;
	private static final double[] averageArray = new double[averageArraySize];// 保存最近averageArray个画面平均红色分量值

	/**
	 * 类型枚举
	 * 
	 * 定义枚举类型来标识曲线的上升或下降状态
	 * 
	 * @author KINGHMY 颜色类型，我在这里用一个枚举类型来定义，这个枚举类型很简单，
	 *         只有两种颜色，一种是绿色，代表上升，一种是红色，代表下降趋势。
	 */
	public static enum TYPE {
		GREEN, RED
	};

	private static TYPE currentType = TYPE.GREEN; // 设置默认类型，为上升趋势

	public static TYPE getCurrent() { // 获取当前类型
		return currentType;
	}

	private static int beatsIndex = 0; // 心跳下标值
	private static final int beatsArraySize = 2; // 心跳数组的大小
	private static final int[] beatsArray = new int[beatsArraySize];// 心率数组，存储最近beatsArraySize个生成的心率数值
	// 心跳数组
	private static double beats = 0; // 心跳脉冲
	private static long startTime = 0; // 开始时间
	private static int count_fft = 0; // fft计算计数

	private static boolean isExit = false; // 定义一个变量，来标识是否退出
	private static Toast toast; // 公用Toast变量

	static class MyHandler extends Handler {
		// 内部类，静态handler 避免内存泄露发生
		WeakReference<Activity> mActivityReference;

		MyHandler(Activity activity) {
			mActivityReference = new WeakReference<Activity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/**
		 * 初始化配置
		 * 
		 * 总体上的功能是初始化程序的各个配置，包括调用其他方法，例如页面图表的初始化，
		 * UI控件的初始化，应用程序启动后显示的样式，调用相机，通过Handler接收其他方
		 * 法传递过来的消息信息来更新UI，，等等。主要是实现应用的配置功能，同时相当于一
		 * 个应用程序的管家，它来直接或间接的调用其他方法，来使整个应用程序顺利运行起来。
		 */
		context = getApplicationContext();
		/*
		 * 比如有一个全局的数据操作类,用到了context, 这个时候就要用到 getApplicationContext ,
		 * 而不是用ACtivity, 这就保证了, 数据库的操作与activity无关.
		 */
		// 这里获得main界面上的心电图画布布局，下面会把心电图表画在这个布局里面
		LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
		LinearLayout textareas = (LinearLayout) findViewById(R.id.textareas);
		Light = (Button) findViewById(R.id.light);
		Jump = (Button) findViewById(R.id.jump);
		button_flag = false;
		Light.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Camera.Parameters mParameters = camera.getParameters();
				List<String> flashModes = mParameters.getSupportedFlashModes();
				// Check if camera flash exists
				if (flashModes == null) {
					// Use the screen as a flashlight (next best thing)
					return;
				}
				String flashMode = mParameters.getFlashMode();
				if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
					// Turn on the flash
					if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
						mParameters.setFlashMode(Parameters.FLASH_MODE_TORCH);

					}
				} else {
					mParameters.setFlashMode(Parameters.FLASH_MODE_OFF);
				}
				camera.setParameters(mParameters);

			}

		});

		Jump.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				 * 点击后将暂存的时间序列数组发送给新的activity，然后当前活动over
				 */
				if(button_flag == true){
					Intent intent = new Intent(Heart_Rate_Detect.this, Deep_Comp.class);  
					  
					/* 通过Bundle对象存储需要传递的数据 */  
					Bundle bundle = new Bundle();
					
					/*public void putDoubleArray (String key, double[] value)
					 * 
					 *Added in API level 1
					 *Inserts a double array value into the mapping of this Bundle, 
					 *replacing any existing value for the given key. 
					 *Either key or value may be null.
					 *
					 *Parameters
					 *key	a String, or null
					 *value	a double array object, or null
					 */
					bundle.putDoubleArray("sigl_array", sigl_tem);
					
					/*把bundle对象assign给Intent*/  
					intent.putExtras(bundle);  
					  
					startActivity(intent);
				}
			}

		});

		// 这个类用来放置一个系列曲线上的所有点，是一个点的集合，根据这些点画出曲线，构造方法参数只是系列名称
		series = new XYSeries(title);

		// 创建一个多系列数据集的实例，这个数据集将被用来创建图表，数据集可以包含0-多个数据系列
		mDataset = new XYMultipleSeriesDataset();

		// 将serial点集添加到这个数据集中
		mDataset.addSeries(series);

		// 以下都是曲线的样式和属性等等的设置，renderer相当于一个用来给图表做渲染的句柄
		int color = Color.RED;
		PointStyle style = PointStyle.CIRCLE;
		/**
		 * 若此处style赋值为null会导致preview无法加载，然后mainactivity崩掉，血与累的教训
		 */

		// 主类中实现的一个保护方法，设置图表颜色和风格
		renderer = buildRenderer(color, style, true);

		// 主类中实现的一个保护方法,设置好图表的样式
		setChartSettings(title,renderer, "Time", "mmHg", 0, 63, -0.8, 0.5,
				Color.WHITE, Color.WHITE);

		// 生成图表
		chart = ChartFactory.getLineChartView(context, mDataset, renderer);

		// 将图表添加到布局中去
		layout.addView(chart, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		/**
		 * 创建一个Handler实例handler
		 * 
		 * 这里的Handler实例将配合下面的Timer实例，完成定时更新图表的功能
		 */
		handler = new MyHandler(this) {
			@Override
			public void handleMessage(Message msg) {
				// 刷新图表
				updateChart(); // 更新图表
				super.handleMessage(msg);
			}
		};

		handle_fft = new MyHandler(this) {
			@Override
			public void handleMessage(Message msg) {

				if (msg.what >= 16 || SNratio > 0.8) {
					button_flag = true;
					Jump.setText("Finish compute!");
					
					toast = Toast.makeText(Heart_Rate_Detect.this,
							"请点击跳转按钮查看分析结果！", Toast.LENGTH_SHORT);
					toast.show();
				} else {
					Compute_Result result = new Compute_Result();
					
					double Discrete_clone[] = yv.clone();// 将当前的一组离散值保存起来

					double ratio = result.getSNR(Discrete_clone);
					
					if (ratio > SNratio) {
						SNratio = ratio;
						sigl_tem = Discrete_clone.clone();
					}
					count_fft++;
				}

				super.handleMessage(msg);
			}
		};

		handle_exit = new MyHandler(this) {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				isExit = false;
			}
		};// 按两次退出的一个handle

		/**
		 * 创建一个计时器实例task
		 * 
		 * public abstract void run() 此计时器任务要执行的操作
		 */
		task = new TimerTask() {
			@Override
			public void run() {
				// Message message = new Message(); //获取一个Message对象
				Message message = handler.obtainMessage();
				message.what = 1; // 只能放数字（作用可以使用来做if判断）
				handler.sendMessage(message); // 发送消息
			}
		};

		task_fft = new TimerTask() {
			@Override
			public void run() {
				
				// Message message = new Message(); //获取一个Message对象
				Message message = handle_fft.obtainMessage();
				message.what = count_fft; // 只能放数字（作用可以使用来做if判断）
				if (button_flag == true) {
					timer_fft.cancel();// 若离散信号以满足要求，按钮已被置为有效，则该定时任务被取消
				}
				handle_fft.sendMessage(message); // 发送消息
			}
		};

		preview = (SurfaceView) findViewById(R.id.preview);

		// 在代码中设置surface占比大小，最终没有使用
		/*
		 * Display currDisplay =
		 * getWindowManager().getDefaultDisplay();//获取屏幕当前分辨率 int displayHeight
		 * = currDisplay.getHeight(); LayoutParams
		 * params=preview.getLayoutParams(); params.height=displayHeight/3;
		 * preview.setLayoutParams(params);
		 * 
		 * params=chart.getLayoutParams(); params.height=displayHeight/3;
		 * chart.setLayoutParams(params);
		 */

		/**
		 * SurfaceView是视图(View)的继承类，这个视图里内嵌了一个专门用于绘制的Surface。
		 * 你可以控制这个Surface的格式和尺寸。Surfaceview控制这个Surface的绘制位置。
		 * surface是纵深排序(Z-ordered
		 * )的，这表明它总在自己所在窗口的后面。surfaceview提供了一个可见区域，只有在这个可见区域内
		 * 的surface部分内容才可见，可见区域外的部分不可见。
		 * surface的排版显示受到视图层级关系的影响，它的兄弟视图结点会在顶端显示。这意味者
		 * surface的内容会被它的兄弟视图遮挡，这一特性可以用来放置遮盖物(overlays)
		 * (例如，文本和按钮等控件)。注意，如果surface上面
		 * 有透明控件，那么它的每次变化都会引起框架重新计算它和顶层控件的透明效果，这会影响性能。
		 * 你可以通过SurfaceHolder接口访问这个surface，getHolder()方法可以得到这个接口。
		 * surfaceview变得可见时，surface被创建；surfaceview隐藏前，surface被销毁。这样能节省资源。如果你要查看
		 * surface被创建和销毁的时机， 可以重载surfaceCreated(SurfaceHolder)和
		 * surfaceDestroyed(SurfaceHolder)。
		 * surfaceview的核心在于提供了两个线程：UI线程和渲染线程。这里应注意： 1>
		 * 所有SurfaceView和SurfaceHolder
		 * .Callback的方法都应该在UI线程里调用，一般来说就是应用程序主线程。渲染线程所要访问的各种变量应该作同步处理。 2>
		 * 由于surface可能被销毁，它只在SurfaceHolder.Callback.surfaceCreated()和
		 * SurfaceHolder
		 * .Callback.surfaceDestroyed()之间有效，所以要确保渲染线程访问的是合法有效的surface。
		 * 
		 * 这里用到了一个类SurfaceHolder,可以把它当成surface的控制器，用来操纵surface。
		 * 处理它的Canvas上画的效果和动画，控制表面，大小，像素等。 几个需要注意的方法： (1)、abstract void
		 * addCallback(SurfaceHolder.Callback callback);//
		 * 给SurfaceView当前的持有者一个回调对象。 (2)、abstract Canvas lockCanvas();//
		 * 锁定画布，一般在锁定后就可以通过其返回的画布对象Canvas，在其上面画图等操作了。 (3)、abstract Canvas
		 * lockCanvas(Rect dirty); //
		 * 锁定画布的某个区域进行画图等..因为画完图后，会调用下面的unlockCanvasAndPost来改变显示内容。 //
		 * 相对部分内存要求比较高的游戏来说，可以不用重画dirty外的其它区域的像素，可以提高速度。 (4)、abstract void
		 * unlockCanvasAndPost(Canvas canvas);// 结束锁定画图，并提交改变。
		 * 
		 */
		previewHolder = preview.getHolder(); // 通过SurfaceHolder接口访问这个surface，getHolder()方法可以得到这个接口
		previewHolder.addCallback(surfaceCallback); // 给SurfaceView当前的持有者一个回调对象

		/**
		 * 这里必须设置为SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS哦，意思
		 * 是创建一个push的'surface'，主要的特点就是不进行缓冲 ，也是为了适应低版本设备
		 */
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		info_heart_rate = (TextView) findViewById(R.id.info_heart_rate); // 获取各个控件
		img_average = (TextView) findViewById(R.id.img_average);
		num_pulse = (TextView) findViewById(R.id.num_pulse);

		/**
		 * 通过PowerManager类我们可以对设备的电源进行管理。对该类API的使用将影响到电池寿命。
		 * 只有在必须使用WakeLocks的时候，才使用WakeLocks，且在不使用它的时候要及时释放（release）
		 * 
		 * 默认情况下，当用户对手机有一段时间没有操作后，手机的Keyboard（这里不仅仅指硬键盘，还包括其他的
		 * 所有键，比如Menu)背光将消失，从Bright变为Off,如果再过段时间没操作，屏幕（Screen）将从高亮
		 * （Bright）变为暗淡（Dim），如果再过段时间没操作，屏幕（Screen）将又由暗淡（Dim）变为不显示（Off）,
		 * 如果再过段时间没操作,CPU将sleep,从on变为off.通过PowerManager类可以对上述过程进行管理,可以让设备
		 * 到达上面的某种状态时，该状态将不再超时，将不再往下走，但是仍然可以跳到到更上级的某种状态（比如用户有活动，
		 * 可以让手机回到最高状态）。你可以通过Context.getSystemService()方法来得到PowerManager类的实例。
		 * 你通常需要使用的是newWakeLock()，它将创建一个PowerManager.WakeLock实例。你可以通过该对象的方法
		 * 来对电源进行管理。
		 */

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// 通过系统提供的Manager接口来访问这些Service提供的数据
		/**
		 * public PowerManager.WakeLock newWakeLock(int flags,String tag) Get a
		 * wake lock at the level of the flags parameter. Call acquire() on the
		 * object to acquire the wake lock, and release() when you are done.
		 * 
		 * @param flags
		 *            - Combination of flag values defining the requested
		 *            behavior of the WakeLock.
		 * @param tag
		 *            - Your class name (or other tag) for debugging purposes.
		 * 
		 *            另请参见：PowerManager.WakeLock.acquire(),
		 *            PowerManager.WakeLock.release()
		 */
		wakeLock = pm
				.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
		// CPU、screen、keyboar全亮，后面的tag作为调试目的
	}

	/**
	 * 曲线样式(渲染器) : 对曲线图渲染器进行配置, 主要配置曲线
	 * 
	 * @param color
	 *            坐标轴的颜色
	 * @param style
	 *            标签的颜色
	 * @param fill
	 *            数据点是否填充
	 * @return renderer 数据渲染集合
	 */
	protected XYMultipleSeriesRenderer buildRenderer(int color,
			PointStyle style, boolean fill) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();/* 创建曲线图图表渲染器 */

		XYSeriesRenderer r = new XYSeriesRenderer();/* 单个曲线的渲染器 */
		r.setColor(color); /* 为单个曲线渲染器设置曲线颜色 */
		r.setPointStyle(style); /* 为单个曲线渲染器设置曲线风格 */
		r.setFillPoints(fill); /* 为单个曲线渲染器设置曲线点是否填充 */
		r.setLineWidth(2); /* 为单个曲线渲染器设置曲线宽度 */

		renderer.setApplyBackgroundColor(true);//设置是否显示背景色  
	    renderer.setBackgroundColor(Color.argb(100, 50, 50, 50));//设置背景色  
	    renderer.setAxisTitleTextSize(16); //设置轴标题文字的大小  
	    renderer.setChartTitleTextSize(20);//?设置整个图表标题文字大小  
	    renderer.setLabelsTextSize(15);//设置刻度显示文字的大小(XY轴都会被设置)  
	      
	    renderer.setMargins(new int[] { 30, 40, 0, 10 });//设置图表的外边框(上/左/下/右)  
	    renderer.setZoomButtonsVisible(true);//是否显示放大缩小按钮  
	    renderer.setPointSize(2);//设置点的大小(图上显示的点的大小和图例中点的大小都会被设置)
	    renderer.setShowLegend(false); /* 不显示图例 */
	    		//renderer.setLegendTextSize(15);//图例文字大小
	    renderer.setShowGrid(true); /* 设置显示网格 */
		renderer.setGridColor(Color.GREEN); /* 设置网格颜色 */
		renderer.setXLabels(20); /* 设置 x轴刻度个数 */
		renderer.setYLabels(8); /* 设置 y轴刻度个数 */
		renderer.setYLabelsAlign(Align.RIGHT); /* 设置 y轴标签对齐 */
		renderer.addSeriesRenderer(r); /* 将单个曲线渲染器设置到渲染器集合中 */
		return renderer;
	}

	/**
	 * 曲线图(渲染器) : 对曲线图坐标轴进行配置
	 * 
	 * @param renderer
	 *            数据渲染集合
	 * @param xTitle
	 *            X轴标签
	 * @param yTitle
	 *            Y轴标签
	 * @param xMin
	 *            X轴最小值
	 * @param xMax
	 *            X轴最大值
	 * @param yMin
	 *            Y轴最小值
	 * @param yMin
	 *            Y轴最小值
	 * @param axesColor
	 *            坐标轴的颜色
	 * @param labelsColor
	 *            标签的颜色
	 */
	protected static void setChartSettings(String title,
			XYMultipleSeriesRenderer renderer, String xTitle, String yTitle,
			double xMin, double xMax, double yMin, double yMax, int axesColor,
			int labelsColor) {
		
		renderer.setChartTitle(title); /* 设置曲线图标题 */
		renderer.setXTitle(xTitle); /* 设置x轴标题 */
		renderer.setYTitle(yTitle); /* 设置y轴标题 */
		renderer.setXAxisMin(xMin); /* 设置x轴最小值 */
		renderer.setXAxisMax(xMax); /* 设置x轴最大值 */
		renderer.setYAxisMin(yMin); /* 设置y轴最小值 */
		renderer.setYAxisMax(yMax); /* 设置y轴最大值 */
		renderer.setAxesColor(axesColor); /* 设置坐标轴颜色 */
		renderer.setLabelsColor(labelsColor); /* 设置标签颜色 */
		
	}

	/**
	 * 更新图表
	 * 
	 * 这个方法主要实现了对图表中曲线图的更新绘制，同时检测手机摄像头感应的手指位置，
	 * 如果手指位置不正确，则会提示“请用您的指尖盖住摄像头镜头”的信息来提示用户。 动态的更新绘制曲线图来模拟用户心跳频率。
	 */
	private void updateChart() {
		if(gx < 220){
			toast = Toast.makeText(context, "请用您的指尖轻触摄像头镜头！",
				Toast.LENGTH_SHORT);
			toast.show();
		}
		
		// Toast用于向用户显示一些帮助/提示~http://blog.csdn.net/yunduanman/article/details/7371990
		
		// 移除数据集中旧的点集
		mDataset.removeSeries(series);

		// 判断当前点集中到底有多少点，因为屏幕总共只能容纳100个，所以当点数超过100时，长度永远是100
		int length_serial = series.getItemCount();
		int bz = 0; // 波形平移量

		if (length_serial > length) {
			length_serial = length;
			bz = 1;
		}

		// int bz=1; //波形平移量
		addX = length_serial;// 在整个数据系列的最右侧即，addX=length加入振幅数据addY
		// 将旧的点集中x和y的数值取出来放入backup中，并且将第i个点的x值减小1，造成曲线向左移动的效果
		for (int i = 0; i < length_serial; i++) {
			xv[i] = (double) series.getX(i) - bz;
			yv[i] = (double) series.getY(i);
		}

		// 点集先清空，为了做成新的点集而准备
		series.clear();
		mDataset.addSeries(series);
		// 将新产生的点首先加入到点集中，然后在循环体中将坐标变换后的一系列点都重新加入到点集中
		// 这里可以试验一下把顺序颠倒过来是什么效果，即先运行循环体，再添加新产生的点

		// series.add(addX, addY);
		series.add(addX, ((int)(gx/10)*10 + 4) - gx);
		
		for (int k = 0; k < length_serial; k++) {
			// 在数据集中添加新的点集
			series.add(xv[k], yv[k]);
		}
		
		// 视图更新，没有这一步，曲线不会呈现动态。如果在非UI主线程中，需要调用postInvalidate()，具体参考api
		chart.invalidate();
	}

	/**
	 * 相机预览回调方法
	 * 
	 * 这个方法中实现动态更新界面UI的功能，通过获取手机摄像头的参数来实时动态计算平均像素值、 脉冲数，从而实时动态计算心率值。
	 */
	private static PreviewCallback previewCallback = new PreviewCallback() {
		/**
		 * void onPreviewFrame(byte[] data, Camera camera)
		 * 
		 * The callback that delivers the preview frames.
		 * 
		 * @param data
		 *            - The contents of the preview frame in
		 *            getPreviewFormat()format.
		 * @param camera
		 *            - The Camera service object.
		 */
		public void onPreviewFrame(byte[] data, Camera cam) {
			if (data == null)
				throw new NullPointerException();
			Camera.Size size = cam.getParameters().getPreviewSize();
			/*
			 * public Camera.Size getPreviewSize () Added in API level 1 Returns
			 * the dimensions setting for preview pictures Returns a Size object
			 * with the width and height setting for the preview picture
			 */
			if (size == null)
				throw new NullPointerException();
			/**
			 * public final boolean compareAndSet(boolean expect,boolean update)
			 * 如果当前值 == 预期值，则以原子方式将该值设置为给定的更新值。 参数：
			 * 
			 * @param expect
			 *            - 预期值
			 * @param update
			 *            - 新值 返回： 如果成功，则返回 true。返回 False 指示实际值与预期值不相等。
			 */
			if (!processing.compareAndSet(false, true))
				return;
			// 若原子变量processing，为false，则直接退出
			int width = size.width;
			int height = size.height;
			// 图像处理 clone()返回此实例的一个副本，protected Object clone() throws
			// CloneNotSupportedException
			double imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(
					data.clone(), height, width);
			// 当前画面内平均红色分量值
			gx = imgAvg;

			img_average.setText("ImgAvg " + String.format("%.4f", imgAvg));

			if (imgAvg <= 220 || imgAvg >= 255) {

				// 如果画面中中红色分量计算越界了
				/**
				 * public final void set(boolean newValue) 无条件地设置为给定值。
				 */
				processing.set(false);
				return;
			}

			double averageArrayAvg = 0;
			int averageArrayCnt = 0;
			for (int i = 0; i < averageArray.length; i++) {
				if (averageArray[i] > 0) {
					// 第一次遍历是时，averageArray[i]==0,不进行if内操作
					averageArrayAvg += averageArray[i];
					averageArrayCnt++;
				}
			}

			double rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt)
					: 0;
			// rollingAverage波动平均值，若averageArrayCnt大于零，则要计算平均值；否则，什么都不做

			TYPE newType = currentType;
			if (imgAvg < rollingAverage) {
				newType = TYPE.RED;
				if (newType != currentType) {
					// 若newType和currentType状态不一致，则脉冲数增加1
					beats++;
					// flag=0;出现新的波峰时，将标志flag置为零
					num_pulse.setText(String.format("%.0f", beats) + " Pulse");
					// Log.e(TAG, "BEAT!! beats=" + beats);
				}
			} else if (imgAvg > rollingAverage) {
				newType = TYPE.GREEN;
				// 当前画面像素值大于前面的平均值，说明，仍然没有一个明显的波峰出现，大致上还处于上升趋势，newType重置为1的话说为上升趋势
			}

			if (averageIndex == averageArraySize) {
				// 实现系数averageIndex在0至averageArraySize-1之间循环
				averageIndex = 0;
			}
			averageArray[averageIndex] = imgAvg;
			// 在averageArray中存画面平均红色像素
			averageIndex++;
			// 索引值加1

			// Transitioned from one state to another to the same，状态转换
			if (newType != currentType) {
				currentType = newType;// 将本次生成的状态保存给current，即上一个状态（上升或这下降趋势）
				// image.postInvalidate();
			}
			// 获取系统结束时间（ms）
			long endTime = System.currentTimeMillis();
			double totalTimeInSecs = (endTime - startTime) / 1000d;// 获得一次持续测量时间

			if (totalTimeInSecs > 4) {

				//double bps = ((beats) / totalTimeInSecs);// 得到心跳频率
				int dpm = (int) (beats * 60d/totalTimeInSecs);// 计算每分钟心率
				if (dpm < 50 || dpm > 180 || imgAvg < 220) {
					// 获取系统开始时间（ms），重新开始一轮计算
					startTime = System.currentTimeMillis();
					beats = 0;// beats心跳总数
					processing.set(false);
					return;
				}
				// Log.e(TAG, "totalTimeInSecs=" + totalTimeInSecs + " beats="+
				// beats);
				if (beatsIndex == beatsArraySize)
					// 实现索引beatsIndex循环
					beatsIndex = 0;

				beatsArray[beatsIndex] = dpm;// 存入心率数组
				beatsIndex++;

				// 马上要开始最终输出心率数值的计算了
				double beatsArrayAvg = 0;// beatsArray中有效心率的平均值
				int beatsArrayCnt = 0;// beatsArray中有效心率数的个数
				for (int i = 0; i < beatsArray.length; i++) {
					if (beatsArray[i] > 0) {
						beatsArrayAvg += beatsArray[i];
						beatsArrayCnt++;
					}
				}
				int beatsAvg = (int) (beatsArrayAvg / beatsArrayCnt);// 平均心率
				info_heart_rate.setText(String.format("%03d", beatsAvg));

				// 以下代码，目的是重新开始一次心率计算
				startTime = System.currentTimeMillis();
				// 重新获取获取系统时间（ms）
				beats = 0;
			}
			processing.set(false);
		}
	};

	/**
	 * 预览回调接口
	 * 
	 * 相机摄像头，捕捉信息改变时调用
	 */
	private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

		// 在创建时激发，一般在这里调用画图的线程
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.v("TAG", "surface Callback_Created");

			try {

				/**
				 * public final void setPreviewDisplay(SurfaceHolder holder)
				 * 
				 * throws IOException Sets the SurfaceHolder to be used for a
				 * picture preview. If the surface changed since the last call,
				 * the screen will blank. Nothing happens if the same surface is
				 * re-set. 参数：
				 * 
				 * @holder - the SurfaceHolder upon which to place the picture
				 *         preview 抛出： IOException - if the method fails.
				 */

				camera.setPreviewDisplay(previewHolder);

			} catch (Throwable t) {
				Log.e("PreviewDemo-setPreviewDisplay",
						"Exception in setPreviewDisplay()", t);
			}
			try {
				/**
				 * public final void setPreviewCallback(Camera.PreviewCallback
				 * cb)
				 * 
				 * Can be called at any time to instruct the camera to use a
				 * callback for each preview frame in addition to displaying it.
				 * 参数： cb - A callback object that receives a copy of each
				 * preview frame. Pass null to stop receiving callbacks at any
				 * time.
				 */
				camera.setPreviewCallback(previewCallback);
			} catch (Throwable t) {
				Log.e("PreviewDemo-setPreviewCallback",
						"Exception in setPreviewDisplay()", t);
			}

		}

		// 当预览改变的时候回调此方法
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.v("TAG", "surface Callback_Changed");
			/**
			 * public Camera.Parameters getParameters()
			 * 
			 * Returns the picture Parameters for this Camera service.
			 */
			Camera.Parameters parameters = null;
			try {
				parameters = camera.getParameters();
			} catch (Throwable t) {
				Log.v("camera.getParameters", "surface Callback_Changed");
			}
			Camera.Size size = getSmallestPreviewSize(width, height, parameters);
			if (size != null) {
				parameters.setPreviewSize(size.width, size.height);
				Log.d("TAG", "Using width=" + size.width + " height="
						+ size.height);
			}

			camera.setParameters(parameters);

			camera.startPreview();
		}

		// 销毁的时候调用
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// Ignore
			Log.v("TAG", "====surfaceDestroyed");
			/**
			 * camera.stopPreview();// stop preview camera.release(); // Release
			 * camera resources camera = null; 在onpause()中已经实现过了，重复实现会导致退出程序时错误
			 * 
			 */

		}
	};

	/**
	 * 获取相机最小的预览尺寸方法
	 * 
	 * 功能是获取当前手机相机最小的预览尺寸
	 * 
	 * @param width
	 * @param height
	 * @param parameters
	 * @return
	 */
	private static Camera.Size getSmallestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			// 遍历所有支持的预览尺寸
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;
					if (newArea < resultArea)
						result = size;
				}
			}
		}
		return result;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exit();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}// 关于按两次退出的代码

	private void exit() {
		if (!isExit) {
			isExit = true;
			
			toast = Toast.makeText(Heart_Rate_Detect.this, "再按一次退出程序",
					Toast.LENGTH_SHORT);
			toast.show();
			// 利用handler延迟发送更改状态信息
			handle_exit.sendEmptyMessageDelayed(0, 2000);
		} else {
			finish();
			System.exit(0);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onResume() {
		super.onResume();
		wakeLock.acquire();
		camera = Camera.open();
		startTime = System.currentTimeMillis();
		/**
		 * public void schedule(TimerTask task, long delay, long period)
		 * 安排指定的任务从指定的延迟后开始进行重复的固定延迟执行。
		 * 
		 * @task - 所要安排的任务。
		 * @delay - 执行任务前的延迟时间，单位是毫秒。
		 * @period - 执行各后续任务之间的时间间隔，单位是毫秒。
		 * 
		 * @IllegalArgumentException - 如果 delay 是负数，或者 delay +
		 *                           System.currentTimeMillis() 是负数。
		 * @IllegalStateException - 如果已经安排或取消了任务，已经取消了计时器，或者计时器线程已终止。
		 */
		timer.schedule(task, 1, 50);
		if (button_flag == false) {
			timer_fft.schedule(task_fft, 1, 2500);// fft计时器
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		wakeLock.release();
		camera.setPreviewCallback(null);
		camera.stopPreview(); // stop preview
		camera.release(); // Release camera resources
		camera = null;
		timer.cancel();
		timer_fft.cancel();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		/**
		 * public void cancel()终止此计时器，丢弃所有当前已安排的任务。这不会干扰当前正在执行的任务（如果存在）。
		 * 一旦终止了计时器，那么它的执行线程也会终止，并且无法根据它安排更多的任务。 注意，在此计时器调用的计时器任务的 run
		 * 方法内调用此方法，就可以绝对确保正在执行的任务是此计时器所执行的最后一个任务。 可以重复调用此方法；但是第二次和后续调用无效。
		 */
		timer.cancel();
		timer_fft.cancel();

	};
	
}