package podbrushkin.javafxcharts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import podbrushkin.javafxcharts.utils.SpinnerDraggable;
import podbrushkin.javafxtable.TableViewJson;

public class ChartsController {

    private TableViewJson tableView;
    private ArrayList<ComboBox<TableColumn<JsonObject,?>>> columnComboBoxes = new ArrayList<>();
    private List<ChartProducer> chartProducers = List.of(
        new PieChartProducer(), 
        new LineChartProducer(),
        new ScatterChartProducer(),
        new BubbleChartProducer(),
        new BarChartProducer()
    );

    // put props here to prevent garbage collection
    private static List<Property> strongReferences = new ArrayList<>();

    public ChartsController(TableViewJson tableView) {
        this.tableView = tableView;
    }
    public void showWindow() {
        var borderPane = new BorderPane();
        
        var leftVboxRoot = new VBox();
        var leftVboxInit = new VBox();
        var leftVboxDisplay = new VBox();
        var sep = new Separator();
        sep.setPadding(new Insets(10));
        leftVboxRoot.getChildren().addAll(leftVboxInit,sep,leftVboxDisplay);

        var topHbox = new HBox();
        var chartProducerSelector = new ComboBox<ChartProducer>();
        chartProducerSelector.getItems().addAll(chartProducers);

        var comboboxChartDisplay = new StringConverter<ChartProducer>() {
            @Override
            public String toString(ChartProducer object) {
                return object != null ? object.getClass().getSimpleName() : "";
            }
            @Override
            public ChartProducer fromString(String string) {
                return null;
            }
        };
        chartProducerSelector.setConverter(comboboxChartDisplay);

        
        // chartProducerSelector.getSelectionModel().selectedItemProperty().addListener(producer -> {
        chartProducerSelector.valueProperty().addListener((ov,oldval,selectedProducer) -> {
            leftVboxInit.getChildren().clear();
            leftVboxDisplay.getChildren().clear();
            fillControlPanel(selectedProducer, leftVboxInit);
        });

        

        chartProducerSelector.getSelectionModel().select(0);

        
        
        var submitButton = new Button("Submit");
        submitButton.setOnAction(me -> {
            // var expectedTypes = chartProducer.getExpectedColumnsInfo().stream().map(entry -> entry.getValue()).toList();
            var chartProducer = chartProducerSelector.getSelectionModel().getSelectedItem();
            Parent chart = buildChart(chartProducer);
            
            leftVboxDisplay.getChildren().clear();
            if (XYChart.class.isAssignableFrom(chart.getClass())) {
                leftVboxDisplay.getChildren().add(buildControlPanel((XYChart<Number,Number>)chart));
            }
            
            leftVboxDisplay.getChildren().addAll(chartProducer.createControls());
            
            
            borderPane.setCenter(chart);
        });
        topHbox.getChildren().addAll(chartProducerSelector,submitButton);


        borderPane.setTop(topHbox);
        borderPane.setLeft(leftVboxRoot);

        
        final Stage stage = new Stage();
        Scene scene = new Scene(borderPane, 200, 200, Color.WHITESMOKE);
        stage.setScene(scene);
        stage.setTitle("New stage");
        stage.centerOnScreen();
        stage.show();
    }

    private void fillControlPanel(ChartProducer chartProducer, Pane controlPanel) {
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
        var parentAndChildColumns = new ArrayList<TableColumn<JsonObject, ?>>();
        for (var tc : tableView.getColumns()) {
            parentAndChildColumns.add(tc);
            for (var tcChild : tc.getColumns()) {
                parentAndChildColumns.add(tcChild);
            }
        }
        columnComboBoxes.clear();
        for (Map.Entry<String,Class> info : chartProducer.getExpectedColumnsInfo()) {
            var combo = new ComboBox<TableColumn<JsonObject,?>>();
            combo.getItems().addAll(parentAndChildColumns);
            combo.setConverter(comboboxDisplayname);
            controlPanel.getChildren().add(new Label(info+":"));
            controlPanel.getChildren().add(combo);
            columnComboBoxes.add(combo);

            // Autoselect if name the same
            for (var clmn : parentAndChildColumns) {
                if (clmn.getText().equals(info.getKey())) {
                    combo.getSelectionModel().select(clmn);
                    break;
                }
            }
        }
    }
    private Parent buildChart(ChartProducer chartProducer) {
        var expectedTypes = chartProducer.getExpectedColumnsInfo();
        return chartProducer.createContent(createArrayFromSelectedColumns(expectedTypes));
    }
    private Object[][] createArrayFromSelectedColumns(List<Map.Entry<String, Class>> targetColumnTypes) {
        var dataList = new ArrayList<List>();
        //for each row
        for (int i = 0; i < tableView.getItems().size(); i++) {
            var singleRow = new ArrayList<>();
            // for each column
            for (int j = 0; j < columnComboBoxes.size(); j++) {
                
                Class targetType = targetColumnTypes.get(j).getValue();
                boolean optional = targetColumnTypes.get(j).getKey().contains("optional");
                TableColumn<JsonObject, ?> selectedColumn = columnComboBoxes.get(j).getValue();
                
                // TODO: pls fix
                Object cellRawValue = null;
                if (selectedColumn == null) {
                    cellRawValue = "";
                } else {
                    cellRawValue = selectedColumn.getCellObservableValue(i).getValue();
                }

                if (cellRawValue == null && optional) {
                    cellRawValue = "";
                } else if (cellRawValue == null) {break;}

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

    
    private VBox buildControlPanel(XYChart<Number,Number> chart){
        var vbox = new VBox();
        String[] labels = {"xAxis Auto Ranging","yAxis Auto Ranging"};
        int i = 0;
        try {
        for (var axis : List.of((NumberAxis)chart.getXAxis(),(NumberAxis)chart.getYAxis())) {
            var autoRangingCheckBox = new CheckBox(labels[i++]);
            vbox.getChildren().add(autoRangingCheckBox);

            autoRangingCheckBox.selectedProperty().bindBidirectional(axis.autoRangingProperty());

            var spinner = createBoundSpinner(axis.lowerBoundProperty());
            spinner.disableProperty().bind(autoRangingCheckBox.selectedProperty());
            vbox.getChildren().add(spinner);

            spinner = createBoundSpinner(axis.upperBoundProperty());
            spinner.disableProperty().bind(autoRangingCheckBox.selectedProperty());
            vbox.getChildren().add(spinner);

            autoRangingCheckBox.setSelected(true);
        }
        } catch (ClassCastException e) { e.printStackTrace();}
        // vbox.setStyle("-fx-border-color: black"); // ugly
        return vbox;
    }
    private Spinner<Integer> createBoundSpinner(DoubleProperty targetDouble) {
        var spinner = new SpinnerDraggable();
        spinner.setEditable(true);

        // Bind props and keep from gc
        IntegerProperty prop = IntegerProperty.integerProperty(spinner.getValueFactory().valueProperty());
        prop.bindBidirectional(targetDouble);
        strongReferences.add(prop);
        return spinner;
    }
}
