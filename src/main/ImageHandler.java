package main;


import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class ImageHandler {

	static double[][] KERNEL_GAUSSIAN = { { 0.0625, 0.125, 0.0625 }, { 0.125, 0.25, 0.125 }, { 0.0625, 0.125, 0.0625 } };

	static double[][] KERNEL_LAPLACIAN = { { 0, -1, 0 }, { -1, 4, -1 }, { 0, -1, 0 } };

	static double[][] KERNEL_HIGHPASS = { { -1, -1, -1 }, { -1, 8, -1 }, { -1, -1, -1 } };

	static double[][] KERNEL_PREWITT_X = { { -1, 0, 1 }, { -1, 0, 1 }, { -1, 0, 1 } };

	static double[][] KERNEL_PREWITT_Y = { { -1, -1, -1 }, { 0, 0, 0 }, { 1, 1, 1 } };

	static double[][] KERNEL_SOBEL_X = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };

	static double[][] KERNEL_SOBEL_Y = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };

	public ImageHandler() {

	}

	static BufferedImage imgToGrayscale(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();

		BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = img.getRGB(x, y);

				int alpha = (pixel >> 24) & 0xff;
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = pixel & 0xff;

				int luminance = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

				pixel = (alpha << 24) | (luminance << 16) | (luminance << 8) | luminance;

				grayImg.setRGB(x, y, pixel);
			}
		}

		return grayImg;
	}

	static BufferedImage imgFlipHorizontal(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int type = img.getType();

		BufferedImage flippedImg = new BufferedImage(width, height, type);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				int pixel = img.getRGB(x, y);

				// possible optimization
				// img.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
				int invertedX = width - x - 1;

				flippedImg.setRGB(invertedX, y, pixel);

				// possible optimization
				// flippedImg.setRGB(startX, startY, w, h, rgbArray, offset, scansize);

			}
		}

		return flippedImg;
	}

	static BufferedImage imgFlipVertical(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int type = img.getType();

		BufferedImage flippedImg = new BufferedImage(width, height, type);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				int pixel = img.getRGB(x, y);
				// img.getRGB(startX, startY, w, h, rgbArray, offset, scansize);
				int invertedY = height - y - 1;

				flippedImg.setRGB(x, invertedY, pixel);
				// flippedImg.setRGB(startX, startY, w, h, rgbArray, offset, scansize);

			}
		}

		return flippedImg;
	}

	static BufferedImage imgRotateCW(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int type = img.getType();

		BufferedImage rotatedImg = new BufferedImage(height, width, type);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				int pixel = img.getRGB(x, y);

				int clockwiseX = height - 1 - y;
				int clockwiseY = x;

				rotatedImg.setRGB(clockwiseX, clockwiseY, pixel);
			}
		}

		return rotatedImg;
	}

	static BufferedImage imgRotateCCW(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		int type = img.getType();

		BufferedImage rotatedImg = new BufferedImage(height, width, type);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				int pixel = img.getRGB(x, y);

				int clockwiseX = y;
				int clockwiseY = width - 1 - x;

				rotatedImg.setRGB(clockwiseX, clockwiseY, pixel);
			}
		}

		return rotatedImg;
	}

	private static int handleLimits(int colorValue) {
		if (colorValue < 0) {
			return 0;
		} else if (255 < colorValue) {
			return 255;
		} else {
			return colorValue;
		}
	}

	private static double handleLimits(double colorValue) {
		if (colorValue < 0) {
			return 0;
		} else if (255 < colorValue) {
			return 255;
		} else {
			return colorValue;
		}
	}

	static BufferedImage imgBrightness(BufferedImage img, int bias) {
		int width = img.getWidth();
		int height = img.getHeight();

		BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		if (bias < -255) {
			bias = -255;
		} else if (255 < bias) {
			bias = 255;
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = img.getRGB(x, y);

				int alpha = (pixel >> 24) & 0xff;
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = pixel & 0xff;

				red = handleLimits(red + bias);
				green = handleLimits(green + bias);
				blue = handleLimits(blue + bias);

				// Replace RGB value with new values
				pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;

				newImg.setRGB(x, y, pixel);
			}
		}

		return newImg;
	}

	static BufferedImage imgContrast(BufferedImage img, double gain) {
		int width = img.getWidth();
		int height = img.getHeight();

		BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		if (gain < 0) {
			gain = 0;
		} else if (255 < gain) {
			gain = 255;
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = img.getRGB(x, y);

				int alpha = (pixel >> 24) & 0xff;
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = pixel & 0xff;

				red = (int) handleLimits(red * gain);
				green = (int) handleLimits(green * gain);
				blue = (int) handleLimits(blue * gain);

				// Replace RGB value with new values
				pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;

				newImg.setRGB(x, y, pixel);
			}
		}

		return newImg;
	}

	static BufferedImage imgNegative(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();

		BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = img.getRGB(x, y);

				int alpha = (pixel >> 24) & 0xff;
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = pixel & 0xff;

				// negative
				red = 255 - red;
				green = 255 - green;
				blue = 255 - blue;

				// replace RGB value with negative
				pixel = (alpha << 24) | (red << 16) | (green << 8) | blue;

				grayImg.setRGB(x, y, pixel);
			}
		}

		return grayImg;
	}

	static BufferedImage imgConvolution(BufferedImage img, double[][] kernel) {

		int width = img.getWidth();
		int height = img.getHeight();

		int[][] luminanceMatrix = new int[width][height];
		int[][] tempLuminanceMatrix = new int[width][height];

		double[][] invertedKernel = new double[3][3];

		BufferedImage grayImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage convImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// Extract a grayscale image and luminance from a grayscale or colored image
		// and also extract a luminance matrix
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = img.getRGB(x, y);

				int alpha = (pixel >> 24) & 0xff;
				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = pixel & 0xff;

				int luminance = (int) (0.299 * red + 0.587 * green + 0.114 * blue) % 255;

				luminanceMatrix[x][y] = luminance;

				pixel = (alpha << 24) | (luminance << 16) | (luminance << 8) | luminance;

				grayImg.setRGB(x, y, pixel);
			}
		}

		// Invert the kernel
		for (int i = 0; i < kernel.length; i++) {
			for (int j = 0; j < kernel.length; j++) {
				invertedKernel[i][j] = kernel[kernel.length - 1 - i][kernel.length - 1 - j];

			}
		}

		// Convolute image luminance with inverted kernel without handling borders
		for (int i = 1; i < width - 1; i++) {
			for (int j = 1; j < height - 1; j++) {
				tempLuminanceMatrix[i][j] = convolute(luminanceMatrix, invertedKernel, i, j);
			}
		}

		// Update convImg with the convoluted pixels
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = grayImg.getRGB(x, y);

				int alpha = (pixel >> 24) & 0xff;

				pixel = (alpha << 24) | (tempLuminanceMatrix[x][y] << 16) | (tempLuminanceMatrix[x][y] << 8)
						| tempLuminanceMatrix[x][y];

				convImg.setRGB(x, y, pixel);
			}
		}

		// return grayImg;
		return convImg;
	}

	private static int convolute(int[][] imgPixelsSource, double[][] invertedKernel, int i, int j) {
		int result = 0;

		for (int i1 = 0; i1 < invertedKernel.length; i1++) {
			for (int j1 = 0; j1 < invertedKernel.length; j1++) {
				result += invertedKernel[i1][j1] * imgPixelsSource[i + i1 - 1][j + j1 - 1];
			}
		}

		if (result < 0) {
			return 0;
		} else if (255 < result) {
			return 255;
		} else {
			return result;
		}

//		return (int) 
//				((invertedKernel[0][0] * imgPixelsSource[i - 1][j - 1])
//				+ (invertedKernel[0][1] * imgPixelsSource[i - 1][j])
//				+ (invertedKernel[0][2] * imgPixelsSource[i - 1][j + 1])
//				+ (invertedKernel[1][0] * imgPixelsSource[i][j - 1]) 
//				+ (invertedKernel[1][1] * imgPixelsSource[i][j])
//				+ (invertedKernel[1][2] * imgPixelsSource[i][j + 1])
//				+ (invertedKernel[2][0] * imgPixelsSource[i + 1][j - 1])
//				+ (invertedKernel[2][1] * imgPixelsSource[i + 1][j])
//				+ (invertedKernel[2][2] * imgPixelsSource[i + 1][j + 1]));
	}

	static BufferedImage imgHistogram(BufferedImage img) {

		int width = img.getWidth();
		int height = img.getHeight();

		int[] luminanceHistogram = new int[256];

		BufferedImage histogramChart = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

		// Extract luminance from each pixel and add it to the histogram
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = img.getRGB(x, y);

				int red = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue = pixel & 0xff;

				int luminance = (int) (0.299 * red + 0.587 * green + 0.114 * blue) % 255;

				if (luminance < 0) {
					luminance = 0;
				} else if (255 < luminance) {
					luminance = 255;
				}

				luminanceHistogram[luminance]++;

			}
		}

		int max = Integer.MIN_VALUE;

		for (int i = 0; i < luminanceHistogram.length; i++) {
			if (luminanceHistogram[i] > max) {
				max = luminanceHistogram[i];
			}
		}

		for (int i = 0; i < luminanceHistogram.length; i++) {
			luminanceHistogram[i] /= (max / 255);
		}

		for (int j = 0; j < 256; j++) {
			for (int i = 0; i < luminanceHistogram[j] && i < 256; i++) {
				histogramChart.setRGB(j, 255 - i, 0xff000000);
			}
		}

		return histogramChart;
	}

	static BufferedImage imgZoomIn(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();

		int zoomedWidth = 2 * width;
		int zoomedHeight = 2 * height;

		BufferedImage zoomedInImg = new BufferedImage(zoomedWidth, zoomedHeight, BufferedImage.TYPE_INT_ARGB);

		int[][] imgPixels = new int[width][height];
				
		imgPixels = extractPixels(img);

		
		
		// filling with original pixels, spaced
		for (int x = 0; x < zoomedWidth; x += 2) {
			for (int y = 0; y < zoomedHeight; y += 2) {
				zoomedInImg.setRGB(x, y, imgPixels[x/2][y/2]);
			}
		}

		// interpolating vertically
		for (int x = 1; x < zoomedWidth - 1; x += 2) {
			for (int y = 0; y < zoomedHeight - 1; y += 2) {
				int rgb = horizontalPixelHandling(zoomedInImg, x, y);
				zoomedInImg.setRGB(x, y, rgb);
			}
		}

		// interpolating horizontally
		for (int x = 0; x < zoomedWidth - 1; x += 2) {
			for (int y = 1; y < zoomedHeight - 1; y += 2) {
				
				int rgb = verticalPixelHandling(zoomedInImg, x, y);
				
				zoomedInImg.setRGB(x, y, rgb);
			}
		}

		// interpolating center squares
		for (int x = 1; x < zoomedWidth - 1; x += 2) {
			for (int y = 1; y < zoomedHeight - 1; y += 2) {
				int rgb = neighborhoodPixelHandling(zoomedInImg, x, y);				
				zoomedInImg.setRGB(x, y, rgb);
			}
		}

		return zoomedInImg;

	}

	private static int neighborhoodPixelHandling(BufferedImage zoomedInImg, int x, int y) {
		int pixelLeft = zoomedInImg.getRGB(x-1, y);
		int pixelRight = zoomedInImg.getRGB(x+1, y);
		int pixelAbove = zoomedInImg.getRGB(x, y-1);
		int pixelBelow = zoomedInImg.getRGB(x, y+1);
		
		int alphaLeft = (pixelLeft >> 24) & 0xff;
		int redLeft = (pixelLeft >> 16) & 0xff;
		int greenLeft = (pixelLeft >> 8) & 0xff;
		int blueLeft = pixelLeft & 0xff;
		
		int alphaRight = (pixelRight >> 24) & 0xff;
		int redRight = (pixelRight >> 16) & 0xff;
		int greenRight = (pixelRight >> 8) & 0xff;
		int blueRight = pixelRight & 0xff;
		

		
		int alphaAbove = (pixelAbove >> 24) & 0xff;
		int redAbove = (pixelAbove >> 16) & 0xff;
		int greenAbove = (pixelAbove >> 8) & 0xff;
		int blueAbove = pixelAbove & 0xff;
		
		int alphaBelow = (pixelBelow >> 24) & 0xff;
		int redBelow = (pixelBelow >> 16) & 0xff;
		int greenBelow = (pixelBelow >> 8) & 0xff;
		int blueBelow = pixelBelow & 0xff;
		
		int alpha = (alphaAbove + alphaBelow + alphaLeft + alphaRight) / 4;
		int red = (redAbove + redBelow + redLeft + redRight) / 4;
		int green = (greenAbove + greenBelow + greenLeft + greenRight) / 4;
		int blue = (blueAbove + blueBelow + blueLeft + blueRight) / 4;
		
		int rgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
		return rgb;
	}

	private static int horizontalPixelHandling(BufferedImage zoomedInImg, int x, int y) {
		int pixelLeft = zoomedInImg.getRGB(x-1, y);
		int pixelRight = zoomedInImg.getRGB(x+1, y);
		
		int alphaLeft = (pixelLeft >> 24) & 0xff;
		int redLeft = (pixelLeft >> 16) & 0xff;
		int greenLeft = (pixelLeft >> 8) & 0xff;
		int blueLeft = pixelLeft & 0xff;
		
		int alphaRight = (pixelRight >> 24) & 0xff;
		int redRight = (pixelRight >> 16) & 0xff;
		int greenRight = (pixelRight >> 8) & 0xff;
		int blueRight = pixelRight & 0xff;
		
		int alpha = (alphaLeft + alphaRight) / 2;
		int red = (redLeft + redRight) / 2;
		int green = (greenLeft + greenRight) / 2;
		int blue = (blueLeft + blueRight) / 2;
		
		int rgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
		return rgb;
	}

	private static int verticalPixelHandling(BufferedImage zoomedInImg, int x, int y) {
		int pixelAbove = zoomedInImg.getRGB(x, y-1);
		int pixelBelow = zoomedInImg.getRGB(x, y+1);
		
		int alphaAbove = (pixelAbove >> 24) & 0xff;
		int redAbove = (pixelAbove >> 16) & 0xff;
		int greenAbove = (pixelAbove >> 8) & 0xff;
		int blueAbove = pixelAbove & 0xff;
		
		int alphaBelow = (pixelBelow >> 24) & 0xff;
		int redBelow = (pixelBelow >> 16) & 0xff;
		int greenBelow = (pixelBelow >> 8) & 0xff;
		int blueBelow = pixelBelow & 0xff;
		
		int alpha = (alphaAbove + alphaBelow) / 2;
		int red = (redAbove + redBelow) / 2;
		int green = (greenAbove + greenBelow) / 2;
		int blue = (blueAbove + blueBelow) / 2;
		
		int rgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
		return rgb;
	}

	private static int[][] extractPixels(BufferedImage img) {

		int width = img.getWidth();
		int height = img.getHeight();

		int[][] pixelMatrix = new int[width][height];

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				pixelMatrix[x][y] = img.getRGB(x, y);
			}
		}
		return pixelMatrix;
	}
	
	public static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat object: " + e);
			return null;
		}
	}

	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}

	private static BufferedImage matToBufferedImage(Mat original)
	{
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
}
