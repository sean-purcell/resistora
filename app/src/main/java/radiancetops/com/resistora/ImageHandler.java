package radiancetops.com.resistora;

import android.hardware.Camera;

/**
 * Created by Sean on 15-08-15.
 */
public class ImageHandler implements Camera.PreviewCallback {

    private int width, height;

    private double[] H, S, L;

    public ImageHandler(int width, int height) {
        super();

        this.width = width;
        this.height = height;

        this.H = new double[width * height];
        this.S = new double[width * height];
        this.L = new double[width * height];
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        decodeNV21(data, width, height);
    }

    private void decodeNV21(byte[] data, int width, int height) {
        final int frameSize = width * height;

        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) data[ci * width + cj]));
                int v = (0xff & ((int) data[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
                int u = (0xff & ((int) data[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));

                int rgb = YUVtoRGB(y, u, v);

                int r = 0xff & (rgb >> 16);
                int g = 0xff & (rgb >>  8);
                int b = 0xff & (rgb >>  0);

                int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));

                L[a] = ((max + min) / 2) / 255.;

                if(max == min){
                    H[a] = S[a] = 0; // achromatic
                } else {
                    int d = max - min;
                    S[a] = L[a] > 127 ? d / (double) (510 - max - min) : d / (double) (max + min);
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
