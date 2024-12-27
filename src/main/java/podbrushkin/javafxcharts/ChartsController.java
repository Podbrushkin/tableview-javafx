package podbrushkin.javafxcharts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import podbrushkin.javafxtable.TableViewJson;

public class ChartsController {

    private TableViewJson tableView;
    // ComboBox<TableColumn<JsonObject,?>> columnComboBox1;
    // ComboBox<TableColumn<JsonObject,?>> columnComboBox2;
    ArrayList<ComboBox<TableColumn<JsonObject,?>>> columnComboBoxes = new ArrayList<>();

    public ChartsController(TableViewJson tableView) {
        this.tableView = tableView;
    }
    public void showWindow() {
        var borderPane = new BorderPane();
        
        var parentAndChildColumns = new ArrayList<TableColumn<JsonObject, ?>>();
        for (var tc : tableView.getColumns()) {
            parentAndChildColumns.add(tc);
            for (var tcChild : tc.getColumns()) {
                parentAndChildColumns.add(tcChild);
            }
        }
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
        var leftVbox = new VBox();
        var pieProducer = new PieChartProducer();
        for (String info : pieProducer.getExpectedColumnsInfo()) {
            var combo = new ComboBox<TableColumn<JsonObject,?>>();
            combo.getItems().addAll(parentAndChildColumns);
            combo.setConverter(comboboxDisplayname);
            leftVbox.getChildren().add(new Label(info+":"));
            leftVbox.getChildren().add(combo);
            columnComboBoxes.add(combo);
        }
        var submitButton = new Button("Submit");
        leftVbox.getChildren().add(submitButton);

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
        
        TableColumn<JsonObject,?> selectedColumn1 = columnComboBoxes.get(0).getValue();
        TableColumn<JsonObject,?> selectedColumn2 = columnComboBoxes.get(1).getValue();
        

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
