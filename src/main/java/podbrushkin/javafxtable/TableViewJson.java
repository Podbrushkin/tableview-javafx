package podbrushkin.javafxtable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import javafx.application.HostServices;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.effect.Bloom;
import javafx.scene.input.MouseEvent;

public class TableViewJson extends TableView<MyObject> {
    private JsonArray data = null;
    private HostServices hostServices;
    private EventHandler<? super MouseEvent> onArrayClicked;

    public TableViewJson(JsonArray data) {
        this.data = data;
        createColumns();
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

    private void createColumns() {
        var referenceJsonObj = data.get(0).getAsJsonObject();
        String[] headers = referenceJsonObj.keySet().toArray(new String[0]);

        // for each property find first non-null value and create a column
        for (var columnName : headers) {
            for (int j = 0; j < data.size(); j++) {
                MyObject sampleObj = new MyObject(data.get(j).getAsJsonObject());
                JsonElement sampleValue = sampleObj.getColumn(columnName);

                if (sampleValue.isJsonPrimitive()) {
                    this.getColumns().add(createColumnForJsonPrimitives(columnName, sampleValue));
                    break;
                } else if (sampleValue.isJsonArray()) {
                    createColumnForJsonArrays(columnName);
                    break;
                } else if (sampleValue.isJsonObject()) {
                    createColumnForJsonObjects(columnName);
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
    }

    private TableColumn<MyObject, ?> createColumnForJsonPrimitives(String columnName, JsonElement sampleValue) {
        Class clazz = getType(sampleValue.getAsJsonPrimitive());

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
                JsonElement element = cellData.getValue().getColumn(columnName);
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
                JsonElement element = cellData.getValue().getColumn(columnName);
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
                JsonElement element = cellData.getValue().getColumn(columnName);
                if (element != null && !element.isJsonNull()) {
                    value = element.getAsBoolean();
                }
                return new SimpleObjectProperty<>(value);
            });
            return column;
        }
        throw new IllegalStateException("What is this column? Not a primitive. "+columnName);
    }

    private void createColumnForJsonArrays(String columnName) {
        TableColumn<MyObject, JsonArray> column = new TableColumn<>(columnName);
        column.setCellValueFactory(cellData -> {
            JsonArray value = null;
            JsonElement element = cellData.getValue().getColumn(columnName);
            if (element != null && !element.isJsonNull()) {
                value = element.getAsJsonArray();
            }
            return new SimpleObjectProperty<>(value);
        });
        column.setCellFactory(tc -> {
            TableCell<MyObject, JsonArray> cell = new TableCell<MyObject, JsonArray>() {
                @Override
                protected void updateItem(JsonArray item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        String view = String.format("▨ Object[%s]", item.size());
                        setText(empty ? null : view);
                        setOnMouseClicked(onArrayClicked);
                        setEffect(new Bloom(0.01));
                    }
                }
            };
            return cell;
        });
        this.getColumns().add(column);
    }

    private void createColumnForJsonObjects(String columnName) {
        TableColumn<MyObject, JsonObject> column = new TableColumn<>(columnName);
        column.setCellValueFactory(cellData -> {
            JsonObject value = null;
            JsonElement element = cellData.getValue().getColumn(columnName);
            if (element != null && !element.isJsonNull()) {
                value = element.getAsJsonObject();
            }
            return new SimpleObjectProperty<>(value);
        });

        column.setCellFactory(tc -> {
            TableCell<MyObject, JsonObject> cell = new TableCell<MyObject, JsonObject>() {
                @Override
                protected void updateItem(JsonObject item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty) {
                        String view = String.format("Object[%s]", item.entrySet().size());
                        setText(view);
                        // Set mouse click event to handle interaction
                        /*
                         * setOnMouseClicked(event -> {
                         * setUserData(item); // Store the JsonObject in UserData
                         * if (onObjectClicked != null) {
                         * onObjectClicked.handle(event); // Call the registered event handler}
                         * });
                         */
                    } else {
                        setText(null);
                    }
                }
            };
            return cell;
        });

        this.getColumns().add(column);
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
}