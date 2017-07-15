package com.heartrate.activity;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.Math;

import com.tools_set.FFTtrans.Compute_Result;

public class Deep_Comp extends Activity {

	private static int length = 64; // 时间域数组长度
	private static double sigl_array[] = new double[length]; // 用来接受时间域序列
	private static double result_fft[] = new double[length]; // fft变换结果
	private static double xlim_fft[] = new double[length];

	private static String title_fft = "FFT"; // 表格标题
	private XYSeries series_fft; // fft 曲线系列数据
	private XYMultipleSeriesDataset mDataset_fft; // 容纳series数据系列的数据集
	private GraphicalView chart_fft; // 图表工厂
	private static XYMultipleSeriesRenderer renderer_fft; // 渲染绘制的曲线
	private static Context context; // 应用上下文
	static double SNratio = 0; // 离散信号变换得到的信噪比

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* 加载页面 */
		setContentView(R.layout.deep_compute);

		/* 获取Intent中的Bundle对象 */
		Bundle bundle = this.getIntent().getExtras();

		/* 获取Bundle中的数据，注意类型和key */
		sigl_array = bundle.getDoubleArray("sigl_array");

		double sum=0;
		for (int i = 0; i < length; i++) {
			sum+=sigl_array[i];
		}
		for (int i = 0; i < length; i++) {
			sigl_array[i]=10*(sigl_array[i]-sum/length);
		}
		
		context = getApplicationContext();

		init_chart_fft(); // 初始化FFT变换图表

		Compute_Result result = new Compute_Result();
		
		SNratio = result.getSNR(sigl_array); // 获得信噪比

		result_fft = result.fftCalculator(sigl_array); // 获得FFT变换结果

		for (int i = 0; i < length; i++) {
			//xlim_fft[i] = i * 25 / length;
			xlim_fft[i] = i ;
		}

		// 点集先清空，为了做成新的点集而准备
		// series_fft.clear();
		// mDataset_fft.addSeries(series_fft);

		for (int i = 0; i < length; i++) {
			// 在数据集中添加新的点集
			series_fft.add(xlim_fft[i], result_fft[i]);
		}

		// 主类中实现的一个保护方法,设置好图表的样式
		setChartSettings(title_fft, renderer_fft, "频率/Hz", "振幅", 0, 63, 0,
				result_fft[max(result_fft)], Color.WHITE, Color.WHITE);
		
		//Log.e("deep_comp",String.valueOf(result_fft.length)+" "+String.valueOf(xlim_fft.length));

		TextView num = (TextView) findViewById(R.id.info_deep);
		num.setText(String.valueOf(result_fft.length));
		
		//chart_fft.invalidate();
	}

	protected void init_chart_fft() {
		LinearLayout layout_fft = (LinearLayout) findViewById(R.id.chart_fft);

		// 这个类用来放置一个系列曲线上的所有点，是一个点的集合，根据这些点画出曲线，构造方法参数只是系列名称
		series_fft = new XYSeries(title_fft);

		// 创建一个多系列数据集的实例，这个数据集将被用来创建图表，数据集可以包含0-多个数据系列
		mDataset_fft = new XYMultipleSeriesDataset();

		// 将serial点集添加到这个数据集中
		mDataset_fft.addSeries(series_fft);

		// 以下都是曲线的样式和属性等等的设置，renderer相当于一个用来给图表做渲染的句柄
		int color = Color.RED;
		PointStyle style = PointStyle.CIRCLE;
		/**
		 * 若此处style赋值为null会导致preview无法加载，然后mainactivity崩掉，血与累的教训
		 */

		// 主类中实现的一个保护方法，设置图表颜色和风格
		renderer_fft = buildRenderer(color, style, true);

		// 生成图表
		chart_fft = ChartFactory.getLineChartView(context, mDataset_fft,
				renderer_fft);

		// 将图表添加到布局中去
		layout_fft.addView(chart_fft, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
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
	    renderer.setPointSize(4);//设置点的大小(图上显示的点的大小和图例中点的大小都会被设置)
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

	public static int max(double[] table) {
		// 返回数组中最大值的序号
		if (table.length == 0) {
			return -1;
		} else {
			int index = 0;
			// double c = table[0];
			for (int i = 1; i < table.length; i++) {
				if (table[i] > table[index]) {
					index = i;
				}
			}
			return index;
		}
	}

	public boolean OnKeyDown(int keyCode, KeyEvent event) {
		// 重写返回键方法，返回父界面
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent();
			intent.setClass(this, Heart_Rate_Detect.class);
			startActivity(intent);
			this.finish();
		}
		return false;
	}

}