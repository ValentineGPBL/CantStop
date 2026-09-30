package GUI;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Menu extends Application {

    @Override
    public void start(Stage primaryStage) {
        MenuController controller = new MenuController();


        Scene scene = new Scene(controller.getView());
        scene.getStylesheets().add(
                getClass().getResource("/GUI/styleMenu.css").toExternalForm()
        );

        primaryStage.setTitle("Menu");
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
