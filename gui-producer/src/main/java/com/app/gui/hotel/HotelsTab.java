package com.app.gui.hotel;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.UUID;

public final class HotelsTab {

    private final ObservableList<HotelRow> rows = FXCollections.observableArrayList();
    private final TableView<HotelRow> table = new TableView<>(rows);

    private final TextField nameField = new TextField();
    private final TextField cityField = new TextField();
    private final TextField priceField = new TextField();

    private final Label selectedIdLabel = new Label("Selected: (none)");
    private final Label statusLabel = new Label();

    private final String bootstrapServers;

    // Delay long enough for local consumer -> sqlite write
    private static final int REFRESH_DELAY_MS = 600;
    
    // Auto-refresh interval for live updates
    private static final int AUTO_REFRESH_INTERVAL_MS = 2000;
    
    private Thread autoRefreshThread;
    private volatile boolean running = true;

    public HotelsTab(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public Node build() {
        // --- Table ---
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<HotelRow, String> idCol = new TableColumn<>("hotel_id");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().hotelId()));

        TableColumn<HotelRow, String> nameCol = new TableColumn<>("name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));

        TableColumn<HotelRow, String> cityCol = new TableColumn<>("city");
        cityCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().city()));

        TableColumn<HotelRow, Number> priceCol = new TableColumn<>("price_per_night");
        priceCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().pricePerNight()));

        table.getColumns().addAll(idCol, nameCol, cityCol, priceCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) {
                selectedIdLabel.setText("Selected: (none)");
                return;
            }
            selectedIdLabel.setText("Selected: " + newV.hotelId());
            nameField.setText(newV.name());
            cityField.setText(newV.city());
            priceField.setText(Double.toString(newV.pricePerNight()));
        });

        // --- Form ---
        nameField.setPromptText("Hotel name");
        cityField.setPromptText("City");
        priceField.setPromptText("Price per night (e.g. 2500)");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Name:"), nameField);
        form.addRow(1, new Label("City:"), cityField);
        form.addRow(2, new Label("Price:"), priceField);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(70);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c1, c2);

        // --- Buttons ---
        Button refreshBtn = new Button("Refresh");
        Button addBtn = new Button("Add");
        Button updateBtn = new Button("Update");
        Button deleteBtn = new Button("Delete");

        refreshBtn.setOnAction(e -> refreshNow());

        addBtn.setOnAction(e -> onAdd());
        updateBtn.setOnAction(e -> onUpdate());
        deleteBtn.setOnAction(e -> onDelete());

        HBox buttons = new HBox(10, refreshBtn, addBtn, updateBtn, deleteBtn);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        VBox right = new VBox(10,
                new Label("Hotel CRUD"),
                selectedIdLabel,
                form,
                buttons,
                statusLabel
        );
        right.setPadding(new Insets(10));
        right.setMinWidth(360);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setCenter(table);
        root.setRight(right);

        // initial load
        refreshNow();
        
        // Start auto-refresh thread
        startAutoRefresh();

        return root;
    }

    // -------------------- Actions --------------------

    private void onAdd() {
        try {
            System.out.println("[DEBUG] onAdd() called");
            String name = req(nameField.getText(), "Name");
            String city = req(cityField.getText(), "City");
            double price = parsePrice(priceField.getText());
            UUID id = UUID.randomUUID();

            System.out.println("[DEBUG] Creating hotel: " + name + ", " + city + ", " + price);
            statusLabel.setText("Sending hotel.created...");

            try (HotelKafkaProducer kp = new HotelKafkaProducer(bootstrapServers)) {
                kp.createHotel(id, name, city, price);
            }

            System.out.println("[DEBUG] Hotel created successfully: " + id);
            statusLabel.setText("Sent hotel.created: " + id);

            clearForm();
            refreshLater();
        } catch (Exception ex) {
            System.err.println("[ERROR] Add failed: " + ex.getMessage());
            ex.printStackTrace();
            showError("Add failed", ex);
        }
    }

    private void onUpdate() {
        try {
            System.out.println("[DEBUG] onUpdate() called");
            HotelRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select a hotel row first, then edit fields, then Update.");
                System.out.println("[DEBUG] No hotel selected for update");
                return;
            }

            UUID id = UUID.fromString(selected.hotelId());
            String name = req(nameField.getText(), "Name");
            String city = req(cityField.getText(), "City");
            double price = parsePrice(priceField.getText());

            System.out.println("[DEBUG] Updating hotel: " + id + " -> " + name + ", " + city + ", " + price);
            statusLabel.setText("Sending hotel.updated for: " + id);

            try (HotelKafkaProducer kp = new HotelKafkaProducer(bootstrapServers)) {
                kp.updateHotel(id, name, city, price);
            }

            System.out.println("[DEBUG] Hotel updated successfully: " + id);
            statusLabel.setText("Sent hotel.updated: " + id);

            // Keep selection visible but refresh data
            refreshLater();
        } catch (Exception ex) {
            System.err.println("[ERROR] Update failed: " + ex.getMessage());
            ex.printStackTrace();
            showError("Update failed", ex);
        }
    }

    private void onDelete() {
        try {
            System.out.println("[DEBUG] onDelete() called");
            HotelRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Select a hotel row first, then Delete.");
                System.out.println("[DEBUG] No hotel selected for delete");
                return;
            }

            boolean ok = confirm("Delete hotel?", "Delete: " + selected.name() + "\nID: " + selected.hotelId());
            if (!ok) {
                System.out.println("[DEBUG] Delete cancelled by user");
                return;
            }

            UUID id = UUID.fromString(selected.hotelId());
            System.out.println("[DEBUG] Deleting hotel: " + id);
            statusLabel.setText("Sending hotel.deleted for: " + id);

            try (HotelKafkaProducer kp = new HotelKafkaProducer(bootstrapServers)) {
                kp.deleteHotel(id);
            }

            System.out.println("[DEBUG] Hotel deleted successfully: " + id);
            statusLabel.setText("Sent hotel.deleted: " + id);

            clearForm();
            refreshLater();
        } catch (Exception ex) {
            System.err.println("[ERROR] Delete failed: " + ex.getMessage());
            ex.printStackTrace();
            showError("Delete failed", ex);
        }
    }

    // -------------------- Refresh helpers --------------------

    private void refreshNow() {
        try {
            System.out.println("[DEBUG] Refreshing hotel list...");
            rows.setAll(HotelDbReader.loadHotels());
            System.out.println("[DEBUG] Loaded " + rows.size() + " hotel(s)");
            statusLabel.setText("Loaded " + rows.size() + " hotel(s).");
        } catch (Exception ex) {
            System.err.println("[ERROR] DB read error: " + ex.getMessage());
            ex.printStackTrace();
            statusLabel.setText("DB read error: " + ex.getMessage());
        }
    }

    private void refreshLater() {
        // Kafka -> consumer -> sqlite is asynchronous; wait a bit then refresh
        new Thread(() -> {
            try {
                Thread.sleep(REFRESH_DELAY_MS);
            } catch (InterruptedException ignored) {}
            Platform.runLater(this::refreshNow);
        }, "refresh-delay-thread").start();
    }
    
    private void startAutoRefresh() {
        autoRefreshThread = new Thread(() -> {
            System.out.println("[DEBUG] Auto-refresh thread started");
            while (running) {
                try {
                    Thread.sleep(AUTO_REFRESH_INTERVAL_MS);
                    Platform.runLater(this::refreshNow);
                } catch (InterruptedException e) {
                    break;
                }
            }
            System.out.println("[DEBUG] Auto-refresh thread stopped");
        }, "auto-refresh-thread");
        autoRefreshThread.setDaemon(true);
        autoRefreshThread.start();
    }
    
    public void stop() {
        running = false;
        if (autoRefreshThread != null) {
            autoRefreshThread.interrupt();
        }
    }

    private void clearForm() {
        nameField.clear();
        cityField.clear();
        priceField.clear();
        table.getSelectionModel().clearSelection();
        selectedIdLabel.setText("Selected: (none)");
    }

    // -------------------- Validation + dialogs --------------------

    private static String req(String s, String fieldName) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(fieldName + " is required.");
        return s.trim();
    }

    private static double parsePrice(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Price must be a number (e.g. 2500 or 2500.50).");
        }
    }

    private static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void showError(String header, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }
}
