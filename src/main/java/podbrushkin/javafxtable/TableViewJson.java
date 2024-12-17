package podbrushkin.javafxtable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import javafx.application.HostServices;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class TableViewJson extends TableView<MyObject> {
    private JsonArray data = null;
    private HostServices hostServices;
    public TableViewJson(JsonArray data) {
        this.data = data;
        createColumns();
        fillData(data);
    }
    public TableViewJson(JsonArray data, HostServices hostServices) {
        this(data);
        this.hostServices = hostServices;
    }

    private void createColumns() {
        var referenceJsonObj = data.get(0).getAsJsonObject();
        String[] headers = referenceJsonObj.keySet().toArray(new String[0]);

        for (int i = 0; i < headers.length; i++) {
            final int columnIndex = i; // Final variable for use in lambda
            final String columnName = headers[columnIndex];
            
            // find an object where value for this column is not null
            MyObject referenceDataObj = null;
            for (int j = 0; j < data.size(); j++) {
                MyObject tmpDataObj = new MyObject(data.get(j).getAsJsonObject());
                if (tmpDataObj.getColumn(columnName).isJsonPrimitive()) {
                    referenceDataObj = tmpDataObj;
                    break;
                }

            }
            if (referenceDataObj == null) {
                System.out.println("No values in property "+columnName+
                    ", there will be no column for this property.");
                continue;
            }

            Class clazz = getType(referenceDataObj.getColumn(columnName).getAsJsonPrimitive());
            
            if (String.class.isAssignableFrom(clazz)) {
                TableColumn<MyObject, String> column = new TableColumn<>(columnName);
                column.setCellValueFactory(cellData -> {
                    String value = null;
                    JsonElement element = cellData.getValue().getColumn(columnName);
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
                /* // this makes word wrap but how do you toggle it?
                column.setCellFactory(tc -> {
                    var cell = new TableCell<MyObject, String>();
                    Text text = new Text();
                    cell.setGraphic(text);
                    cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
                    text.wrappingWidthProperty().bind(column.widthProperty());
                    text.textProperty().bind(cell.itemProperty().asString());
                    return cell;
                }); */
                this.getColumns().add(column);
            }
            if (Integer.class.isAssignableFrom(clazz)) {
                TableColumn<MyObject, Integer> column = new TableColumn<>(headers[i]);
                column.setCellValueFactory(cellData -> {
                    Integer value = null;
                    JsonElement element = cellData.getValue().getColumn(columnName);
                    if (element != null && !element.isJsonNull()) {
                        value = element.getAsInt();
                    }
                    return new SimpleObjectProperty<>(value);
                });
                this.getColumns().add(column);
            }
            if (Float.class.isAssignableFrom(clazz)) {
                TableColumn<MyObject, Float> column = new TableColumn<>(headers[i]);
                column.setCellValueFactory(cellData -> {
                        Float value = null;
                        JsonElement element = cellData.getValue().getColumn(columnName);
                        if (element != null && !element.isJsonNull()) {
                            value = element.getAsFloat();
                        }
                        return new SimpleObjectProperty<>(value);
                });
                this.getColumns().add(column);
            }
            if (Boolean.class.isAssignableFrom(clazz)) {
                TableColumn<MyObject, Boolean> column = new TableColumn<>(headers[i]);
                column.setCellValueFactory(cellData -> {
                    Boolean value = null;
                    JsonElement element = cellData.getValue().getColumn(columnName);
                    if (element != null && !element.isJsonNull()) {
                        value = element.getAsBoolean();
                    }
                    return new SimpleObjectProperty<>(value);
                });
                this.getColumns().add(column);
            }
        }
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
        } 
        else if (prim.isNumber()) {
            String num = prim.getAsString();
            boolean isFloat = num.matches("[-+]?[0-9]*\\.[0-9]+");
            if (isFloat) {
                clazz = Float.class;
            } else {
                clazz = Integer.class;
            }
        }
        else if (prim.isBoolean()) {
            clazz = Boolean.class;
        }
        return clazz;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}