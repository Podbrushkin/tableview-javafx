package podbrushkin.javafxcharts;

import java.util.ArrayList;
import java.util.List;
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
    private ArrayList<ComboBox<TableColumn<JsonObject,?>>> columnComboBoxes = new ArrayList<>();

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
        for (Map.Entry<String,Class> info : pieProducer.getExpectedColumnsInfo()) {
            var combo = new ComboBox<TableColumn<JsonObject,?>>();
            combo.getItems().addAll(parentAndChildColumns);
            combo.setConverter(comboboxDisplayname);
            leftVbox.getChildren().add(new Label(info+":"));
            leftVbox.getChildren().add(combo);
            columnComboBoxes.add(combo);
        }
        var submitButton = new Button("Submit");
        leftVbox.getChildren().add(submitButton);

        submitButton.setOnAction(me -> {
            var expectedTypes = pieProducer.getExpectedColumnsInfo().stream().map(entry -> entry.getValue()).toList();
            var pie = new PieChartProducer().createContent(createArrayFromSelectedColumns(expectedTypes));
            borderPane.setCenter(pie);
        });


        borderPane.setLeft(leftVbox);

        
        final Stage stage = new Stage();
        Scene scene = new Scene(borderPane, 200, 200, Color.WHITESMOKE);
        stage.setScene(scene);
        stage.setTitle("New stage");
        stage.centerOnScreen();
        stage.show();
    }
    private Object[][] createArrayFromSelectedColumns(List<Class> targetColumnTypes) {
        var dataList = new ArrayList<List>();
        //for each row
        for (int i = 0; i < tableView.getItems().size(); i++) {
            var singleRow = new ArrayList<>();
            // for each column
            for (int j = 0; j < columnComboBoxes.size(); j++) {
                var selectedColumn = columnComboBoxes.get(j).getValue();
                Class targetType = targetColumnTypes.get(j);
                
                Object cellRawValue = selectedColumn.getCellObservableValue(i).getValue();
                if (cellRawValue == null) {break;}
                else if (targetType.equals(String.class)) {
                    singleRow.add(cellRawValue.toString());
                }
                else if (targetType.equals(Double.class)) {
                    singleRow.add(Double.valueOf(cellRawValue.toString()));
                }
            }
            // each row should have legit values for all columns
            if (singleRow.size() == columnComboBoxes.size() && !singleRow.contains(null)) {
                dataList.add(singleRow);
            }
        }
        // 2d list to 2d array
        return dataList.stream().map(List::toArray).toArray(Object[][]::new);
    }
}
