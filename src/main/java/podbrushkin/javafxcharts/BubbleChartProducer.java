package podbrushkin.javafxcharts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BubbleChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;

public class BubbleChartProducer implements ChartProducer {
    private CircularBubbleChart chart;
    
    public ObservableList<XYChart.Series<Double, Double>> generateData(Object[][] seriesXY) {
        ObservableList<XYChart.Series<Double, Double>> xyChartData = FXCollections.observableArrayList();
        Map<String, XYChart.Series<Double, Double>> seriesMap = new HashMap<>();

        for (Object[] row : seriesXY) {
            String seriesName = (String) row[0];
            Double xValue = (Double) row[1];
            Double yValue = (Double) row[2];
            Double size = (Double) row[3];

            if (!seriesMap.containsKey(seriesName)) {
                XYChart.Series<Double, Double> series = new XYChart.Series<>();
                series.setName(seriesName);
                seriesMap.put(seriesName, series);
                xyChartData.add(series);
            }

            XYChart.Data<Double, Double> data = new XYChart.Data<>(xValue, yValue, size);
            seriesMap.get(seriesName).getData().add(data);
        }
        return xyChartData;
    }
    public Parent createContent(Object[][] seriesXY) {
        // generateData(seriesXY)
        var xAxis = new NumberAxis();
        xAxis.setAutoRanging(true);
        xAxis.setForceZeroInRange(false);
        
        var yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);

        chart = new CircularBubbleChart(xAxis,yAxis);
        chart.getData().addAll(generateData(seriesXY));
        return chart;
    }

    public List<Map.Entry<String,Class>> getExpectedColumnsInfo() {
        return List.of(
            Map.entry("Series", String.class),
            Map.entry("X", Double.class),
            Map.entry("Y", Double.class),
            Map.entry("Size", Double.class)
            );
    }

    @Override
    public List<Node> createControls() {
        var controls = new ArrayList<Node>();
        var bubbleSizeSlider = new Slider(0, 5, 1);
        bubbleSizeSlider.valueProperty().bindBidirectional(chart.bubbleSizeFactorProperty());
        controls.add(new Label("Bubble Size"));
        controls.add(bubbleSizeSlider);
        return controls;
    }

    
}

// https://stackoverflow.com/a/38614934/
class CircularBubbleChart<X, Y> extends BubbleChart<X, Y> {
    private DoubleProperty bubbleSizeFactorProperty = new SimpleDoubleProperty(1);

    public CircularBubbleChart(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
        bubbleSizeFactorProperty.addListener((observable, oldValue, newValue) -> {
            layoutPlotChildren(); // Call layoutPlotChildren when the property changes
        });
    }

    public CircularBubbleChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
    }

    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        getData().stream().flatMap(series -> series.getData().stream())
            .map(Data::getNode)
            .map(StackPane.class::cast)
            .map(StackPane::getShape)
            .map(Ellipse.class::cast)
            .forEach(ellipse -> {
                double radius = Math.sqrt(ellipse.getRadiusX()*ellipse.getRadiusY())*bubbleSizeFactorProperty.get();
                ellipse.setRadiusX(radius);
                ellipse.setRadiusY(radius);
            });
        
    }

    public double getBubbleSizeFactor() {
        return bubbleSizeFactorProperty.get();
    }

    public void setBubbleSizeFactor(double bubbleSizeFactor) {
        bubbleSizeFactorProperty.set(bubbleSizeFactor);
    }
    public final DoubleProperty bubbleSizeFactorProperty() {
        return bubbleSizeFactorProperty;
    }
}