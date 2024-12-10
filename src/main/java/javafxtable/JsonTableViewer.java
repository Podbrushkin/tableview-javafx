package javafxtable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringJoiner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class JsonTableViewer extends Application {
    static JsonArray data = null;

    // Data structure to hold the table data
    public static class MyObject {
        private final Object[] columns;

        public MyObject(Object[] columns) {
            this.columns = columns;
        }

        // Customize getters for each column you expect, e.g., if you expect three columns:
        public Object getColumn(int index) {
            return (index < columns.length) ? columns[index] : "";
        }

        public String toString() {
            return Arrays.toString(columns);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JSON Table Viewer");

        // Initialize TableView and its columns
        TableView<MyObject> tableView = new TableView<>();
        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // tableView.setC
        
        

        // Process lines from TSV and populate the table
        if (!data.isEmpty()) {
            // String[] headers = data.get(0).split("\t"); // Split the first line to get headers

            var jsonObj = data.get(0).getAsJsonObject();
            // String[] headers = (String[])jsonObj.keySet().toArray();
            String[] headers = jsonObj.keySet().toArray(new String[0]);

            for (int i = 0; i < headers.length; i++) {
                final int columnIndex = i; // Final variable for use in lambda
                
                var value = jsonObjectToValuesArray(jsonObj)[i];

                if (value instanceof String) {
                    TableColumn<MyObject, String> column = new TableColumn<>(headers[i]);
                    column.setCellValueFactory(cellData -> 
                        new SimpleStringProperty(cellData.getValue().getColumn(columnIndex).toString())
                        );
                    
                    /* // this makes word wrap but how do you toggle it?
                    column.setCellFactory(tc -> {
                        var cell = new TableCell<MyObject, String>();
                        Text text = new Text();
                        cell.setGraphic(text);
                        cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
                        text.wrappingWidthProperty().bind(column.widthProperty());
                        text.textProperty().bind(cell.itemProperty().asString());
                        return cell;
                    }); */
                    tableView.getColumns().add(column);
                }
                if (value instanceof Integer) {
                    TableColumn<MyObject, Integer> column = new TableColumn<>(headers[i]);
                    column.setCellValueFactory(cellData -> 
                        // new ReadOnlyIntegerWrapper
                        new SimpleIntegerProperty((Integer)cellData.getValue().getColumn(columnIndex)).asObject()
                        );
                    tableView.getColumns().add(column);
                }
                if (value instanceof Float) {
                    TableColumn<MyObject, Float> column = new TableColumn<>(headers[i]);
                    column.setCellValueFactory(cellData -> 
                        // new ReadOnlyIntegerWrapper
                        new SimpleFloatProperty((Float)cellData.getValue().getColumn(columnIndex)).asObject()
                        );
                    tableView.getColumns().add(column);
                }
                if (value instanceof Boolean) {
                    TableColumn<MyObject, Boolean> column = new TableColumn<>(headers[i]);
                    column.setCellValueFactory(cellData -> 
                        new ReadOnlyBooleanWrapper((Boolean)cellData.getValue().getColumn(columnIndex))
                        );
                    tableView.getColumns().add(column);
                }

                
            }

            for (int i = 0; i < data.size(); i++) {
                Object[] rowValues = jsonObjectToValuesArray(data.get(i).getAsJsonObject());
                MyObject row = new MyObject(rowValues);
                tableView.getItems().add(row);
            }

            

        }

        // Setup layout and scene
        /* VBox vbox = new VBox(tableView);
        Scene scene = new Scene(vbox);
        primaryStage.setScene(scene);
        primaryStage.show(); */
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root,500,300);
        primaryStage.setScene(scene);
        primaryStage.show();
        var zp = new ZoomingPane(tableView);
        tableView.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                Double ov = zp.getZoomFactor();
                boolean zoomOut = e.getDeltaY() < 0;
                float delta = 0.08f;
                if (zoomOut) {
                    zp.zoomFactorProperty().setValue(ov*(1f-delta));
                } else {
                    zp.zoomFactorProperty().setValue(ov*(1f+delta));
                }
                e.consume();
            }
        });
        // tableView.col
        /* zp.setOnScroll((eh) -> {
            System.out.println(eh);
        });
        tableView.setOnScroll((eh) -> {
            System.out.println("TABLEVIEW SCROLL");
            System.out.println(eh);
        }); */
        // Slider slider = new Slider(0.5,2,1);
        // zp.zoomFactorProperty().bind(slider.valueProperty());
        // root.setBottom(slider);
        // root.setCenter(tableView);
        root.setCenter(zp);
        root.setTop(getSearchField(tableView));
        // System.out.println(tableView.getParent());
        root.setBottom(buildCommonControl(tableView));
    }

    private Node getSearchField(TableView<MyObject> tableView) {
        var filteredList = new FilteredList<>(tableView.getItems());
        SortedList<MyObject> sortableData = new SortedList<>(filteredList);
        tableView.setItems(sortableData);
        sortableData.comparatorProperty().bind(tableView.comparatorProperty());
        // tableView.setItems(filteredList);
        var tf = new TextField();
        tf.setPromptText(String.format("Search across %s rows...",sortableData.size()));
        tf.textProperty().addListener((obsVal,oldVal,newVal) -> {
            filteredList.setPredicate((obj) -> {
                return obj.toString().contains(newVal);
            });
        });
        return tf;
    }

    private Object[] jsonObjectToValuesArray(JsonObject obj) {
        // var entrySet = ;
        var list = new ArrayList<Object>();
        for (var e : obj.entrySet()) {
            Object value = null;
            var prim = e.getValue().getAsJsonPrimitive();
            if (prim.isString()) { 
                value = prim.getAsString();
            } 
            else if (prim.isNumber()) {
                String num = prim.getAsString();
                boolean isFloat = num.matches("[-+]?[0-9]*\\.[0-9]+");
                if (isFloat) {
                    value = prim.getAsFloat();
                } else {
                    value = prim.getAsInt();
                }
            }
            else if (prim.isBoolean()) {
                value = prim.getAsBoolean();
            }
            list.add(value);
        }
        return list.toArray();
    }

    private Node buildCommonControl(TableView<MyObject> tableView) {
        final GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));

        int row = 0;

        CheckBox constrainedColumnPolicy = new CheckBox("Constrained Column Policy");
        constrainedColumnPolicy.setSelected(tableView.getColumnResizePolicy().equals(TableView.CONSTRAINED_RESIZE_POLICY));
        // Bindings.when(constrainedColumnPolicy.selectedProperty()).then(TableView.CONSTRAINED_RESIZE_POLICY).otherwise(TableView.UNCONSTRAINED_RESIZE_POLICY).bind(tableView.columnResizePolicyProperty());
        tableView.columnResizePolicyProperty().bind(Bindings.when(constrainedColumnPolicy.selectedProperty()).then(TableView.CONSTRAINED_RESIZE_POLICY).otherwise(TableView.UNCONSTRAINED_RESIZE_POLICY));
        // constrainedColumnPolicy.selectedProperty().bind(null);
        grid.add(constrainedColumnPolicy, 0, row++);

        var parent = tableView.getParent();
        if (parent != null && parent instanceof ZoomingPane) {
            ZoomingPane zoomingPane = (ZoomingPane)parent;
            Slider slider = new Slider(0.5,2,1);
            zoomingPane.zoomFactorProperty().bindBidirectional(slider.valueProperty());
            grid.add(slider, 0, row++);
        }

        CheckBox fixedCellSize = new CheckBox("Set Fixed Cell Size");
        fixedCellSize.setDisable(true);
        fixedCellSize.selectedProperty().bind(Bindings.greaterThan(tableView.fixedCellSizeProperty(),0));
        // tableView.fixedCellSizeProperty().bind(Bindings.when(fixedCellSize.selectedProperty()).then(40).otherwise(0));
        // System.out.println(tableView.fixedCellSizeProperty()); 
        // tableView.
        Slider sliderCellSize = new Slider(0,100,40);
        // sliderCellSize.disableProperty().bind(fixedCellSize.selectedProperty().not());

        tableView.fixedCellSizeProperty().bindBidirectional(sliderCellSize.valueProperty());
        grid.add(fixedCellSize, 0, row++);
        grid.add(sliderCellSize, 0, row++);

        
        /* CheckBox showTableMenuButton = new CheckBox("Show Table Menu Button");
        showTableMenuButton.selectedProperty().bind(tableView.tableMenuButtonVisibleProperty());
        grid.add(showTableMenuButton, 0, row++); */

        
        /* Slider sliderZoom = new Slider(1, 2, 1);
        tableView.scaleZProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleXProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleYProperty().bindBidirectional(sliderZoom.valueProperty());
        grid.add(sliderZoom, 0, row++); */
        /* Slider sliderX = new Slider(1, 2, 1);
        tableView.scaleZProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleXProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleYProperty().bindBidirectional(sliderZoom.valueProperty()); */
        // grid.add(sliderZoom, 0, row++);
        /* Slider sliderY = new Slider(0, 100, 0);
        tableView.translateYProperty().bindBidirectional(sliderY.valueProperty());
        grid.add(sliderY, 0, row++); */


        // tableView.onZoomStartedProperty();
        /* tableView.setOnZoomStarted((eh) ->{
            System.out.println(eh);
        }); 
        tableView.setOnScroll((eh) -> {
            System.out.println(eh);
        });*/

        var tp = new TitledPane("TableView Options", grid);
        tp.setExpanded(false);
        return tp;
    }

    // Sample data source (replace this with actual TSV data)
    private static String getSampleData() {
        return """
        [{"lang_id":1,"lang_name":"Afrikaans","lang_code":"afr","latin_script":"\\\\N"},{"lang_id":2,"lang_name":"Albanian","lang_code":"alb","latin_script":"\\\\N"},{"lang_id":3,"lang_name":"Ancient Greek","lang_code":"grc","latin_script":"No"},{"lang_id":4,"lang_name":"Arabic","lang_code":"ara","latin_script":"No"},{"lang_id":5,"lang_name":"Armenian","lang_code":"arm","latin_script":"No"},{"lang_id":6,"lang_name":"Azerbaijani","lang_code":"aze","latin_script":"\\\\N"},{"lang_id":7,"lang_name":"Basque","lang_code":"baq","latin_script":"\\\\N"},{"lang_id":8,"lang_name":"Belarusian","lang_code":"bel","latin_script":"No"},{"lang_id":9,"lang_name":"Bengali","lang_code":"ben","latin_script":"No"},{"lang_id":10,"lang_name":"Bulgarian","lang_code":"bul","latin_script":"No"}]
        """;
    }
    private static String readFromStdin(Scanner sc) {
        
        // var lines = new ArrayList<String>();
        // var sb = new StringBuilder();
        var sj = new StringJoiner(System.lineSeparator());
        while (sc.hasNextLine()) {
            // lines.add(sc.nextLine());
            sj.add(sc.nextLine());
        }
        
        return sj.toString();
        
    }

    public static void main(String[] args) {
        String dataJson = null;
        var sc = new Scanner(System.in, "UTF-8");
        dataJson = sc.hasNextLine() ? readFromStdin(sc) : getSampleData();
        sc.close();
        data = new Gson().fromJson(dataJson, JsonArray.class);
        launch(args);
    }
}