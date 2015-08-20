package radiancetops.com.resistora;

import android.hardware.Camera;
import android.util.Log;
import android.widget.TextView;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Sean on 15-08-15.
 */
public class ImageHandler implements Camera.PreviewCallback {

    private int width, height, stripheight;

    private double[] H, S, L;
    private int[] rgb;

    private double[] Ha, Sa, La, diff;

    private int[] idxs;


    TextView rtv;
    MarkerView markerTextView;

    public ImageHandler(int width, int height, int stripheight, TextView rtv,MarkerView markerView) {
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
        this.rtv = rtv;

        this.markerTextView = markerView;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.v("test", "test");
        // Decode the image data to HSL
        decodeNV21(data, width, height);

        // Average data
        avgImg();

        // Find the maxima
        findMaxima();

        colors(idxs, rgb);

        markerTextView.setBandLocation(idxs);

        camera.addCallbackBuffer(data);
    }

    private void colors(int[] idxs, int[] rgb) {
        WIDTH = width;
        HEIGHT = stripheight;
        rgb1 = new int[WIDTH][HEIGHT];
        output1 = new int[WIDTH][HEIGHT];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < stripheight; j++) {
                rgb1[i][j] = rgb[j * width + i];
            }
        }

        initializeColors();
        normalizeSat();
        avgColorStrip();
        replaceColors();

        for(int i = 0; i < idxs.length; i++) {
            /* image is reversed due to rotation */
            idxs[i] = rgb1[width - idxs[i] - 1][0];
        }

        rtv.setText("\n" + resistanceValue(idxs[0], idxs[1], idxs[2], idxs[3]) + "\n" + idxs[0] + " " + idxs[1] + " " + idxs[2] + " " + idxs[3]);
        //rtv.setText(idxs[0] + " " + idxs[1] + " " + idxs[2] + " " + idxs[3]);
    }

    private  String resistanceValue (int a, int b, int c, int tolerance){
        //gold is ten
        int SILVER = 11;
        //silver is eleven
        int GOLD = 10;


        if (a == 10) a = 1;
        if (b == 10) b = 4;
        if (a == 11) a = 8;
        if (b == 11) b = 8;


        int resistance = (int)((10 * a + b)*Math.pow(10,c));
        String value = "\n" + resistance;

        if (tolerance == 8){
            tolerance = 11;
        }
        else tolerance = 10;

        double mult = 1;

        if(tolerance == GOLD) {
            mult = 0.05;
        } else {
            mult = 0.1;
        }

        value+= " ± " + (int)(mult * resistance) + "Ω\n";
        return value;
    }

    private void findMaxima() {
        int[] midx = new int[4];
        for(int i = 7; i < this.width - 7; i++) {
            boolean nvalid = false;
            for(int j = i - 4; j <= i + 4; j++) {
                if(i == j) continue;
                if(diff[j] >= diff[i]) {
                    nvalid = true;
                    break;
                }
            }

            if(!nvalid) {
                if(diff[i] > diff[midx[3]]) {
                    midx[3] = i;
                    for(int q = 3; q >= 1; q--) {
                        if(diff[midx[q]] > diff[midx[q-1]]) {
                            int tmp = midx[q];
                            midx[q] = midx[q-1];
                            midx[q-1] = tmp;
                        }
                    }
                }
            }
        }

        Log.v("idx", midx[0] + " " + midx[1] + " " + midx[2] + " " + midx[3]);

        for(int i = 0; i < 4; i++) {
            /* the image is reversed due to the rotation */
            idxs[i] = width - midx[i] - 1;
        }

        Arrays.sort(idxs);
    }

    private void avgImg() {
        for(int i = 0; i < width; i++) {
            for (int j = 0; j < stripheight; j++) {
                Ha[i] += H[i + j * width];
                Sa[i] += S[i + j * width];
                La[i] += L[i + j * width];
            }
            Ha[i] /= stripheight;
            Sa[i] /= stripheight;
            La[i] /= stripheight;

            diff[i] = Sa[i] - La[i];
        }
    }

	public void writeCSV () {

        Log.v("idx", idxs[0] + " " + idxs[1] + " " + idxs[2] + " " + idxs[3]);
        /*
        double[] h = new double[width], s = new double[width], l = new double[width];
        int[] rgb = new int[width];
        for(int i = 0; i < width; i++) {
                rgb[i] = this.rgb[i + (stripheight / 2) * this.width];
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

    private void decodeNV21(byte[] data, int height, int width) {
        final int frameSize = width * height;

        for (int j = 0; j < this.width; ++j) {
            for (int i = this.height / 2 - stripheight / 2; i < this.height / 2 + stripheight / 2; ++i) {
                int y = (0xff & ((int) data[j * this.height + i]));
                int v = (0xff & ((int) data[frameSize + (j >> 1) * width + (i & ~1) + 0]));
                int u = (0xff & ((int) data[frameSize + (j >> 1) * width + (i & ~1) + 1]));

                int a = (i - (this.height / 2 - stripheight / 2)) * this.width + j;

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


    private static int WIDTH, HEIGHT;
    private static int[][] rgb1;
    private static int[][] output1;
    private static int[] presetRGB = new int[20];
    private static double avgr, avgg, avgb, avgsat;

    private static void initializeColors () {
        presetRGB[0] = rgbToInt(0,0,0);
        presetRGB[1] = rgbToInt(102, 51, 50);
        presetRGB[2] = rgbToInt(255,0,0);
        presetRGB[3] = rgbToInt(255, 102, 0);
        presetRGB[4] = rgbToInt(255, 255, 0);
        presetRGB[5] = rgbToInt(0, 255, 0);
        presetRGB[6] = rgbToInt(0, 0, 255);
        presetRGB[7] = rgbToInt(206, 101, 255);
        presetRGB[8] = rgbToInt(130, 130, 130);
        presetRGB[9] = rgbToInt(255, 255, 255);
        presetRGB[10] = rgbToInt(205, 153, 51);
        presetRGB[11] = rgbToInt(204, 204, 204);
    }
    private static void normalizeSat () {
        avgsat = 0;
        for (int i = 0; i < WIDTH; i++)
            for (int j = 0; j < HEIGHT; j++)
                avgsat += toHSL(new Tuple(getRed(rgb1[i][j]), getGreen(rgb1[i][j]), getBlue(rgb1[i][j]))).val[1];
        avgsat /= HEIGHT * WIDTH;

        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                Tuple HSL = toHSL(new Tuple(getRed(rgb1[i][j]), getGreen(rgb1[i][j]), getBlue(rgb1[i][j])));
                HSL.val[1] = Math.min(1.0, HSL.val[1] / avgsat / 2);
                Tuple RGB = toRGB(HSL);
                rgb1[i][j] = rgbToInt((int)RGB.val[0], (int)RGB.val[1], (int)RGB.val[2]);
            }
        }
    }
    private static void avgColorStrip () {
        for (int i = 0; i < WIDTH; i++) {
            avgr = 0;
            avgg = 0;
            avgb = 0;
            for (int j = 0; j < HEIGHT; j++) {
                avgr += getRed(rgb1[i][j]);
                avgg += getGreen(rgb1[i][j]);
                avgb += getBlue(rgb1[i][j]);
            }
            avgr /= HEIGHT;
            avgg /= HEIGHT;
            avgb /= HEIGHT;
            for (int j = 0; j < HEIGHT; j++)
                rgb1[i][j] = rgbToInt((int)avgr, (int)avgg, (int)avgb);
        }
    }
    private static void replaceColors () {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                rgb1[i][j] = getResistorColor(rgb1[i][j]);
                output1[i][j] = rgb1[i][j];
            }
        }
    }
    private static int getResistorColor (int rgb) {
        int r = getRed(rgb);
        int g = getGreen(rgb);
        int b = getBlue(rgb);
        Tuple HSL = toHSL(new Tuple(r, g, b));
        // BLACK AND WHITE
        if (HSL.val[2] < 0.13) return 0;
        if (HSL.val[2] > 0.90) return 9;

        if (Math.max(r, Math.max(g, b)) - Math.min(r,  Math.min(g,b)) < 10){
            if ((r+g+b)/3 > 160) return 8;
            else return 11;

        }
        if (HSL.val[0] > 0.95 || HSL.val[0] < 0.093){ // red,orange or brown
            if (((HSL.val[2] < 0.32 || HSL.val[1]<0.51) && (HSL.val[0]>0.01 && HSL.val[0] < 0.04)) || ((HSL.val[2]<0.29 || HSL.val[1] < 0.42) && HSL.val[0]>=0.05 && HSL.val[0] <= 0.093)) return 1;
            else if ( HSL.val[0]>0.9 || HSL.val[0] < 0.05) return 2;
            else return 3;
        }
        if (HSL.val[0] >= 0.093 && HSL.val[0] < 0.21){
            if (HSL.val[1] < 0.5 || HSL.val[2] < 0.27) return 10;
            else return 4;
        }

        if (HSL.val[0] >= 0.21 && HSL.val[0] < 0.49)
            return 5;
        if (HSL.val[0] >= 0.49 && HSL.val[0] < 0.69)
            return 6;
        if (HSL.val[0]>=0.69 && HSL.val[0] <= 0.95)
            return 7;

        return 12;


    }
    // get the R value (0, 255) from a 32 bit integer
    private static int getRed (int n) {
        return 0xFF & (n >> 16);
    }
    // get the G value (0, 255) from a 32 bit integer
    private static int getBlue (int n) {
        return 0xFF & (n >> 0);
    }
    // get the B value (0, 255) from a 32 bit integer
    private static int getGreen (int n) {
        return 0xFF & (n >> 8);
    }
    private static Tuple toHSL (Tuple rgb) {
        double r = rgb.val[0] / 255.0; // RED
        double g = rgb.val[1] / 255.0; // GREEN
        double b = rgb.val[2] / 255.0; // BLUE
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double h = (max + min) / 2.0;
        double s = (max + min) / 2.0;
        double l = (max + min) / 2.0;
        if (max == min) {
            h = s = 0;
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else if (max == b) {
                h = (r - g) / d + 4;
            }
            h /= 6.0;
        }
        return new Tuple(h, s, l);
    }
    private static int rgbToInt(int r, int g, int b){
        int a = 255;
        return (((a<<8)+r<<8)+g<<8)+b;
    }
    private static Tuple toRGB (Tuple HSL) {
        double h = HSL.val[0];
        double s = HSL.val[1];
        double l = HSL.val[2];
        double r = 0, g = 0, b = 0;
        if (s == 0) {
            r = g = b = 1;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRGB(p, q, (h + 1.0d/3.0d));
            g = hueToRGB(p, q, h);
            b = hueToRGB(p, q, (h - 1.0d/3.0d));
        }
        return new Tuple(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }
    private static double hueToRGB (double p, double q, double t) {
        if(t < 0.0d) t += 1;
        if(t > 1.0d) t -= 1;
        if(t < 1.0d/6.0d) return p + (q - p) * 6 * t;
        if(t < 1.0d/2.0d) return q;
        if(t < 2.0d/3.0d) return p + (q - p) * (2.0/3.0 - t) * 6;
        return p;
    }
    private static class Tuple {
        double[] val;
        Tuple (double... args) {
            val = new double[args.length];
            for (int i = 0; i < args.length; i++)
                val[i] = args[i];
        }
    }
}
