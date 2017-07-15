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

	private static int length = 64; // ʱ�������鳤��
	private static double sigl_array[] = new double[length]; // ��������ʱ��������
	private static double result_fft[] = new double[length]; // fft�任���
	private static double xlim_fft[] = new double[length];

	private static String title_fft = "FFT"; // ������
	private XYSeries series_fft; // fft ����ϵ������
	private XYMultipleSeriesDataset mDataset_fft; // ����series����ϵ�е����ݼ�
	private GraphicalView chart_fft; // ͼ����
	private static XYMultipleSeriesRenderer renderer_fft; // ��Ⱦ���Ƶ�����
	private static Context context; // Ӧ��������
	static double SNratio = 0; // ��ɢ�źű任�õ��������

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		/* ����ҳ�� */
		setContentView(R.layout.deep_compute);

		/* ��ȡIntent�е�Bundle���� */
		Bundle bundle = this.getIntent().getExtras();

		/* ��ȡBundle�е����ݣ�ע�����ͺ�key */
		sigl_array = bundle.getDoubleArray("sigl_array");

		double sum=0;
		for (int i = 0; i < length; i++) {
			sum+=sigl_array[i];
		}
		for (int i = 0; i < length; i++) {
			sigl_array[i]=10*(sigl_array[i]-sum/length);
		}
		
		context = getApplicationContext();

		init_chart_fft(); // ��ʼ��FFT�任ͼ��

		Compute_Result result = new Compute_Result();
		
		SNratio = result.getSNR(sigl_array); // ��������

		result_fft = result.fftCalculator(sigl_array); // ���FFT�任���

		for (int i = 0; i < length; i++) {
			//xlim_fft[i] = i * 25 / length;
			xlim_fft[i] = i ;
		}

		// �㼯����գ�Ϊ�������µĵ㼯��׼��
		// series_fft.clear();
		// mDataset_fft.addSeries(series_fft);

		for (int i = 0; i < length; i++) {
			// �����ݼ�������µĵ㼯
			series_fft.add(xlim_fft[i], result_fft[i]);
		}

		// ������ʵ�ֵ�һ����������,���ú�ͼ�����ʽ
		setChartSettings(title_fft, renderer_fft, "Ƶ��/Hz", "���", 0, 63, 0,
				result_fft[max(result_fft)], Color.WHITE, Color.WHITE);
		
		//Log.e("deep_comp",String.valueOf(result_fft.length)+" "+String.valueOf(xlim_fft.length));

		TextView num = (TextView) findViewById(R.id.info_deep);
		num.setText(String.valueOf(result_fft.length));
		
		//chart_fft.invalidate();
	}

	protected void init_chart_fft() {
		LinearLayout layout_fft = (LinearLayout) findViewById(R.id.chart_fft);

		// �������������һ��ϵ�������ϵ����е㣬��һ����ļ��ϣ�������Щ�㻭�����ߣ����췽������ֻ��ϵ������
		series_fft = new XYSeries(title_fft);

		// ����һ����ϵ�����ݼ���ʵ����������ݼ�������������ͼ�����ݼ����԰���0-�������ϵ��
		mDataset_fft = new XYMultipleSeriesDataset();

		// ��serial�㼯��ӵ�������ݼ���
		mDataset_fft.addSeries(series_fft);

		// ���¶������ߵ���ʽ�����Եȵȵ����ã�renderer�൱��һ��������ͼ������Ⱦ�ľ��
		int color = Color.RED;
		PointStyle style = PointStyle.CIRCLE;
		/**
		 * ���˴�style��ֵΪnull�ᵼ��preview�޷����أ�Ȼ��mainactivity������Ѫ���۵Ľ�ѵ
		 */

		// ������ʵ�ֵ�һ����������������ͼ����ɫ�ͷ��
		renderer_fft = buildRenderer(color, style, true);

		// ����ͼ��
		chart_fft = ChartFactory.getLineChartView(context, mDataset_fft,
				renderer_fft);

		// ��ͼ����ӵ�������ȥ
		layout_fft.addView(chart_fft, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
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
	    renderer.setPointSize(4);//���õ�Ĵ�С(ͼ����ʾ�ĵ�Ĵ�С��ͼ���е�Ĵ�С���ᱻ����)
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

	public static int max(double[] table) {
		// �������������ֵ�����
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
		// ��д���ؼ����������ظ�����
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent();
			intent.setClass(this, Heart_Rate_Detect.class);
			startActivity(intent);
			this.finish();
		}
		return false;
	}

}