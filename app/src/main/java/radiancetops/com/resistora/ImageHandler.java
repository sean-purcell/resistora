package radiancetops.com.resistora;

import android.hardware.Camera;
import android.util.Log;
import java.io.*;

/**
 * Created by Sean on 15-08-15.
 */
public class ImageHandler implements Camera.PreviewCallback {

    private int width, height, stripheight;

    private double[] H, S, L;
    private int[] rgb;

    private double[] Ha, Sa, La, diff;

    private int[] idxs;

    public ImageHandler(int width, int height, int stripheight) {
        super();

        this.width = height;
        this.height = width;
        this.stripheight = stripheight;

        this.H = new double[width * stripheight];
        this.S = new double[width * stripheight];
        this.L = new double[width * stripheight];
        this.rgb = new int[width * stripheight];

        this.Ha = new double[width];
        this.Sa = new double[width];
        this.La = new double[width];
        this.diff = new double[width];

        this.idxs = new int[4];
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // Decode the image data to HSL
        decodeNV21(data, width, height);

        // Average data
        avgImg();

        // Find the maxima
    }

    private void avgImg() {
        for(int i = 0; i < width; i++) {
            for (int j = 0; j < stripheight; j++) {
                Ha[i] += H[i + j * width];
                Sa[i] += H[i + j * width];
                La[i] += H[i + j * width];
            }
            Ha[i] /= stripheight;
            Sa[i] /= stripheight;
            La[i] /= stripheight;

            diff[i] = Sa[i] - La[i];
        }
    }

	public void writeCSV () {
        double[] h = new double[width], s = new double[width], l = new double[width];
        int[] rgb = new int[width];
        for(int i = 0; i < width; i++) {
                rgb[i] = this.rgb[i * stripheight + stripheight / 2];
            h[i] = Ha[i];
            s[i] = Sa[i];
            l[i] = La[i];
        }
        String csv = "";
        Log.v("data", "w: " + width);
        for (int i = 0; i < width; i++) {
            //Log.v("data", h[i] + ","+ s[i]+ "," + l[i]);
            Log.v("rgb", i + "," + (0xff & (rgb[i] >> 16)) + "," + (0xff & (rgb[i] >> 8)) + "," + (0xff & (rgb[i])));
        }
        /*
		try {
			PrintWriter pw = new PrintWriter(new FileWriter("data.csv"));
			for (int i = 0; i < width; i++) {
				pw.println(Ha[i] + ","+ Sa[i]+ "," + La[i]);
			}
			pw.close();
		} catch (IOException e) {}
		*/
    }

    private void decodeNV21(byte[] data, int width, int height) {
        final int frameSize = width * height;

        int a = 0;

        for (int j = 0; j < width; ++j) {
            for (int i = height / 2 - stripheight / 2; i < height / 2 + stripheight / 2; ++i) {
                int y = (0xff & ((int) data[j * height + i]));
                int v = (0xff & ((int) data[frameSize + (j >> 1) * width + (i & ~1) + 0]));
                int u = (0xff & ((int) data[frameSize + (j >> 1) * width + (i & ~1) + 1]));

                int rgb = this.rgb[a] = YUVtoRGB(y, u, v);

                double r = (0xff & (rgb >> 16)) / 255.;
                double g = (0xff & (rgb >>  8)) / 255.;
                double b = (0xff & (rgb >>  0)) / 255.;

                double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));

                L[a] = ((max + min) / 2);

                if(max == min){
                    H[a] = S[a] = 0; // achromatic
                } else {
                    double d = max - min;
                    S[a] = L[a] > 0.5 ? d / (double) (2 - max - min) : d / (double) (max + min);
                    if (max == r) {
                        H[a] = (g - b) / (double) d + (g < b ? 6 : 0);
                    } else if (max == g) {
                        H[a] = (b - r) / (double) d + 1;
                    } else {
                        H[a] = (r - g) / (double) d + 4;
                    }
                    H[a] /= 6;
                }
                a++;
            }
        }
    }

    private int YUVtoRGB(int y, int u, int v) {
        y = y < 16 ? 16 : y;

        int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
        int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
        int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

        r = r < 0 ? 0 : (r > 255 ? 255 : r);
        g = g < 0 ? 0 : (g > 255 ? 255 : g);
        b = b < 0 ? 0 : (b > 255 ? 255 : b);

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}
