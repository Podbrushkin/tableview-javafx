package podbrushkin.javafxtable;

import java.util.function.Function;

import com.google.gson.JsonElement;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.util.Callback;
import javafx.scene.control.TableColumn.CellDataFeatures;

public class MyStringCallBack implements Callback<CellDataFeatures<MyObject,String>,ObservableValue<String>> {
    String columnName;
    // CellDataFeatures<MyObject, String> cellData;
    Function<MyObject,JsonElement> mapFunc;

    // public MyStringCallBack(){}
    public MyStringCallBack(
        String columnName,
        // CellDataFeatures<MyObject, String> cellData,
        Function<MyObject,JsonElement> mapFunc
        ){
            this.columnName = columnName;
            // this.cellData = cellData;
            this.mapFunc = mapFunc;
    }

    @Override
    public ObservableValue<String> call(CellDataFeatures<MyObject, String> cellData) {
        String value = null;
        MyObject rowObject = cellData.getValue();
        JsonElement element = mapFunc.apply(rowObject).getAsJsonObject().get(columnName);
        
        if (element != null && !element.isJsonNull()) {
            // element = mapFunc.apply(element);
            value = element.getAsString();
        } else {
            System.out.printf("there is no '%s' column in %s object. %n",columnName,cellData.getValue());
        }
        return new SimpleObjectProperty<>(value);
    }
    
}
