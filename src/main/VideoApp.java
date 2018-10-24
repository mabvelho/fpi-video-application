package main;


import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class VideoApp extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			// load FXML, store the root element
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/VideoFX.fxml"));
			// store the root element for controller usage
			BorderPane root = (BorderPane) loader.load();

			// create and style scene
			Scene scene = new Scene(root, 1000, 600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			primaryStage.setTitle("VideoApp");
			primaryStage.setScene(scene);
			// display the stage frame
			primaryStage.show();			

			VideoController controller = loader.getController();
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we)
				{
					controller.setClosed();
				}
			}));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
