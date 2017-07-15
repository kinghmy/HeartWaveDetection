package com.tools_set.FFTtrans;

import android.util.Log;

import com.tools_set.FFTtrans.FFT;

public class Compute_Result {

	static int length = 64;

	static double fft_result[] = new double[length];
	static double all_sigl = 0;
	static double sigl = 0;

	public double getSNR(double[] Discrete) {
		// 返回最终一段，长度为128离散序列的信噪比：1-3Hz内信号占总信号的比率
		
			fft_result = fftCalculator(Discrete);
		
		for (int i = 0; i < length; i++) {
			all_sigl += fft_result[i];
			if (i * 20 / length >= 1 && i * 20 / length <= 3) {
				sigl += fft_result[i];
			}
		}

		Log.e("comput",String.valueOf(sigl)+" "+String.valueOf(all_sigl));

		return sigl / all_sigl;
	}

	public  double getPeakFre(double[] Discrete){
		// 返回最终一段，长度为128离散序列的峰值频率值

		fft_result = fftCalculator(Discrete);
	
		return max(fft_result)*20/length;
		// 0.375=24(采样率)/64
	}

	public  double[] fftCalculator(double[] re) {
		// 将FFT变换后的实部和虚部，变为实数幅值
		double[] im = new double[length];
		if (re.length != im.length)
			return null;
		FFT sample = new FFT(re.length);
		sample.fft(re, im);
		double[] fftMag = new double[re.length];
		for (int i = 0; i < re.length; i++) {
			fftMag[i] = Math.sqrt(Math.pow(re[i], 2) + Math.pow(im[i], 2));
		}
		
		Log.e("comput",String.valueOf(fftMag[5])+" "+String.valueOf(fftMag[10]));
		
		return fftMag;
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
}