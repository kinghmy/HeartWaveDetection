package com.tools_set.FFTtrans;
/*This function replaces your inputs arrays with the FFT output.

Input

N = the number of data points (the size of your input array, must be a power of 2)
X = the real part of your data to be transformed
Y = the imaginary part of the data to be transformed
i.e. if your input is (1+8i, 2+3j, 7-i, -10-3i)

N = 4
X = (1, 2, 7, -10)
Y = (8, 3, -1, -3)
Output

X = the real part of the FFT output
Y = the imaginary part of the FFT output
To get your classic FFT graph, you will want to calculate the magnitude of the real and imaginary parts.

Something like:

public double[] fftCalculator(double[] re, double[] im) {
    if (re.length != im.length) return null;
    FFT fft = new FFT(re.length);
    fft.fft(re, im);
    double[] fftMag = new double[re.length];
    for (int i = 0; i < re.length; i++) {
       fftMag[i] = Math.pow(re[i], 2) + Math.pow(im[i], 2);
    }
    return fftMag;
}
*/

public class FFT {

	  int n, m;
	  // Lookup tables. Only need to recompute when size of FFT changes.
	  double[] cos;
	  double[] sin;
	  
	  public FFT(int n) {
	      this.n = n;
	      this.m = (int) (Math.log(n) / Math.log(2));

	      // Make sure n is a power of 2
	      if (n != (1 << m))
	          throw new RuntimeException("FFT length must be power of 2");

	      // precompute tables
	      cos = new double[n / 2];
	      sin = new double[n / 2];

	      for (int i = 0; i < n / 2; i++) {
	          cos[i] = Math.cos(-2 * Math.PI * i / n);
	          sin[i] = Math.sin(-2 * Math.PI * i / n);
	      }

	  }

	  public void fft(double[] x, double[] y) {
	      int i, j, k, n1, n2, a;
	      double c, s, t1, t2;

	      // Bit-reverse
	      j = 0;
	      n2 = n / 2;
	      for (i = 1; i < n - 1; i++) {
	          n1 = n2;
	          while (j >= n1) {
	              j = j - n1;
	              n1 = n1 / 2;
	          }
	          j = j + n1;

	          if (i < j) {
	              t1 = x[i];
	              x[i] = x[j];
	              x[j] = t1;
	              t1 = y[i];
	              y[i] = y[j];
	              y[j] = t1;
	          }
	      }

	      // FFT
	      n1 = 0;
	      n2 = 1;

	      for (i = 0; i < m; i++) {
	          n1 = n2;
	          n2 = n2 + n2;
	          a = 0;

	          for (j = 0; j < n1; j++) {
	              c = cos[a];
	              s = sin[a];
	              a += 1 << (m - i - 1);

	              for (k = j; k < n; k = k + n2) {
	                  t1 = c * x[k + n1] - s * y[k + n1];
	                  t2 = s * x[k + n1] + c * y[k + n1];
	                  x[k + n1] = x[k] - t1;
	                  y[k + n1] = y[k] - t2;
	                  x[k] = x[k] + t1;
	                  y[k] = y[k] + t2;
	              }
	          }
	      }
	  }
	}