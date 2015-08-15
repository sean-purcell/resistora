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

        this.width = width;
        this.height = height;
        this.stripheight = stripheight;

        this.H = new double[width * stripheight];
        this.S = new double[width * stripheight];
        this.L = new double[width * stripheight];

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
        String csv = "";
        for (int i = 0; i < width; i++) {
            csv+=(Ha[i] + ","+ Sa[i]+ "," + La[i]+"\n");
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
        Log.v("data",csv);
	}

    private void decodeNV21(byte[] data, int width, int height) {
        final int frameSize = width * height;

        int a = 0;
        for (int i = height / 2 - stripheight / 2; i < height / 2 + stripheight / 2; ++i) {
            for (int j = 0; j < width; ++j) {
                int y = (0xff & ((int) data[i * width + j]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));

                int rgb = this.rgb[a] = YUVtoRGB(y, u, v);

                int r = 0xff & (rgb >> 16);
                int g = 0xff & (rgb >>  8);
                int b = 0xff & (rgb >>  0);

                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));

                L[a] = ((max + min) / 2) / 255.;

                if(max == min){
                    H[a] = S[a] = 0; // achromatic
                } else {
                    int d = max - min;
                    S[a] = L[a] > 0.5 ? d / (double) (510 - max - min) : d / (double) (max + min);
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

        int a0 = 1192 * (y - 16);
        int a1 = 1634 * (v - 128);
        int a2 = 832 * (v - 128);
        int a3 = 400 * (u - 128);
        int a4 = 2066 * (u - 128);

        int r = (a0 + a1) >> 10;
        int g = (a0 - a2 - a3) >> 10;
        int b = (a0 + a4) >> 10;

        r = r < 0 ? 0 : (r > 255 ? 255 : r);
        g = g < 0 ? 0 : (g > 255 ? 255 : g);
        b = b < 0 ? 0 : (b > 255 ? 255 : b);

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}
