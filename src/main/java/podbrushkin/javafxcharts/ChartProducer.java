package podbrushkin.javafxcharts;

import java.util.List;
import java.util.Map;

import javafx.scene.Parent;

public interface ChartProducer {

    public List<Map.Entry<String,Class>> getExpectedColumnsInfo();
    public Parent createContent(Object[][] seriesXY);
}
