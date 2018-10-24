package main;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class VideoController {
	@FXML
	private Button button;

	@FXML
	private CheckBox blurring;
	@FXML
	private CheckBox canny;
	@FXML
	private CheckBox sobel;
	@FXML
	private CheckBox grayscale;
	@FXML
	private CheckBox flipHor;
	@FXML
	private CheckBox flipVert;
	@FXML
	private CheckBox negative;

	@FXML
	private Slider rotation;
	@FXML
	private Slider brightness;
	@FXML
	private Slider contrast;

	@FXML
	private ImageView currentFrame;

	// OpenCV: video capture
	private VideoCapture capture = new VideoCapture();
	// Timer for video stream
	private ScheduledExecutorService timer;
	private boolean isCameraActive = false;
	private static int cameraId = 0;

	public void initialize() {
		this.capture = new VideoCapture();
		this.isCameraActive = false;
	}

	@FXML
	protected void startCamera(ActionEvent event) {

		this.currentFrame.setFitWidth(600);
		this.currentFrame.setPreserveRatio(true);

		if (!this.isCameraActive) {
			// start capturing video
			this.capture.open(cameraId);

			// test for opening success
			if (this.capture.isOpened()) {
				this.isCameraActive = true;

				// 30 FPS
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// convert and show the frame
						Image imageToShow = ImageHandler.mat2Image(frame);
						updateImageView(currentFrame, imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.button.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Camera connection unavailable.");
			}
		} else {
			// the camera is not active at this point
			this.isCameraActive = false;
			// update again the button content
			this.button.setText("Start Camera");

			// stop the timer
			this.stopAcquisition();
		}
	}

	private byte saturate(double val) {
		int iVal = (int) Math.round(val);
		iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
		return (byte) iVal;
	}

	private Mat grabFrame() {
		// init everything
		Mat frame = new Mat();
		double bias = 0;
		double gain = 1;

		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty()) {
					// apply gaussian convolution
					if (blurring.isSelected()) {
						Imgproc.GaussianBlur(frame, frame, new Size(3, 3), 3);
					}

					// apply canny convolution
					if (canny.isSelected()) {
						Imgproc.Canny(frame, frame, 3, 3);
					}

					// apply sobel convolution
					if (sobel.isSelected()) {
						Imgproc.Sobel(frame, frame, 0, 0, 0);
					}

					// apply grayscale filter
					if (grayscale.isSelected()) {
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					}

					// apply horizontal mirroring
					if (flipHor.isSelected()) {
						Core.flip(frame, frame, 1);
					}

					// apply vertical mirroring
					if (flipVert.isSelected()) {
						Core.flip(frame, frame, 0);
					}

					// apply negative
					if (negative.isSelected()) {
						Core.bitwise_not(frame, frame);
					}

					// apply rotation
					rotateFrame(frame);

					// apply brightness and contrast
					bias = brightness.getValue();
					gain = contrast.getValue();
					if (bias != 0 || gain != 1) {
						Mat newFrame = Mat.zeros(frame.size(), frame.type());

						byte[] frameData = new byte[(int) (frame.total() * frame.channels())];
						frame.get(0, 0, frameData);
						byte[] newFrameData = new byte[(int) (newFrame.total() * newFrame.channels())];
						for (int y = 0; y < frame.rows(); y++) {
							for (int x = 0; x < frame.cols(); x++) {
								for (int c = 0; c < frame.channels(); c++) {
									double pixelValue = frameData[(y * frame.cols() + x) * frame.channels() + c];
									pixelValue = pixelValue < 0 ? pixelValue + 256 : pixelValue;
									newFrameData[(y * frame.cols() + x) * frame.channels() + c] = saturate(
											gain * pixelValue + bias);
								}
							}
						}
						newFrame.put(0, 0, newFrameData);
						frame.put(0, 0, newFrameData);
					}
				}

			} catch (Exception e) {
				// log the error
				System.err.println("Exception during image elaboration: " + e);
			}
		}

		return frame;
	}

	private void rotateFrame(Mat frame) {
		switch ((int) rotation.getValue()) {
		case 0:
			break;
		case 90:
			Core.rotate(frame, frame, Core.ROTATE_90_CLOCKWISE);
			break;
		case 180:
			Core.rotate(frame, frame, Core.ROTATE_180);
			break;
		case 270:
			Core.rotate(frame, frame, Core.ROTATE_90_COUNTERCLOCKWISE);
			break;
		}
	}

	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
		}
	}

	private void updateImageView(ImageView view, Image image) {
		ImageHandler.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On main close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}
}
