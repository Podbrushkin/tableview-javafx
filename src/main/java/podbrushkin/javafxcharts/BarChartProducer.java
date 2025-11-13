package podbrushkin.javafxcharts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class BarChartProducer implements ChartProducer {

    public ObservableList<XYChart.Series<String, Number>> generateData(Object[][] seriesXY) {

        ObservableList<XYChart.Series<String, Number>> chartData = FXCollections.observableArrayList();
        Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();

        for (Object[] row : seriesXY) {
            String seriesName = (String) row[0];
            String xValue = row[1].toString();
            Double yValue = (Double) row[2];

            if (!seriesMap.containsKey(seriesName)) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(seriesName);
                seriesMap.put(seriesName, series);
                chartData.add(series);
            }

            XYChart.Data<String, Number> data = new XYChart.Data<>(xValue, yValue);
            seriesMap.get(seriesName).getData().add(data);
        }
        return chartData;
    }
    public Parent createContent(Object[][] seriesXY) {
        // generateData(seriesXY)
        var xAxis = new CategoryAxis();
        xAxis.setAutoRanging(true);
        // xAxis.setForceZeroInRange(false);
        
        var yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);

        var chart = new BarChart<>(xAxis,yAxis);
        chart.getData().addAll(generateData(seriesXY));
        // new Barc

        for (var d : generateData(seriesXY)) {
            chart.getData().add(d);
        }
        
        
        // var chart = new LineChart(new NumberAxis(),new NumberAxis(),generateData(seriesXY));
        // chart.setClockwise(false);
        return chart;
    }

    public List<Map.Entry<String,Class>> getExpectedColumnsInfo() {
        return List.of(
            Map.entry("Series (optional)", String.class),
            Map.entry("X", String.class),
            Map.entry("Y", Double.class)
            );
    }

}
