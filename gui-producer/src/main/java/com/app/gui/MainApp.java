package com.app.gui;

import com.app.gui.hotel.HotelsTab;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        String bootstrap = "localhost:9092";

        TabPane tabs = new TabPane();

        Tab hotels = new Tab("Hotels");
        hotels.setClosable(false);
        hotels.setContent(new HotelsTab(bootstrap).build());

        tabs.getTabs().addAll(hotels);

        stage.setTitle("Hotel + Tour (CRUD)");
        stage.setScene(new Scene(tabs, 1200, 650));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
