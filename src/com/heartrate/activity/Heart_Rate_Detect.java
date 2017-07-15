package com.heartrate.activity;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;


//importͼ������
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


//import�Լ��Ĺ�����
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
	private Timer timer = new Timer(); // һ�ֹ��ߣ��߳����䰲���Ժ��ں�̨�߳���ִ�е����񡣿ɰ�������ִ��һ�Σ����߶����ظ�ִ��
	private Timer timer_fft = new Timer();
	private TimerTask task; // ��ʱ��ʵ��task
	private TimerTask task_fft;
	private static double gx; // ��ǰ������ƽ����ɫ����ֵ
	private MyHandler handler; // handlerʵ��
	private MyHandler handle_fft;
	private MyHandler handle_exit;
	private static String title = "Pulse"; // ������
	private XYSeries series; // pause ����ϵ������
	private XYMultipleSeriesDataset mDataset; // ����series����ϵ�е����ݼ�
	private GraphicalView chart; // ͼ����
	private static XYMultipleSeriesRenderer renderer; // ��Ⱦ���Ƶ�����
	private static Context context; // Ӧ��������
	private int addX = -1;
	double addY;
	static int length = 64; // һ����ɢ��ֵ�ĳ���
	double[] xv = new double[length]; // �����µ㼯��xֵ
	double[] yv = new double[length]; // �����µ㼯��yֵ
	static double sigl_tem[] = new double[length]; // �ݴ�һ��ʱ����ɢ�ź�
	static double SNratio = 0; // ��ɢ�źű任�õ��������
	boolean button_flag = false; // ��ť״̬��־��

	private static final AtomicBoolean processing = new AtomicBoolean(false);
	/**
	 * ȡ��һ��ԭ�ӱ���processing��ָʾ��ǰ����Ĵ���״̬�Ƿ���ȷ��AtomicBoolean ������ԭ�ӷ�ʽ���µ� boolean ֵ
	 */
	private static SurfaceView preview = null; // Android�ֻ�Ԥ���ؼ�
	private static SurfaceHolder previewHolder = null;
	// Ԥ��������Ϣ
	private static Camera camera = null; // Android�ֻ�������
	private static TextView info_heart_rate = null;
	private static TextView img_average = null;
	private static TextView num_pulse = null;
	private static Button Light = null; // ���������
	private static Button Jump = null; // ��ת��ť
	private static WakeLock wakeLock = null;
	private static int averageIndex = 0;
	private static final int averageArraySize = 8;
	private static final double[] averageArray = new double[averageArraySize];// �������averageArray������ƽ����ɫ����ֵ

	/**
	 * ����ö��
	 * 
	 * ����ö����������ʶ���ߵ��������½�״̬
	 * 
	 * @author KINGHMY ��ɫ���ͣ�����������һ��ö�����������壬���ö�����ͺܼ򵥣�
	 *         ֻ��������ɫ��һ������ɫ������������һ���Ǻ�ɫ�������½����ơ�
	 */
	public static enum TYPE {
		GREEN, RED
	};

	private static TYPE currentType = TYPE.GREEN; // ����Ĭ�����ͣ�Ϊ��������

	public static TYPE getCurrent() { // ��ȡ��ǰ����
		return currentType;
	}

	private static int beatsIndex = 0; // �����±�ֵ
	private static final int beatsArraySize = 2; // ��������Ĵ�С
	private static final int[] beatsArray = new int[beatsArraySize];// �������飬�洢���beatsArraySize�����ɵ�������ֵ
	// ��������
	private static double beats = 0; // ��������
	private static long startTime = 0; // ��ʼʱ��
	private static int count_fft = 0; // fft�������

	private static boolean isExit = false; // ����һ������������ʶ�Ƿ��˳�
	private static Toast toast; // ����Toast����

	static class MyHandler extends Handler {
		// �ڲ��࣬��̬handler �����ڴ�й¶����
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
		 * ��ʼ������
		 * 
		 * �����ϵĹ����ǳ�ʼ������ĸ������ã�����������������������ҳ��ͼ��ĳ�ʼ����
		 * UI�ؼ��ĳ�ʼ����Ӧ�ó�����������ʾ����ʽ�����������ͨ��Handler����������
		 * �����ݹ�������Ϣ��Ϣ������UI�����ȵȡ���Ҫ��ʵ��Ӧ�õ����ù��ܣ�ͬʱ�൱��һ
		 * ��Ӧ�ó���Ĺܼң�����ֱ�ӻ��ӵĵ���������������ʹ����Ӧ�ó���˳������������
		 */
		context = getApplicationContext();
		/*
		 * ������һ��ȫ�ֵ����ݲ�����,�õ���context, ���ʱ���Ҫ�õ� getApplicationContext ,
		 * ��������ACtivity, ��ͱ�֤��, ���ݿ�Ĳ�����activity�޹�.
		 */
		// ������main�����ϵ��ĵ�ͼ�������֣��������ĵ�ͼ���������������
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
				 * ������ݴ��ʱ���������鷢�͸��µ�activity��Ȼ��ǰ�over
				 */
				if(button_flag == true){
					Intent intent = new Intent(Heart_Rate_Detect.this, Deep_Comp.class);  
					  
					/* ͨ��Bundle����洢��Ҫ���ݵ����� */  
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
					
					/*��bundle����assign��Intent*/  
					intent.putExtras(bundle);  
					  
					startActivity(intent);
				}
			}

		});

		// �������������һ��ϵ�������ϵ����е㣬��һ����ļ��ϣ�������Щ�㻭�����ߣ����췽������ֻ��ϵ������
		series = new XYSeries(title);

		// ����һ����ϵ�����ݼ���ʵ����������ݼ�������������ͼ�����ݼ����԰���0-�������ϵ��
		mDataset = new XYMultipleSeriesDataset();

		// ��serial�㼯��ӵ�������ݼ���
		mDataset.addSeries(series);

		// ���¶������ߵ���ʽ�����Եȵȵ����ã�renderer�൱��һ��������ͼ������Ⱦ�ľ��
		int color = Color.RED;
		PointStyle style = PointStyle.CIRCLE;
		/**
		 * ���˴�style��ֵΪnull�ᵼ��preview�޷����أ�Ȼ��mainactivity������Ѫ���۵Ľ�ѵ
		 */

		// ������ʵ�ֵ�һ����������������ͼ����ɫ�ͷ��
		renderer = buildRenderer(color, style, true);

		// ������ʵ�ֵ�һ����������,���ú�ͼ�����ʽ
		setChartSettings(title,renderer, "Time", "mmHg", 0, 63, -0.8, 0.5,
				Color.WHITE, Color.WHITE);

		// ����ͼ��
		chart = ChartFactory.getLineChartView(context, mDataset, renderer);

		// ��ͼ����ӵ�������ȥ
		layout.addView(chart, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		/**
		 * ����һ��Handlerʵ��handler
		 * 
		 * �����Handlerʵ������������Timerʵ������ɶ�ʱ����ͼ��Ĺ���
		 */
		handler = new MyHandler(this) {
			@Override
			public void handleMessage(Message msg) {
				// ˢ��ͼ��
				updateChart(); // ����ͼ��
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
							"������ת��ť�鿴���������", Toast.LENGTH_SHORT);
					toast.show();
				} else {
					Compute_Result result = new Compute_Result();
					
					double Discrete_clone[] = yv.clone();// ����ǰ��һ����ɢֵ��������

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
		};// �������˳���һ��handle

		/**
		 * ����һ����ʱ��ʵ��task
		 * 
		 * public abstract void run() �˼�ʱ������Ҫִ�еĲ���
		 */
		task = new TimerTask() {
			@Override
			public void run() {
				// Message message = new Message(); //��ȡһ��Message����
				Message message = handler.obtainMessage();
				message.what = 1; // ֻ�ܷ����֣����ÿ���ʹ������if�жϣ�
				handler.sendMessage(message); // ������Ϣ
			}
		};

		task_fft = new TimerTask() {
			@Override
			public void run() {
				
				// Message message = new Message(); //��ȡһ��Message����
				Message message = handle_fft.obtainMessage();
				message.what = count_fft; // ֻ�ܷ����֣����ÿ���ʹ������if�жϣ�
				if (button_flag == true) {
					timer_fft.cancel();// ����ɢ�ź�������Ҫ�󣬰�ť�ѱ���Ϊ��Ч����ö�ʱ����ȡ��
				}
				handle_fft.sendMessage(message); // ������Ϣ
			}
		};

		preview = (SurfaceView) findViewById(R.id.preview);

		// �ڴ���������surfaceռ�ȴ�С������û��ʹ��
		/*
		 * Display currDisplay =
		 * getWindowManager().getDefaultDisplay();//��ȡ��Ļ��ǰ�ֱ��� int displayHeight
		 * = currDisplay.getHeight(); LayoutParams
		 * params=preview.getLayoutParams(); params.height=displayHeight/3;
		 * preview.setLayoutParams(params);
		 * 
		 * params=chart.getLayoutParams(); params.height=displayHeight/3;
		 * chart.setLayoutParams(params);
		 */

		/**
		 * SurfaceView����ͼ(View)�ļ̳��࣬�����ͼ����Ƕ��һ��ר�����ڻ��Ƶ�Surface��
		 * ����Կ������Surface�ĸ�ʽ�ͳߴ硣Surfaceview�������Surface�Ļ���λ�á�
		 * surface����������(Z-ordered
		 * )�ģ�������������Լ����ڴ��ڵĺ��档surfaceview�ṩ��һ���ɼ�����ֻ��������ɼ�������
		 * ��surface�������ݲſɼ����ɼ�������Ĳ��ֲ��ɼ���
		 * surface���Ű���ʾ�ܵ���ͼ�㼶��ϵ��Ӱ�죬�����ֵ���ͼ�����ڶ�����ʾ������ζ��
		 * surface�����ݻᱻ�����ֵ���ͼ�ڵ�����һ���Կ������������ڸ���(overlays)
		 * (���磬�ı��Ͱ�ť�ȿؼ�)��ע�⣬���surface����
		 * ��͸���ؼ�����ô����ÿ�α仯�������������¼������Ͷ���ؼ���͸��Ч�������Ӱ�����ܡ�
		 * �����ͨ��SurfaceHolder�ӿڷ������surface��getHolder()�������Եõ�����ӿڡ�
		 * surfaceview��ÿɼ�ʱ��surface��������surfaceview����ǰ��surface�����١������ܽ�ʡ��Դ�������Ҫ�鿴
		 * surface�����������ٵ�ʱ���� ��������surfaceCreated(SurfaceHolder)��
		 * surfaceDestroyed(SurfaceHolder)��
		 * surfaceview�ĺ��������ṩ�������̣߳�UI�̺߳���Ⱦ�̡߳�����Ӧע�⣺ 1>
		 * ����SurfaceView��SurfaceHolder
		 * .Callback�ķ�����Ӧ����UI�߳�����ã�һ����˵����Ӧ�ó������̡߳���Ⱦ�߳���Ҫ���ʵĸ��ֱ���Ӧ����ͬ������ 2>
		 * ����surface���ܱ����٣���ֻ��SurfaceHolder.Callback.surfaceCreated()��
		 * SurfaceHolder
		 * .Callback.surfaceDestroyed()֮����Ч������Ҫȷ����Ⱦ�̷߳��ʵ��ǺϷ���Ч��surface��
		 * 
		 * �����õ���һ����SurfaceHolder,���԰�������surface�Ŀ���������������surface��
		 * ��������Canvas�ϻ���Ч���Ͷ��������Ʊ��棬��С�����صȡ� ������Ҫע��ķ����� (1)��abstract void
		 * addCallback(SurfaceHolder.Callback callback);//
		 * ��SurfaceView��ǰ�ĳ�����һ���ص����� (2)��abstract Canvas lockCanvas();//
		 * ����������һ����������Ϳ���ͨ���䷵�صĻ�������Canvas���������滭ͼ�Ȳ����ˡ� (3)��abstract Canvas
		 * lockCanvas(Rect dirty); //
		 * ����������ĳ��������л�ͼ��..��Ϊ����ͼ�󣬻���������unlockCanvasAndPost���ı���ʾ���ݡ� //
		 * ��Բ����ڴ�Ҫ��Ƚϸߵ���Ϸ��˵�����Բ����ػ�dirty���������������أ���������ٶȡ� (4)��abstract void
		 * unlockCanvasAndPost(Canvas canvas);// ����������ͼ�����ύ�ı䡣
		 * 
		 */
		previewHolder = preview.getHolder(); // ͨ��SurfaceHolder�ӿڷ������surface��getHolder()�������Եõ�����ӿ�
		previewHolder.addCallback(surfaceCallback); // ��SurfaceView��ǰ�ĳ�����һ���ص�����

		/**
		 * �����������ΪSurfaceHolder.SURFACE_TYPE_PUSH_BUFFERSŶ����˼
		 * �Ǵ���һ��push��'surface'����Ҫ���ص���ǲ����л��� ��Ҳ��Ϊ����Ӧ�Ͱ汾�豸
		 */
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		info_heart_rate = (TextView) findViewById(R.id.info_heart_rate); // ��ȡ�����ؼ�
		img_average = (TextView) findViewById(R.id.img_average);
		num_pulse = (TextView) findViewById(R.id.num_pulse);

		/**
		 * ͨ��PowerManager�����ǿ��Զ��豸�ĵ�Դ���й����Ը���API��ʹ�ý�Ӱ�쵽���������
		 * ֻ���ڱ���ʹ��WakeLocks��ʱ�򣬲�ʹ��WakeLocks�����ڲ�ʹ������ʱ��Ҫ��ʱ�ͷţ�release��
		 * 
		 * Ĭ������£����û����ֻ���һ��ʱ��û�в������ֻ���Keyboard�����ﲻ����ָӲ���̣�������������
		 * ���м�������Menu)���⽫��ʧ����Bright��ΪOff,����ٹ���ʱ��û��������Ļ��Screen�����Ӹ���
		 * ��Bright����Ϊ������Dim��������ٹ���ʱ��û��������Ļ��Screen�������ɰ�����Dim����Ϊ����ʾ��Off��,
		 * ����ٹ���ʱ��û����,CPU��sleep,��on��Ϊoff.ͨ��PowerManager����Զ��������̽��й���,�������豸
		 * ���������ĳ��״̬ʱ����״̬�����ٳ�ʱ�������������ߣ�������Ȼ�������������ϼ���ĳ��״̬�������û��л��
		 * �������ֻ��ص����״̬���������ͨ��Context.getSystemService()�������õ�PowerManager���ʵ����
		 * ��ͨ����Ҫʹ�õ���newWakeLock()����������һ��PowerManager.WakeLockʵ���������ͨ���ö���ķ���
		 * ���Ե�Դ���й���
		 */

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		// ͨ��ϵͳ�ṩ��Manager�ӿ���������ЩService�ṩ������
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
		 *            ����μ���PowerManager.WakeLock.acquire(),
		 *            PowerManager.WakeLock.release()
		 */
		wakeLock = pm
				.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen");
		// CPU��screen��keyboarȫ���������tag��Ϊ����Ŀ��
	}

	/**
	 * ������ʽ(��Ⱦ��) : ������ͼ��Ⱦ����������, ��Ҫ��������
	 * 
	 * @param color
	 *            ���������ɫ
	 * @param style
	 *            ��ǩ����ɫ
	 * @param fill
	 *            ���ݵ��Ƿ����
	 * @return renderer ������Ⱦ����
	 */
	protected XYMultipleSeriesRenderer buildRenderer(int color,
			PointStyle style, boolean fill) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();/* ��������ͼͼ����Ⱦ�� */

		XYSeriesRenderer r = new XYSeriesRenderer();/* �������ߵ���Ⱦ�� */
		r.setColor(color); /* Ϊ����������Ⱦ������������ɫ */
		r.setPointStyle(style); /* Ϊ����������Ⱦ���������߷�� */
		r.setFillPoints(fill); /* Ϊ����������Ⱦ���������ߵ��Ƿ���� */
		r.setLineWidth(2); /* Ϊ����������Ⱦ���������߿�� */

		renderer.setApplyBackgroundColor(true);//�����Ƿ���ʾ����ɫ  
	    renderer.setBackgroundColor(Color.argb(100, 50, 50, 50));//���ñ���ɫ  
	    renderer.setAxisTitleTextSize(16); //������������ֵĴ�С  
	    renderer.setChartTitleTextSize(20);//?��������ͼ��������ִ�С  
	    renderer.setLabelsTextSize(15);//���ÿ̶���ʾ���ֵĴ�С(XY�ᶼ�ᱻ����)  
	      
	    renderer.setMargins(new int[] { 30, 40, 0, 10 });//����ͼ�����߿�(��/��/��/��)  
	    renderer.setZoomButtonsVisible(true);//�Ƿ���ʾ�Ŵ���С��ť  
	    renderer.setPointSize(2);//���õ�Ĵ�С(ͼ����ʾ�ĵ�Ĵ�С��ͼ���е�Ĵ�С���ᱻ����)
	    renderer.setShowLegend(false); /* ����ʾͼ�� */
	    		//renderer.setLegendTextSize(15);//ͼ�����ִ�С
	    renderer.setShowGrid(true); /* ������ʾ���� */
		renderer.setGridColor(Color.GREEN); /* ����������ɫ */
		renderer.setXLabels(20); /* ���� x��̶ȸ��� */
		renderer.setYLabels(8); /* ���� y��̶ȸ��� */
		renderer.setYLabelsAlign(Align.RIGHT); /* ���� y���ǩ���� */
		renderer.addSeriesRenderer(r); /* ������������Ⱦ�����õ���Ⱦ�������� */
		return renderer;
	}

	/**
	 * ����ͼ(��Ⱦ��) : ������ͼ�������������
	 * 
	 * @param renderer
	 *            ������Ⱦ����
	 * @param xTitle
	 *            X���ǩ
	 * @param yTitle
	 *            Y���ǩ
	 * @param xMin
	 *            X����Сֵ
	 * @param xMax
	 *            X�����ֵ
	 * @param yMin
	 *            Y����Сֵ
	 * @param yMin
	 *            Y����Сֵ
	 * @param axesColor
	 *            ���������ɫ
	 * @param labelsColor
	 *            ��ǩ����ɫ
	 */
	protected static void setChartSettings(String title,
			XYMultipleSeriesRenderer renderer, String xTitle, String yTitle,
			double xMin, double xMax, double yMin, double yMax, int axesColor,
			int labelsColor) {
		
		renderer.setChartTitle(title); /* ��������ͼ���� */
		renderer.setXTitle(xTitle); /* ����x����� */
		renderer.setYTitle(yTitle); /* ����y����� */
		renderer.setXAxisMin(xMin); /* ����x����Сֵ */
		renderer.setXAxisMax(xMax); /* ����x�����ֵ */
		renderer.setYAxisMin(yMin); /* ����y����Сֵ */
		renderer.setYAxisMax(yMax); /* ����y�����ֵ */
		renderer.setAxesColor(axesColor); /* ������������ɫ */
		renderer.setLabelsColor(labelsColor); /* ���ñ�ǩ��ɫ */
		
	}

	/**
	 * ����ͼ��
	 * 
	 * ���������Ҫʵ���˶�ͼ��������ͼ�ĸ��»��ƣ�ͬʱ����ֻ�����ͷ��Ӧ����ָλ�ã�
	 * �����ָλ�ò���ȷ�������ʾ����������ָ���ס����ͷ��ͷ������Ϣ����ʾ�û��� ��̬�ĸ��»�������ͼ��ģ���û�����Ƶ�ʡ�
	 */
	private void updateChart() {
		if(gx < 220){
			toast = Toast.makeText(context, "��������ָ���ᴥ����ͷ��ͷ��",
				Toast.LENGTH_SHORT);
			toast.show();
		}
		
		// Toast�������û���ʾһЩ����/��ʾ~http://blog.csdn.net/yunduanman/article/details/7371990
		
		// �Ƴ����ݼ��оɵĵ㼯
		mDataset.removeSeries(series);

		// �жϵ�ǰ�㼯�е����ж��ٵ㣬��Ϊ��Ļ�ܹ�ֻ������100�������Ե���������100ʱ��������Զ��100
		int length_serial = series.getItemCount();
		int bz = 0; // ����ƽ����

		if (length_serial > length) {
			length_serial = length;
			bz = 1;
		}

		// int bz=1; //����ƽ����
		addX = length_serial;// ����������ϵ�е����Ҳ༴��addX=length�����������addY
		// ���ɵĵ㼯��x��y����ֵȡ��������backup�У����ҽ���i�����xֵ��С1��������������ƶ���Ч��
		for (int i = 0; i < length_serial; i++) {
			xv[i] = (double) series.getX(i) - bz;
			yv[i] = (double) series.getY(i);
		}

		// �㼯����գ�Ϊ�������µĵ㼯��׼��
		series.clear();
		mDataset.addSeries(series);
		// ���²����ĵ����ȼ��뵽�㼯�У�Ȼ����ѭ�����н�����任���һϵ�е㶼���¼��뵽�㼯��
		// �����������һ�°�˳��ߵ�������ʲôЧ������������ѭ���壬������²����ĵ�

		// series.add(addX, addY);
		series.add(addX, ((int)(gx/10)*10 + 4) - gx);
		
		for (int k = 0; k < length_serial; k++) {
			// �����ݼ�������µĵ㼯
			series.add(xv[k], yv[k]);
		}
		
		// ��ͼ���£�û����һ�������߲�����ֶ�̬������ڷ�UI���߳��У���Ҫ����postInvalidate()������ο�api
		chart.invalidate();
	}

	/**
	 * ���Ԥ���ص�����
	 * 
	 * ���������ʵ�ֶ�̬���½���UI�Ĺ��ܣ�ͨ����ȡ�ֻ�����ͷ�Ĳ�����ʵʱ��̬����ƽ������ֵ�� ���������Ӷ�ʵʱ��̬��������ֵ��
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
			 * �����ǰֵ == Ԥ��ֵ������ԭ�ӷ�ʽ����ֵ����Ϊ�����ĸ���ֵ�� ������
			 * 
			 * @param expect
			 *            - Ԥ��ֵ
			 * @param update
			 *            - ��ֵ ���أ� ����ɹ����򷵻� true������ False ָʾʵ��ֵ��Ԥ��ֵ����ȡ�
			 */
			if (!processing.compareAndSet(false, true))
				return;
			// ��ԭ�ӱ���processing��Ϊfalse����ֱ���˳�
			int width = size.width;
			int height = size.height;
			// ͼ���� clone()���ش�ʵ����һ��������protected Object clone() throws
			// CloneNotSupportedException
			double imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(
					data.clone(), height, width);
			// ��ǰ������ƽ����ɫ����ֵ
			gx = imgAvg;

			img_average.setText("ImgAvg " + String.format("%.4f", imgAvg));

			if (imgAvg <= 220 || imgAvg >= 255) {

				// ����������к�ɫ��������Խ����
				/**
				 * public final void set(boolean newValue) ������������Ϊ����ֵ��
				 */
				processing.set(false);
				return;
			}

			double averageArrayAvg = 0;
			int averageArrayCnt = 0;
			for (int i = 0; i < averageArray.length; i++) {
				if (averageArray[i] > 0) {
					// ��һ�α�����ʱ��averageArray[i]==0,������if�ڲ���
					averageArrayAvg += averageArray[i];
					averageArrayCnt++;
				}
			}

			double rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt)
					: 0;
			// rollingAverage����ƽ��ֵ����averageArrayCnt�����㣬��Ҫ����ƽ��ֵ������ʲô������

			TYPE newType = currentType;
			if (imgAvg < rollingAverage) {
				newType = TYPE.RED;
				if (newType != currentType) {
					// ��newType��currentType״̬��һ�£�������������1
					beats++;
					// flag=0;�����µĲ���ʱ������־flag��Ϊ��
					num_pulse.setText(String.format("%.0f", beats) + " Pulse");
					// Log.e(TAG, "BEAT!! beats=" + beats);
				}
			} else if (imgAvg > rollingAverage) {
				newType = TYPE.GREEN;
				// ��ǰ��������ֵ����ǰ���ƽ��ֵ��˵������Ȼû��һ�����ԵĲ�����֣������ϻ������������ƣ�newType����Ϊ1�Ļ�˵Ϊ��������
			}

			if (averageIndex == averageArraySize) {
				// ʵ��ϵ��averageIndex��0��averageArraySize-1֮��ѭ��
				averageIndex = 0;
			}
			averageArray[averageIndex] = imgAvg;
			// ��averageArray�д滭��ƽ����ɫ����
			averageIndex++;
			// ����ֵ��1

			// Transitioned from one state to another to the same��״̬ת��
			if (newType != currentType) {
				currentType = newType;// ���������ɵ�״̬�����current������һ��״̬�����������½����ƣ�
				// image.postInvalidate();
			}
			// ��ȡϵͳ����ʱ�䣨ms��
			long endTime = System.currentTimeMillis();
			double totalTimeInSecs = (endTime - startTime) / 1000d;// ���һ�γ�������ʱ��

			if (totalTimeInSecs > 4) {

				//double bps = ((beats) / totalTimeInSecs);// �õ�����Ƶ��
				int dpm = (int) (beats * 60d/totalTimeInSecs);// ����ÿ��������
				if (dpm < 50 || dpm > 180 || imgAvg < 220) {
					// ��ȡϵͳ��ʼʱ�䣨ms�������¿�ʼһ�ּ���
					startTime = System.currentTimeMillis();
					beats = 0;// beats��������
					processing.set(false);
					return;
				}
				// Log.e(TAG, "totalTimeInSecs=" + totalTimeInSecs + " beats="+
				// beats);
				if (beatsIndex == beatsArraySize)
					// ʵ������beatsIndexѭ��
					beatsIndex = 0;

				beatsArray[beatsIndex] = dpm;// ������������
				beatsIndex++;

				// ����Ҫ��ʼ�������������ֵ�ļ�����
				double beatsArrayAvg = 0;// beatsArray����Ч���ʵ�ƽ��ֵ
				int beatsArrayCnt = 0;// beatsArray����Ч�������ĸ���
				for (int i = 0; i < beatsArray.length; i++) {
					if (beatsArray[i] > 0) {
						beatsArrayAvg += beatsArray[i];
						beatsArrayCnt++;
					}
				}
				int beatsAvg = (int) (beatsArrayAvg / beatsArrayCnt);// ƽ������
				info_heart_rate.setText(String.format("%03d", beatsAvg));

				// ���´��룬Ŀ�������¿�ʼһ�����ʼ���
				startTime = System.currentTimeMillis();
				// ���»�ȡ��ȡϵͳʱ�䣨ms��
				beats = 0;
			}
			processing.set(false);
		}
	};

	/**
	 * Ԥ���ص��ӿ�
	 * 
	 * �������ͷ����׽��Ϣ�ı�ʱ����
	 */
	private static SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

		// �ڴ���ʱ������һ����������û�ͼ���߳�
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
				 * re-set. ������
				 * 
				 * @holder - the SurfaceHolder upon which to place the picture
				 *         preview �׳��� IOException - if the method fails.
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
				 * ������ cb - A callback object that receives a copy of each
				 * preview frame. Pass null to stop receiving callbacks at any
				 * time.
				 */
				camera.setPreviewCallback(previewCallback);
			} catch (Throwable t) {
				Log.e("PreviewDemo-setPreviewCallback",
						"Exception in setPreviewDisplay()", t);
			}

		}

		// ��Ԥ���ı��ʱ��ص��˷���
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

		// ���ٵ�ʱ�����
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// Ignore
			Log.v("TAG", "====surfaceDestroyed");
			/**
			 * camera.stopPreview();// stop preview camera.release(); // Release
			 * camera resources camera = null; ��onpause()���Ѿ�ʵ�ֹ��ˣ��ظ�ʵ�ֻᵼ���˳�����ʱ����
			 * 
			 */

		}
	};

	/**
	 * ��ȡ�����С��Ԥ���ߴ緽��
	 * 
	 * �����ǻ�ȡ��ǰ�ֻ������С��Ԥ���ߴ�
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
			// ��������֧�ֵ�Ԥ���ߴ�
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
	}// ���ڰ������˳��Ĵ���

	private void exit() {
		if (!isExit) {
			isExit = true;
			
			toast = Toast.makeText(Heart_Rate_Detect.this, "�ٰ�һ���˳�����",
					Toast.LENGTH_SHORT);
			toast.show();
			// ����handler�ӳٷ��͸���״̬��Ϣ
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
		 * ����ָ���������ָ�����ӳٺ�ʼ�����ظ��Ĺ̶��ӳ�ִ�С�
		 * 
		 * @task - ��Ҫ���ŵ�����
		 * @delay - ִ������ǰ���ӳ�ʱ�䣬��λ�Ǻ��롣
		 * @period - ִ�и���������֮���ʱ��������λ�Ǻ��롣
		 * 
		 * @IllegalArgumentException - ��� delay �Ǹ��������� delay +
		 *                           System.currentTimeMillis() �Ǹ�����
		 * @IllegalStateException - ����Ѿ����Ż�ȡ���������Ѿ�ȡ���˼�ʱ�������߼�ʱ���߳�����ֹ��
		 */
		timer.schedule(task, 1, 50);
		if (button_flag == false) {
			timer_fft.schedule(task_fft, 1, 2500);// fft��ʱ��
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
		 * public void cancel()��ֹ�˼�ʱ�����������е�ǰ�Ѱ��ŵ������ⲻ����ŵ�ǰ����ִ�е�����������ڣ���
		 * һ����ֹ�˼�ʱ������ô����ִ���߳�Ҳ����ֹ�������޷����������Ÿ�������� ע�⣬�ڴ˼�ʱ�����õļ�ʱ������� run
		 * �����ڵ��ô˷������Ϳ��Ծ���ȷ������ִ�е������Ǵ˼�ʱ����ִ�е����һ������ �����ظ����ô˷��������ǵڶ��κͺ���������Ч��
		 */
		timer.cancel();
		timer_fft.cancel();

	};
	
}