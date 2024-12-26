package podbrushkin.javafxcharts;

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

    public Parent createContent(Map<String,Double> map) {
        chart = new PieChart(generateData(map));
        chart.setClockwise(false);
        return chart;
    }
    public Parent createContent() {
        chart = new PieChart(generateDataSample());
        chart.setClockwise(false);
        return chart;
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