package podbrushkin.javafxcharts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import podbrushkin.javafxtable.TableViewJson;

public class ChartsController {

    private TableViewJson tableView;
    ComboBox<TableColumn<JsonObject,?>> columnComboBox1;
    ComboBox<TableColumn<JsonObject,?>> columnComboBox2;

    public ChartsController(TableViewJson tableView) {
        this.tableView = tableView;
    }
    public void showWindow() {
        var borderPane = new BorderPane();
        
        // borderPane.setCenter(new PieChartProducer().createContent());
        
        var leftVbox = new VBox();
        var submitButton = new Button("Submit");
        columnComboBox1 = new ComboBox<TableColumn<JsonObject,?>>();
        columnComboBox2 = new ComboBox<TableColumn<JsonObject,?>>();
        
        var parentAndChildColumns = new ArrayList<TableColumn<JsonObject, ?>>();
        for (var tc : tableView.getColumns()) {
            parentAndChildColumns.add(tc);
            for (var tcChild : tc.getColumns()) {
                parentAndChildColumns.add(tcChild);
            }
        }
        columnComboBox1.getItems().addAll(parentAndChildColumns);
        columnComboBox2.getItems().addAll(parentAndChildColumns);

        var comboboxDisplayname = new StringConverter<TableColumn<JsonObject,?>>() {
            @Override
            public String toString(TableColumn object) {
                return object != null ? object.getText() : "";
            }
            @Override
            public TableColumn fromString(String string) {
                return null;
            }
        };
        columnComboBox1.setConverter(comboboxDisplayname);
        columnComboBox2.setConverter(comboboxDisplayname);
        leftVbox.getChildren().addAll(columnComboBox1,columnComboBox2,submitButton);

        // populateColumnComboBoxes();

        // leftVbox.getChildren().addAll(listView,submitButton);
        submitButton.setOnAction(me -> {
            // var selectedColumnNames = listView.getSelectionModel().getSelectedItems();
            
            ;
            var pie = new PieChartProducer().createContent(createMapFromSelectedColumns());
            borderPane.setCenter(pie);

            // new PieChartProducer().createContent(stringToDouble)
        });


        borderPane.setLeft(leftVbox);

        
        final Stage stage = new Stage();
        Scene scene = new Scene(borderPane, 200, 200, Color.WHITESMOKE);
        stage.setScene(scene);
        stage.setTitle("New stage");
        stage.centerOnScreen();
        stage.show();
    }
    private Map<String,Double> createMapFromSelectedColumns() {
        TableColumn<JsonObject,?> selectedColumn1 = columnComboBox1.getValue();
        TableColumn<JsonObject,?> selectedColumn2 = columnComboBox2.getValue();
        

        if (selectedColumn1 != null && selectedColumn2 != null) {
            Map<String, Double> resultMap = new HashMap<>();
            for (JsonObject row : tableView.getItems()) {
                // JsonElement keyJsonEl = (JsonElement) selectedColumn1.getCellObservableValue(row).getValue();

                // String key = (String) selectedColumn1.getCellObservableValue(row).getValue().toString();
                var keyObj = selectedColumn1.getCellObservableValue(row).getValue();
                var valueObj = selectedColumn2.getCellObservableValue(row).getValue();
                if (keyObj == null || valueObj == null) {continue;}
                String key = keyObj.toString();
                Double value = Double.valueOf(valueObj.toString());
                // Double value = (Double) selectedColumn2.getCellObservableValue(row).getValue();
                resultMap.put(key, value);
            }
            System.out.println("Created Map: " + resultMap); // Display or use the map as needed
            return resultMap;
            
        } else {
            System.err.println("Please select both columns!");
        }
        return null;
    }

    /* private getPieChartForSelectedColumns() {
        columnComboBox1 = new ComboBox<>();
        columnComboBox2 = new ComboBox<>();
        populateColumnComboBoxes();
    } */
    
}
