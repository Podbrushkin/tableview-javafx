package podbrushkin.javafxcharts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class LineChartProducer implements ChartProducer {

    public ObservableList<XYChart.Series<Double, Double>> generateData(Object[][] seriesXY) {
        System.out.printf("Object[%s][%s]%n", seriesXY.length, seriesXY[0].length);
        for (var arr : seriesXY) {
            System.out.println(Arrays.toString(arr));
        }

        ObservableList<XYChart.Series<Double, Double>> lineChartData = FXCollections.observableArrayList();
        Map<String, XYChart.Series<Double, Double>> seriesMap = new HashMap<>();

        for (Object[] row : seriesXY) {
            String seriesName = (String) row[0];
            Double xValue = (Double) row[1];
            Double yValue = (Double) row[2];

            if (!seriesMap.containsKey(seriesName)) {
                XYChart.Series<Double, Double> series = new XYChart.Series<>();
                series.setName(seriesName);
                seriesMap.put(seriesName, series);
                lineChartData.add(series);
            }

            XYChart.Data<Double, Double> data = new XYChart.Data<>(xValue, yValue);
            seriesMap.get(seriesName).getData().add(data);
        }
        return lineChartData;
    }
    public Parent createContent(Object[][] seriesXY) {
        // generateData(seriesXY)
        var xAxis = new NumberAxis();
        xAxis.setAutoRanging(true);
        xAxis.setForceZeroInRange(false);
        
        var yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);

        var chart = new LineChart(xAxis,yAxis);
        chart.getData().addAll(generateData(seriesXY));
        
        
        // var chart = new LineChart(new NumberAxis(),new NumberAxis(),generateData(seriesXY));
        // chart.setClockwise(false);
        return chart;
    }

    public List<Map.Entry<String,Class>> getExpectedColumnsInfo() {
        return List.of(
            Map.entry("Series (optional)", String.class),
            Map.entry("X", Double.class),
            Map.entry("Y", Double.class)
            );
    }

}
