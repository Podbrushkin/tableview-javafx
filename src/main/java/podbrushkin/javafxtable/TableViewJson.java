package podbrushkin.javafxtable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import javafx.application.HostServices;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.effect.Bloom;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TableViewJson extends TableView<MyObject> {
    private HostServices hostServices;
    private EventHandler<? super MouseEvent> onArrayClicked;

    public TableViewJson(JsonArray data) {
        // this.data = data;
        for (var column : buildColumns(data,null)) {
            this.getColumns().add(column);
        }
        fillData(data);
    }

    public TableViewJson(JsonArray data, HostServices hostServices) {
        this(data);
        this.hostServices = hostServices;
    }

    public TableViewJson(
            JsonArray data,
            HostServices hostServices,
            EventHandler<? super MouseEvent> onArrayClicked) {
        this(data, hostServices);
        this.onArrayClicked = onArrayClicked;
    }

    /* private List<TableColumn<MyObject,?>> buildColumns(JsonArray data) {
        
    } */
    private List<TableColumn<MyObject,?>> buildColumns(JsonArray data, BiFunction<MyObject,String,JsonElement> rowToCellFunc) {
        var referenceJsonObj = data.get(0).getAsJsonObject();
        String[] headers = referenceJsonObj.keySet().toArray(new String[0]);
        var builtColumns = new ArrayList<TableColumn<MyObject,?>>();
        // for each property find first non-null value and create a column
        for (var columnName : headers) {
            for (int j = 0; j < data.size(); j++) {
                MyObject sampleObj = new MyObject(data.get(j).getAsJsonObject());
                JsonElement sampleValue = sampleObj.getColumn(columnName);

                if (sampleValue.isJsonPrimitive()) {
                    builtColumns.add(createColumnForJsonPrimitives(columnName, sampleValue, rowToCellFunc));
                    break;
                } else if (sampleValue.isJsonArray()) {
                    builtColumns.add(createColumnForJsonArrays(columnName));
                    break;
                } else if (sampleValue.isJsonObject()) {
                    builtColumns.add(createColumnForJsonObjects(columnName));
                    break;
                }

                else if (sampleValue.isJsonNull() && j < data.size()) {
                    continue;
                } else {
                    System.out.println("There is no non-null values in property " + columnName +
                            ", there will be no column for this property.");
                    break;
                }
            }
        }
        return builtColumns;
    }

    private TableColumn<MyObject, ?> createColumnForJsonPrimitives(String columnName, JsonElement sampleValue) {
        BiFunction<MyObject,String,JsonElement> rowToCellFunc = (myObj,propName) -> myObj.getColumn(columnName);
        return createColumnForJsonPrimitives(columnName, sampleValue, rowToCellFunc);
    }
    private TableColumn<MyObject, ?> createColumnForJsonPrimitives(String columnName, JsonElement sampleValue, BiFunction<MyObject,String,JsonElement> rowToCellFunc) {
        // :TODO please change
        if (rowToCellFunc == null) {
            return createColumnForJsonPrimitives(columnName, sampleValue);
        }
        Class clazz = getType(sampleValue.getAsJsonPrimitive());

        if (String.class.isAssignableFrom(clazz)) {
            TableColumn<MyObject, String> column = new TableColumn<>(columnName);
            
            column.setCellValueFactory(cellData -> {
                String value = null;
                JsonElement element = rowToCellFunc.apply(cellData.getValue(),columnName);
                if (element != null && !element.isJsonNull()) {
                    value = element.getAsString();
                }
                return new SimpleObjectProperty<>(value);
            });
            column.setCellFactory(tc -> {
                TableCell<MyObject, String> cell = new TableCell<MyObject, String>() {
                    boolean isLink(String item, boolean empty) {
                        return !empty && item != null && getText() != null && item.matches("^https?://.*");
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item);
                        // System.out.printf("getText()=%s item=%s%n",getText(),item);
                        setOnMouseClicked(e -> {
                            if (isLink(item, empty) && hostServices != null) {
                                hostServices.showDocument(item);
                            }
                        });
                        setOnMouseEntered(e -> {
                            if (isLink(item, empty) && hostServices != null) {
                                setCursor(Cursor.HAND);
                            }
                        });
                    }
                };
                return cell;
            });
            /*
             * // this makes word wrap but how do you toggle it?
             * column.setCellFactory(tc -> {
             * var cell = new TableCell<MyObject, String>();
             * Text text = new Text();
             * cell.setGraphic(text);
             * cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
             * text.wrappingWidthProperty().bind(column.widthProperty());
             * text.textProperty().bind(cell.itemProperty().asString());
             * return cell;
             * });
             */
            return column;
        }
        

        if (Integer.class.isAssignableFrom(clazz)) {
            TableColumn<MyObject, Integer> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Integer value = null;
                JsonElement element = rowToCellFunc.apply(cellData.getValue(),columnName);
                
                if (element != null && !element.isJsonNull()) {
                    value = element.getAsInt();
                }
                return new SimpleObjectProperty<>(value);
            });
            return column;
        }
        if (Float.class.isAssignableFrom(clazz)) {
            TableColumn<MyObject, Float> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Float value = null;
                JsonElement element = rowToCellFunc.apply(cellData.getValue(),columnName);
                if (element != null && !element.isJsonNull()) {
                    value = element.getAsFloat();
                }
                return new SimpleObjectProperty<>(value);
            });
            return column;
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            TableColumn<MyObject, Boolean> column = new TableColumn<>(columnName);
            column.setCellValueFactory(cellData -> {
                Boolean value = null;
                JsonElement element = rowToCellFunc.apply(cellData.getValue(),columnName);
                if (element != null && !element.isJsonNull()) {
                    value = element.getAsBoolean();
                }
                return new SimpleObjectProperty<>(value);
            });
            return column;
        }
        throw new IllegalStateException("What is this column? Not a primitive. "+columnName);
    }

    private TableColumn<MyObject, JsonArray> createColumnForJsonArrays(String columnName) {
        TableColumn<MyObject, JsonArray> column = new TableColumn<>(columnName);
        column.setCellValueFactory(cellData -> {
            JsonArray value = null;
            JsonElement element = cellData.getValue().getColumn(columnName);
            try {
                if (element != null && !element.isJsonNull()) {
                    value = element.getAsJsonArray();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                // value = element.toString();
            }
            return new SimpleObjectProperty<>(value);
        });
        column.setCellFactory(tc -> {
            TableCell<MyObject, JsonArray> cell = new TableCell<MyObject, JsonArray>() {
                @Override
                protected void updateItem(JsonArray item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        String view = String.format("▨ Array[%s]", item.size());
                        setText(empty ? null : view);
                        setOnMouseClicked(onArrayClicked);
                        setEffect(new Bloom(0.01));
                    }
                }
            };
            return cell;
        });
        return column;
    }

    private TableColumn<MyObject, JsonObject> createColumnForJsonObjects(String columnName) {
        TableColumn<MyObject, JsonObject> column = new TableColumn<>(columnName);
        column.setCellValueFactory(cellData -> {
            JsonObject value = null;
            JsonElement element = cellData.getValue().getColumn(columnName);
            if (element != null && !element.isJsonNull()) {
                value = element.getAsJsonObject();
            }
            return new SimpleObjectProperty<>(value);
        });

        // this will create a new window table
        EventHandler<? super MouseEvent> newWindowTableHandler = e -> {
            var tableCell = (TableCell<MyObject, JsonArray>)e.getSource();
            var tableColumn = tableCell.getTableColumn();
            var columnData = new JsonArray();
            for (var val : tableCell.getTableView().getItems()) {
                columnData.add(tableColumn.getCellObservableValue(val).getValue());
            }
            displayTable(new TableViewJson(columnData));
        };
        EventHandler<? super MouseEvent> newColumnsTableHandler = e -> {
            var tableCell = (TableCell<MyObject, JsonArray>)e.getSource();
            var parentTableColumn = tableCell.getTableColumn();
            var dataForNewColumns = new JsonArray();
            for (var val : tableCell.getTableView().getItems()) {
                dataForNewColumns.add(parentTableColumn.getCellObservableValue(val).getValue());
            }
            
            BiFunction<MyObject,String,JsonElement> rowObjectToValue = 
                (myObj,propName) -> myObj.getColumn(parentTableColumn.getText()).getAsJsonObject().get(propName);

            for (TableColumn<MyObject, ?> childColumn : buildColumns(dataForNewColumns,rowObjectToValue)) {
                parentTableColumn.getColumns().add(childColumn);
            };
            this.refresh();    // prevents dead cells issue with nested columns
        };

        column.setCellFactory(tc -> {
            TableCell<MyObject, JsonObject> cell = new TableCell<MyObject, JsonObject>() {
                @Override
                protected void updateItem(JsonObject item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        var propNames = String.join(", ", item.keySet());
                        String view = String.format("Object[%s]", propNames);
                        setEffect(new Bloom(0.1));
                        setText(view);
                        setOnMouseClicked(newColumnsTableHandler);
                    } else {
                        setText(null);
                    }
                }
            };
            return cell;
        });

        return column;
    }
    
    private void fillData(JsonArray data) {
        for (int i = 0; i < data.size(); i++) {
            // Object[] rowValues = jsonObjectToValuesArray();
            MyObject row = new MyObject(data.get(i).getAsJsonObject());
            this.getItems().add(row);
        }
    }

    private Class getType(JsonPrimitive prim) {
        Class clazz = null;
        // var prim = e.getValue().getAsJsonPrimitive();
        if (prim.isString()) {
            clazz = String.class;
        } else if (prim.isNumber()) {
            String num = prim.getAsString();
            boolean isFloat = num.matches("[-+]?[0-9]*\\.[0-9]+");
            if (isFloat) {
                clazz = Float.class;
            } else {
                clazz = Integer.class;
            }
        } else if (prim.isBoolean()) {
            clazz = Boolean.class;
        }
        return clazz;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void setOnArrayClicked(EventHandler<? super MouseEvent> onArrayClicked) {
        this.onArrayClicked = onArrayClicked;
    }

    private void displayTable(TableView tableView) {
        final Stage stage = new Stage();
            var rootGroup = new BorderPane(tableView);
            Scene scene = new Scene(rootGroup, 200, 200, Color.WHITESMOKE);
            stage.setScene(scene);
            stage.setTitle("New stage");
            stage.centerOnScreen();
            stage.show();

    }
}