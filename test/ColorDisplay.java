import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;
public class ColorDisplay {
	private static int WIDTH, HEIGHT;
	private static int[][] rgb;
	private static boolean[][] v;
	private static int[][] output;
	private static int currColor;


	private static final double DIFF = 0.004;
	private static int maxh, minh, maxw, minw;
	private static int cnt;
	private static double avghue, avgsat, avgbright;
	public static void main (String[] args) throws IOException {
		BufferedImage bi = ImageIO.read(new File("resistor2.png"));
		WIDTH = bi.getWidth();
		HEIGHT = bi.getHeight();
		rgb = new int[WIDTH][HEIGHT];
		output = new int[WIDTH][HEIGHT];
		v = new boolean[WIDTH][HEIGHT];
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				rgb[i][j] = bi.getRGB(i, j);
				output[i][j] = bi.getRGB(i, j);
			}
		}

		double[] hues = new double[WIDTH];
		double[] sats = new double[WIDTH];
		double[] lits = new double[WIDTH];
		for(int i = 0; i < WIDTH; i++) {
			for(int j = 0; j < HEIGHT; j++) {
				Tuple t = toHSL(new Tuple(getRed(rgb[i][j]), getGreen(rgb[i][j]), getBlue(rgb[i][j])));
				hues[i] += t.val[0];
				sats[i] += t.val[1];
				lits[i] += t.val[2];
			}
		}
		
		for(int i = 0; i < WIDTH; i++) {
			System.out.println(hues[i] + "," + sats[i] + "," + lits[i]);
		}
		/*
		for (int i = 0; i < WIDTH; i++) {
			if (!v[i][HEIGHT/2]) {
				currColor = (int)(Math.random() * Integer.MAX_VALUE) | 255 << 24;
				maxh = 0;
				minh = HEIGHT;
				maxw = 0;
				minw = WIDTH;
				avghue = 0;
				avgsat = 0;
				avgbright = 0;
				cnt = 0;
				fill(i, HEIGHT/2);
				avghue /= cnt;
				avgsat /= cnt;
				avgbright /= cnt;
				v = new boolean[WIDTH][HEIGHT];
				if (maxw - minw > 3 && maxh - minh > HEIGHT/2 && maxw - minw < WIDTH/5) {
					fillSame(i, HEIGHT/2);
					System.out.println(avghue + " " + avgsat + " " + avgbright);
					getColor(avghue, avgsat, avgbright);
				}
				
			} 
		}

		BufferedImage res = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < WIDTH; i++) {
			for (int j = 0; j < HEIGHT; j++) {
				res.setRGB(i, j, output[i][j]);
			}
		}
		ImageIO.write(res, "png", new File("output.png"));*/
	}
	private static void getColor (double avgsat, double avghue, double avgbright) {
		if (avgbright < 0.1) {
			System.out.println("Black");
		}
		if (Math.min(Math.abs(avgsat - 0.0), Math.abs(avgsat - 1.0)) < 0.05) {
			System.out.println("RED");
		} else if (Math.abs(avgsat - 0.16) < 0.05) {
			System.out.println("YELLOW");
		} else if (Math.abs(avgsat - 0.33) < 0.05) {
			System.out.println("GREEN");
		} else if (Math.abs(avgsat - 0.66) < 0.05) {
			System.out.println("BLUE");
		} else if (Math.abs(avgsat - 0.83) < 0.05) {
			System.out.println("VIOLET");
		} else {
			System.out.println("NOTHING");
		}
	}
	private static void fillSame (int w, int h) {
//		output[w][h] = 0;
		for (int dh = -1; dh <= 1; dh++) {
			for (int dw = -1; dw <= 1; dw++) {
				int nh = h + dh;
				int nw = w + dw;
				if (0 <= nh && nh < HEIGHT && 0 <= nw && nw < WIDTH && !v[nw][nh] && currColor == output[nw][nh]) {
					v[nw][nh] = true;
					fillSame(nw, nh);
				}
			}
		}
	}
	private static void fill (int w, int h) {
		cnt++;
		Tuple hsl = toHSL(new Tuple(getRed(rgb[w][h]), getGreen(rgb[w][h]), getBlue(rgb[w][h])));
		maxh = Math.max(maxh, h);
		minh = Math.min(minh, h);

		maxw = Math.max(maxw, w);
		minw = Math.min(minw, w);
		
		avghue += hsl.val[0];
		avgsat += hsl.val[1];
		avgbright += hsl.val[2];
		
		for (int dh = -1; dh <= 1; dh++) {
			for (int dw = -1; dw <= 1; dw++) {
				int nh = h + dh;
				int nw = w + dw;
				if (0 <= nh && nh < HEIGHT && 0 <= nw && nw < WIDTH && !v[nw][nh] && getDiff(rgb[w][h], rgb[nw][nh]) < DIFF) {
					v[nw][nh] = true;
					output[nw][nh] = currColor;
					fill(nw, nh);
				}
			}
		}
	}

	private static double getDiff (int i, int j) {
		double h1 = toHSL(new Tuple(getRed(i), getGreen(i), getBlue(i))).val[0];
		double h2 = toHSL(new Tuple(getRed(j), getGreen(j), getBlue(j))).val[0];
		double s1 = toHSL(new Tuple(getRed(i), getGreen(i), getBlue(i))).val[0];
		double s2 = toHSL(new Tuple(getRed(j), getGreen(j), getBlue(j))).val[0];
		return Math.min(Math.abs(Math.abs(h1 - h2)-1.0), Math.abs(h1 - h2)) + Math.min(Math.abs(Math.abs(s1 - s2)-1.0), Math.abs(s1 - s2));
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
	private static Tuple toRBG (Tuple HSL) {
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
			b = hueToRGB(p, q, (h - 1.0/3.0));
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