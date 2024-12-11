package javafxtable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.StringJoiner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class JsonTableViewer extends Application {
    static HostServices hostServices;
    
    @Override
    public void start(Stage primaryStage) {
        hostServices = getHostServices();
        primaryStage.setTitle("JSON Table Viewer");
        JsonElement dataEl = getData();
        Parent root = null;
        if (dataEl.isJsonArray()) {
            root = getTableNode(dataEl.getAsJsonArray());
        } else if (dataEl.isJsonObject()) {
            var tabPane = new TabPane();
            var objectWithArrays = dataEl.getAsJsonObject();
            for (var entry : objectWithArrays.entrySet()) {
                if (entry.getValue().isJsonArray()) {
                    String tableName = entry.getKey();
                    JsonArray data = entry.getValue().getAsJsonArray();
                    var tab = new Tab(tableName, getTableNode(data));
                    tabPane.getTabs().add(tab);
                }
            }
            root = tabPane;
        }
        
        
        
        Scene scene = new Scene(root,500,300);
        primaryStage.setScene(scene);
        primaryStage.show();
        
    }

    private BorderPane getTableNode(JsonArray data) {
        TableViewJson tableView = new TableViewJson(data);
        BorderPane root = new BorderPane();
        var zp = new ZoomingPane(tableView);
        tableView.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                Double ov = zp.getZoomFactor();
                boolean zoomOut = e.getDeltaY() < 0;
                float delta = 0.08f;
                if (zoomOut) {
                    zp.zoomFactorProperty().setValue(ov*(1f-delta));
                } else {
                    zp.zoomFactorProperty().setValue(ov*(1f+delta));
                }
                e.consume();
            }
        });
        root.setCenter(zp);
        root.setTop(getSearchField(tableView));
        root.setBottom(buildCommonControl(tableView));
        return root;
    }

    private Node getSearchField(TableView<MyObject> tableView) {
        var filteredList = new FilteredList<>(tableView.getItems());
        SortedList<MyObject> sortableData = new SortedList<>(filteredList);
        tableView.setItems(sortableData);
        sortableData.comparatorProperty().bind(tableView.comparatorProperty());
        // tableView.setItems(filteredList);
        var tf = new TextField();
        tf.setPromptText(String.format("Search across %s rows...",sortableData.size()));
        tf.textProperty().addListener((obsVal,oldVal,newVal) -> {
            filteredList.setPredicate((obj) -> {
                return obj.toString().toLowerCase().contains(newVal.toLowerCase());
            });
        });
        return tf;
    }

    

    private Node buildCommonControl(TableView<MyObject> tableView) {
        final GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        int row = 0;

        /* CheckBox constrainedColumnPolicy = new CheckBox("Constrained Column Policy");
        constrainedColumnPolicy.setSelected(tableView.getColumnResizePolicy().equals(TableView.CONSTRAINED_RESIZE_POLICY));
        // Bindings.when(constrainedColumnPolicy.selectedProperty()).then(TableView.CONSTRAINED_RESIZE_POLICY).otherwise(TableView.UNCONSTRAINED_RESIZE_POLICY).bind(tableView.columnResizePolicyProperty());
        tableView.columnResizePolicyProperty().bind(Bindings.when(constrainedColumnPolicy.selectedProperty()).then(TableView.CONSTRAINED_RESIZE_POLICY).otherwise(TableView.UNCONSTRAINED_RESIZE_POLICY));
        // constrainedColumnPolicy.selectedProperty().bind(null);
        grid.add(constrainedColumnPolicy, 0, row++); */

        

        var resizePolicies = new ArrayList<Callback<ResizeFeatures, Boolean>>();
        resizePolicies.add(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        resizePolicies.add(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        resizePolicies.add(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        resizePolicies.add(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        resizePolicies.add(TableView.CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN);
        resizePolicies.add(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        resizePolicies.add(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // var resizePoliciesNames = new ArrayList<String>();
        var resizePoliciesNames = new String[] {
            "CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS",
            "CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN",
            "CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN",
            "CONSTRAINED_RESIZE_POLICY_LAST_COLUMN",
            "CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN",
            "CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS",
            "UNCONSTRAINED_RESIZE_POLICY"
        };

        /* // var radioButtons = new ArrayList<RadioButton>();
        ToggleGroup tg = new ToggleGroup();
        var radioButtons = new HBox();
        radioButtons.setSpacing(5);
        int i = 0;
        for (var rp : resizePolicies) {
            RadioButton rb = new RadioButton();
            rb.setToggleGroup(tg);
            final int iDup = i++;
            rb.setOnAction(ea -> {
                tableView.setColumnResizePolicy(rp); 
                policyLabel.setText(resizePoliciesNames[iDup]);
            });
            // radioButtons.add(rb);
            radioButtons.getChildren().add(rb);
        }
        
        grid.add(radioButtons, 0, row++); */

        var policyLabel = new Label("Resize Policy:");
        var cb = new ChoiceBox<String>();
        cb.setPrefWidth(160);
        // cb.setPrefWidth(grid.getCellBounds(0, 0).getWidth());
        
        // var firstColumnConstraints = grid.getColumnConstraints().get(0);
        // cb.prefWidthProperty().bind(firstColumnConstraints.prefWidthProperty());
        
        cb.getItems().addAll(resizePoliciesNames);
        cb.setOnAction(ea -> {
            int choice = cb.getSelectionModel().getSelectedIndex();
            tableView.setColumnResizePolicy(resizePolicies.get(choice));
        });
        cb.getSelectionModel().selectFirst();
        grid.add(policyLabel, 0, row++);
        grid.add(cb, 0, row++);
    

        /* RadioButton rb1 = new RadioButton("CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS");
        rb1.setToggleGroup(tg);
        rb1.setOnAction(ea -> {
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        }); */

        var parent = tableView.getParent();
        if (parent != null && parent instanceof ZoomingPane) {
            ZoomingPane zoomingPane = (ZoomingPane)parent;
            Slider slider = new Slider(0.5,2,1);
            zoomingPane.zoomFactorProperty().bindBidirectional(slider.valueProperty());
            

            var resetScaling = new Button("\u21BA");
            resetScaling.setOnAction(ea -> {
               zoomingPane.zoomFactorProperty().set(1); 
            });
            
            
            var label = new Label();
            label.textProperty().bind(new StringBinding() {
                { bind(slider.valueProperty()); }
                
                @Override protected String computeValue() {
                    return String.format("Scaling: %.1f", slider.getValue());
                }
            });
            grid.add(label, 0, row++);
            grid.add(slider, 0, row++);
            grid.add(resetScaling, 1, row-1);
        }
        {
            CheckBox fixedCellSize = new CheckBox("Fixed");
            fixedCellSize.setDisable(true);
            fixedCellSize.selectedProperty().bind(Bindings.greaterThan(tableView.fixedCellSizeProperty(),0));
            double defaultCellHeight = 25;
            Slider sliderCellSize = new Slider(0,100,defaultCellHeight);
            // sliderCellSize.disableProperty().bind(fixedCellSize.selectedProperty().not());

            tableView.fixedCellSizeProperty().bindBidirectional(sliderCellSize.valueProperty());

            
            var resetCellSize = new Button("\u21BA");
            resetCellSize.setOnAction(ea -> {
                tableView.fixedCellSizeProperty().set(defaultCellHeight); 
            });

            var label = new Label();
            label.textProperty().bind(new StringBinding() {
                { bind(sliderCellSize.valueProperty()); }
                
                @Override protected String computeValue() {
                    return String.format("Cell Height: %.1f", sliderCellSize.getValue());
                }
            });
            grid.add(label, 0, row++);
            grid.add(fixedCellSize, 0, row++);
            grid.add(sliderCellSize, 0, row++);
            grid.add(resetCellSize, 1, row-1);
        }
        
        /* CheckBox showTableMenuButton = new CheckBox("Show Table Menu Button");
        showTableMenuButton.selectedProperty().bind(tableView.tableMenuButtonVisibleProperty());
        grid.add(showTableMenuButton, 0, row++); */

        
        /* Slider sliderZoom = new Slider(1, 2, 1);
        tableView.scaleZProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleXProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleYProperty().bindBidirectional(sliderZoom.valueProperty());
        grid.add(sliderZoom, 0, row++); */
        /* Slider sliderX = new Slider(1, 2, 1);
        tableView.scaleZProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleXProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleYProperty().bindBidirectional(sliderZoom.valueProperty()); */
        // grid.add(sliderZoom, 0, row++);
        /* Slider sliderY = new Slider(0, 100, 0);
        tableView.translateYProperty().bindBidirectional(sliderY.valueProperty());
        grid.add(sliderY, 0, row++); */
        var tp = new TitledPane("TableView Options", grid);
        tp.setExpanded(false);
        return tp;
    }

    private static String getSampleData() {
        return """
        [{"lang_id":1,"lang_name":"Afrikaans","lang_code":"afr","latin_script":null},{"lang_id":11,"lang_name":"Albanian","lang_code":"alb","latin_script":null},{"lang_id":20,"lang_name":"Ancient Greek","lang_code":"grc","latin_script":false},{"lang_id":4,"lang_name":"Arabic","lang_code":"ara","latin_script":false},{"lang_id":5,"lang_name":"Armenian","lang_code":"arm","latin_script":false},{"lang_id":6,"lang_name":"Azerbaijani","lang_code":"aze","latin_script":null},{"lang_id":7,"lang_name":"Basque","lang_code":"baq","latin_script":null},{"lang_id":8,"lang_name":"Belarusian","lang_code":"bel","latin_script":false},{"lang_id":null,"lang_name":"Bengali","lang_code":"ben","latin_script":false},{"lang_id":10,"lang_name":null,"lang_code":"bul","latin_script":false}]
        """;
    }
    private static String readFromStdin() throws IOException {
        System.out.print("Reading input... ");
        var sj = new StringJoiner(System.lineSeparator());
        try (var br = new BufferedReader(new InputStreamReader(System.in))) {
            // StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sj.add(line);
            }
        }/*  catch (IOException e) {
            e.printStackTrace();
        } */

        System.out.println(" Done.");
        return sj.toString();
    }

    private JsonElement getData() {
        var params = getParameters().getRaw();
        String dataJson = null;
        
        try {
            if (System.in.available() > 0) {
                dataJson = readFromStdin();
            }
            else if (params.size() == 0) {
                dataJson = getSampleData();
            }
            else if (params.get(0).equals("-")) {
                dataJson = readFromStdin();
            } 
            else if (params.size() == 1) {
                dataJson = Files.readString(Path.of(params.get(0)), Charset.forName("UTF-8"));
            } 
            else if (params.size() > 1) {
                // var jsonArray = new JsonArray();
                var gsonObj = new JsonObject();
                int i = 0;
                for (var filePath : params) {
                    String arrJsonStr = Files.readString(Path.of(params.get(i++)), Charset.forName("UTF-8"));
                    JsonArray arrGson = new Gson().fromJson(arrJsonStr, JsonArray.class);
                    String filename = filePath.replaceAll("^.*[\\\\/]", "");
                    gsonObj.add(filename, arrGson);
                }
                return gsonObj;
            }
        } catch (Exception e) {
            e.printStackTrace();
            dataJson = getSampleData();
        }
        return new Gson().fromJson(dataJson, JsonElement.class);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class TableViewJson extends TableView<MyObject> {
    private JsonArray data = null;
    public TableViewJson(JsonArray data) {
        this.data = data;
        this.setTableMenuButtonVisible(true);
        this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        createColumns();
        fillData(data);
    }

    private void createColumns() {
        var referenceJsonObj = data.get(0).getAsJsonObject();
        String[] headers = referenceJsonObj.keySet().toArray(new String[0]);

        for (int i = 0; i < headers.length; i++) {
            final int columnIndex = i; // Final variable for use in lambda
            
            // find an object where value for this column is not null
            MyObject referenceDataObj = null;
            for (int j = 0; j < data.size(); j++) {
                MyObject tmpDataObj = new MyObject(data.get(j).getAsJsonObject());
                if (tmpDataObj.getColumn(columnIndex).isJsonPrimitive()) {
                    referenceDataObj = tmpDataObj;
                    break;
                }

            }
            if (referenceDataObj == null) {
                System.out.println("No values in property "+headers[columnIndex]+
                    ", there will be no column for this property.");
                continue;
            }

            Class clazz = getType(referenceDataObj.getColumn(columnIndex).getAsJsonPrimitive());
            
            if (String.class.isAssignableFrom(clazz)) {
                TableColumn<MyObject, String> column = new TableColumn<>(headers[i]);
                column.setCellValueFactory(cellData -> {
                    String value = null;
                    JsonElement element = cellData.getValue().getColumn(columnIndex);
                    if (element != null && !element.isJsonNull()) {
                        value = element.getAsString();
                    }
                    return new SimpleObjectProperty<>(value);
                });

                boolean isLink = referenceDataObj.getColumn(columnIndex).getAsString().startsWith("http");
                if (isLink) {
                    // final Application = this;
                    column.setCellFactory(tc -> {
                        var cell = new TableCell<MyObject, String>();
                        var hl = new Hyperlink();
                        cell.setGraphic(hl);
                        cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
                        // TODO: How to prevent "null" being shown when there is no link?
                        hl.textProperty().bind(cell.itemProperty().asString());
                        // hl.setText(cell.itemProperty().asString().get());
                        hl.setOnAction((ae) -> {
                            // TODO: How to get parent Application?
                            JsonTableViewer.hostServices.showDocument(hl.textProperty().get());
                        });
                        return cell;
                    });
                }
                
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
                    JsonElement element = cellData.getValue().getColumn(columnIndex);
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
                        JsonElement element = cellData.getValue().getColumn(columnIndex);
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
                    JsonElement element = cellData.getValue().getColumn(columnIndex);
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
}

class MyObject {
    private final JsonObject obj;
    private String toString;

    public MyObject(JsonObject obj) {
        this.obj = obj;
    }

    public JsonElement getColumn(int index) {
        var iter = obj.entrySet().iterator();
        // JsonPrimitive value = null;
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        var val = iter.next().getValue();
        return val;
        /* if (val.isJsonPrimitive()) {
            return val.getAsJsonPrimitive();
        } else {
            return new JsonPrimitive(val.toString());
        } */
        
    }

    public String toString() {
        if (toString == null) { 
            toString = obj.toString();
        }
        return toString;
    }
}