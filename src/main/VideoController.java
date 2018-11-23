package main;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class VideoController {
	@FXML
	private Button btnCamera;
	@FXML
	private Button btnReset;
	
	
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
	private CheckBox resizing;

	@FXML
	private TextField camStatusTextField;

	@FXML
	private Slider rotation;
	@FXML
	private Slider brightness;
	@FXML
	private Slider contrast;
	@FXML
	private Slider cannyThreshold;
	@FXML
	private Slider gaussIntensity;
	@FXML
	private Slider heightResize;
	@FXML
	private Slider widthResize;

	@FXML
	private ImageView currentFrame;

	// OpenCV: video capture
	private VideoCapture capture = new VideoCapture();
	// Timer for video stream
	private ScheduledExecutorService timer;
	private boolean isCameraActive = false;
	private static int cameraId = 0;

	String cameraErrorMessage = "Camera status: Not connected or unreachable. Please try again with a camera connected or set up.";
	String cameraOkMessage = "Camera status: Connected.";

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
				System.out.println(cameraOkMessage);
				camStatusTextField.setText(cameraOkMessage);

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

				// update the camera content
				this.btnCamera.setText("Stop Camera");
			} else {
				// log the error
				System.err.println(cameraErrorMessage);
				camStatusTextField.setText(cameraErrorMessage);
			}
		} else {
			// the camera is not active at this point
			this.isCameraActive = false;
			// update again the camera content
			this.btnCamera.setText("Start Camera");

			// stop the timer
			this.stopAcquisition();
		}
	}

	@FXML
	protected void resetControls(ActionEvent event) {
		
		// uncheck all checkboxes
		blurring.setSelected(false);
		canny.setSelected(false);
		sobel.setSelected(false);
		grayscale.setSelected(false);
		flipHor.setSelected(false);
		flipVert.setSelected(false);
		negative.setSelected(false);
		resizing.setSelected(false);
		
		// reset all sliders to default value
		rotation.setValue(0);
		brightness.setValue(0);
		contrast.setValue(1);
		cannyThreshold.setValue(0);
		gaussIntensity.setValue(0);
		heightResize.setValue(1);
		widthResize.setValue(1);
		rotation.setValue(0);
		
	}

	private Mat grabFrame() {

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
						gaussIntensity.setDisable(false);
						double maxBlur = gaussIntensity.getValue();
						for (int i = 0; i < maxBlur; i++) {
							Imgproc.GaussianBlur(frame, frame, new Size(3, 3), 3);
						}

					} else {
						if (gaussIntensity.isDisable() == false) {
							gaussIntensity.setDisable(true);
						}
					}

					// apply grayscale filter - WORKING
					if (grayscale.isSelected()) {
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					}
					
					// apply canny convolution
					if (canny.isSelected()) {

						cannyThreshold.setDisable(false);

						// convert to grayscale
						if (!grayscale.isSelected()) {
							Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
						}

						// reduce noise with a 3x3 kernel
						Imgproc.blur(frame, frame, new Size(3, 3));

						// canny detector, with ratio of lower:upper threshold of 3:1
						Imgproc.Canny(frame, frame, cannyThreshold.getValue(), cannyThreshold.getValue() * 3);

					} else if (cannyThreshold.isDisable() == false) {
						cannyThreshold.setDisable(true);
					}

					// apply sobel convolution
					if (sobel.isSelected()) {
						
						int ddepth = CvType.CV_16S;
					    Mat grayImage = new Mat();
					    Mat gradientX = new Mat();
					    Mat gradientY = new Mat();
					    Mat absGradientX = new Mat();
					    Mat absGradientY = new Mat();

					    // reduce noise with a 3x3 kernel
					    Imgproc.GaussianBlur(frame, frame, new Size(3, 3), 0, 0, Core.BORDER_DEFAULT);

					    // convert to grayscale if not already converted
						if (!grayscale.isSelected() && !canny.isSelected()) {
							Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);
							
							// Gradient X
						    Imgproc.Sobel(grayImage, gradientX, ddepth, 1, 0);
						    Core.convertScaleAbs(gradientX, absGradientX);

						    // Gradient Y
						    Imgproc.Sobel(grayImage, gradientY, ddepth, 0, 1);
						    Core.convertScaleAbs(gradientY, absGradientY);
						} else {
							// Gradient X
						    Imgproc.Sobel(frame, gradientX, ddepth, 1, 0);
						    Core.convertScaleAbs(gradientX, absGradientX);

						    // Gradient Y
						    Imgproc.Sobel(frame, gradientY, ddepth, 0, 1);
						    Core.convertScaleAbs(gradientY, absGradientY);
						}//
						
					    // Total Gradient (approximate)
						Core.addWeighted(absGradientX, 0.5, absGradientY, 0.5, 0, frame);
					}

					// apply horizontal mirroring - WORKING
					if (flipHor.isSelected()) {
						Core.flip(frame, frame, 1);
					}

					// apply vertical mirroring - WORKING
					if (flipVert.isSelected()) {
						Core.flip(frame, frame, 0);
					}

					// apply resizing
					if (resizing.isSelected()) {
						// allow use of resizing sliders
						heightResize.setDisable(false);
						widthResize.setDisable(false);
						
						int frameWidth = frame.cols();
						int frameHeight = frame.rows();						
						
						int newFrameWidth = (int) (widthResize.getValue() * frameWidth);
						int newFrameHeight = (int) (heightResize.getValue() * frameHeight);						
						
						// resize image according to slider values
						Imgproc.resize(frame, frame, new Size(newFrameWidth, newFrameHeight), 0, 0, Imgproc.INTER_CUBIC);
					} else if (!heightResize.isDisable() && !widthResize.isDisable() ) {
						heightResize.setDisable(true);
						widthResize.setDisable(true);
					}

					// apply negative - WORKING
					if (negative.isSelected()) {
						Core.bitwise_not(frame, frame);
					}

					// apply rotation - WORKING
					rotateFrame(frame);

					// apply brightness and contrast
					bias = brightness.getValue();
					gain = contrast.getValue();
					if (bias != 0 || gain != 1) {
						frame.convertTo(frame, -1, gain, bias);

//						Mat newFrame = Mat.zeros(frame.size(), frame.type());
//						
//						byte[] frameData = new byte[(int) (frame.total() * frame.channels())];
//						frame.get(0, 0, frameData);
//						byte[] newFrameData = new byte[(int) (newFrame.total() * newFrame.channels())];
//						for (int y = 0; y < frame.rows(); y++) {
//							for (int x = 0; x < frame.cols(); x++) {
//								for (int c = 0; c < frame.channels(); c++) {
//									double pixelValue = frameData[(y * frame.cols() + x) * frame.channels() + c];
//									pixelValue = pixelValue < 0 ? pixelValue + 256 : pixelValue;
//									newFrameData[(y * frame.cols() + x) * frame.channels() + c] = saturate(
//											gain * pixelValue + bias);
//								}
//							}
//						}
//						newFrame.put(0, 0, newFrameData);
//						frame.put(0, 0, newFrameData);
					}
				}
			}

			catch (Exception e) {
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

	// Stop the acquisition from the camera when main is closed
	protected void setClosed() {
		this.stopAcquisition();
	}
}
