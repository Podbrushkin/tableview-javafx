package podbrushkin.javafxcharts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.stage.Stage;

public class PieChartProducer extends Application {

    private PieChart chart;

    public static ObservableList<PieChart.Data> generateData(Map<String,Double> nameToPercent) {
        var data = nameToPercent.entrySet().stream().map(e -> new PieChart.Data(e.getKey(), e.getValue())).toList();
        return FXCollections.observableArrayList(data);
    }
    public static ObservableList<PieChart.Data> generateDataSample() {
        return FXCollections.observableArrayList(
                new PieChart.Data("Sun", 20),
                new PieChart.Data("IBM", 12),
                new PieChart.Data("HP", 25),
                new PieChart.Data("Dell", 22),
                new PieChart.Data("Apple", 30));
    }



    public ObservableList<PieChart.Data> generateData(Object[][] labelAndValue) {
        System.out.printf("Object[%s][%s]%n",labelAndValue.length,labelAndValue[0].length);
        for (var arr : labelAndValue) {
            System.out.println(Arrays.toString(arr));
        }
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Object[] row : labelAndValue) {
            var entry = new PieChart.Data((String)row[0], (Double)row[1]);
            data.add(entry);
        }
        return data;
    }
    public Parent createContent(Object[][] labelAndValue) {
        chart = new PieChart(generateData(labelAndValue));
        chart.setClockwise(false);
        return chart;
    }
    
    public Parent createContent() {
        chart = new PieChart(generateDataSample());
        chart.setClockwise(false);
        return chart;
    }

    public List<Map.Entry<String,Class>> getExpectedColumnsInfo() {
        return List.of(
            Map.entry("Label", String.class),
            Map.entry("Value", Double.class)
            );
    }

    @Override public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}