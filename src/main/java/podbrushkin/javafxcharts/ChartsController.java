package podbrushkin.javafxcharts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import podbrushkin.javafxtable.TableViewJson;

public class ChartsController {

    private TableViewJson tableView;
    private ArrayList<ComboBox<TableColumn<JsonObject,?>>> columnComboBoxes = new ArrayList<>();
    private List<ChartProducer> chartProducers = List.of(
        new PieChartProducer(), 
        new LineChartProducer(),
        new ScatterChartProducer(),
        new BubbleChartProducer()
    );

    public ChartsController(TableViewJson tableView) {
        this.tableView = tableView;
    }
    public void showWindow() {
        var borderPane = new BorderPane();
        
        var leftVboxControls = new VBox();
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
            leftVboxControls.getChildren().clear();
            fillControlPanel(selectedProducer, leftVboxControls);
        });

        

        chartProducerSelector.getSelectionModel().select(0);

        
        
        var submitButton = new Button("Submit");
        submitButton.setOnAction(me -> {
            // var expectedTypes = chartProducer.getExpectedColumnsInfo().stream().map(entry -> entry.getValue()).toList();
            var chartProducer = chartProducerSelector.getSelectionModel().getSelectedItem();
            Parent chart = buildChart(chartProducer);
            // var x = ;
            if (XYChart.class.isAssignableFrom(chart.getClass())) {
                leftVboxControls.getChildren().add(buildControlPanel((XYChart<Number,Number>)chart));
            }
            
            borderPane.setCenter(chart);
        });
        topHbox.getChildren().addAll(chartProducerSelector,submitButton);


        borderPane.setTop(topHbox);
        borderPane.setLeft(leftVboxControls);

        
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
        // var castedChart = (LineChart)chart;
        var xAxis = (NumberAxis)chart.getXAxis();
        

        Spinner<Double> xLowerSp = createBoundSpinner(xAxis.lowerBoundProperty(), Integer.MIN_VALUE, Integer.MAX_VALUE);

        var autoRangingCheckBox = new CheckBox("Xaxis.autoRanging");
        
        // xAxis.setAutoRanging(true);
        autoRangingCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
            xAxis.setAutoRanging(newValue);
            if (newValue) {
                // xAxis.lowerBoundProperty().asObject().unbind();
                // xLowerSp.getValueFactory().setValue(xAxis.getLowerBound()); 
                // xLowerSp.getValueFactory().valueProperty().bind(xAxis.lowerBoundProperty().asObject());
            } else {
                // xAxis.lowerBoundProperty().asObject().bind(xLowerSp.valueProperty());
                xLowerSp.getValueFactory().setValue(xAxis.getLowerBound()); 
                
            }
        });
        autoRangingCheckBox.setSelected(true);
        
        // autorangingCb.selectedProperty().bindBidirectional(xAxis.autoRangingProperty());

        
        xLowerSp.disableProperty().bind(autoRangingCheckBox.selectedProperty());

        // xLowerSp.getValueFactory().valueProperty().bindBidirectional(xAxis.lowerBoundProperty().asObject());
        // xAxis.lowerBoundProperty().asObject().bindBidirectional(xLowerSp.getValueFactory().valueProperty());
        
        
        xLowerSp.valueProperty().addListener((obs, oldValue, newValue) -> {
            // if (!xAxis.isAutoRanging()) {
            xAxis.setLowerBound(newValue);
            // }
        });

        vbox.getChildren().addAll(
            autoRangingCheckBox,
            new Label("Xaxis.lowerBoundProperty"),
            xLowerSp
            // new Label("Xaxis.upperBoundProperty"),
            // createBoundSpinner(xAxis.upperBoundProperty(), Integer.MIN_VALUE, Integer.MAX_VALUE)
        );
        return vbox;
    }
    public Spinner<Double> createBoundSpinner(DoubleProperty doubleProperty, double min, double max) {
        Spinner<Double> spinner = new Spinner<>();
        SpinnerValueFactory<Double> valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max);
        spinner.setValueFactory(valueFactory);
        spinner.setEditable(true);
        // System.out.println(doubleProperty.doubleValue());
        // doubleProperty.bind(spinner.valueProperty());
        // spinner.getValueFactory().valueProperty().bindBidirectional(doubleProperty.asObject());
        final double[] mouseAnchorY = {0d};
        final double[] spinnerValOnStartDrag = {0d};
        
        spinner.getEditor().setOnMousePressed(event -> {
            // Capture the starting Y position and spinner value
            mouseAnchorY[0] = event.getSceneY();
            spinnerValOnStartDrag[0] = spinner.getValue();
        });
        // Mouse dragged event to calculate new value
        spinner.getEditor().setOnMouseDragged(event -> {
            double deltaY = mouseAnchorY[0] - event.getSceneY();
            
            var valAbs = Math.abs(spinnerValOnStartDrag[0]);
            var factor = String.valueOf(valAbs).length();
            
            double newValue = spinnerValOnStartDrag[0]+deltaY*factor;
            spinner.getValueFactory().setValue(newValue);
        });
        return spinner;
    }
    /* public HBox createBoundSliderAndTextField(DoubleProperty doubleProperty, double min, double max) {
        Slider slider = new Slider(min, max, doubleProperty.get());
        TextField textField = new TextField();

        // Bind the slider value to the DoubleProperty
        slider.valueProperty().bind(doubleProperty);

        // Bind the text of the TextField to the DoubleProperty
        textField.textProperty().bindBidirectional(doubleProperty, java.text.NumberFormat.getNumberInstance());

        // Format TextField value when focus is lost
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                textField.setText(java.text.NumberFormat.getNumberInstance().format(doubleProperty.get()));
            }
        });

        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(slider, textField);
        return hbox;
    } */
}
