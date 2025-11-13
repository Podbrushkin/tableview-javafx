package podbrushkin.javafxtable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import podbrushkin.javafxcharts.ChartsController;
import podbrushkin.javafxcharts.utils.IOUtils;

public class JsonTableViewer extends Application {
    private TabPane tabPane = new TabPane();
    private Map<JsonElement,Integer> elementToOriginalIndex = new HashMap<>();
    @Override
    public void start(Stage primaryStage) {
        // hostServices = getHostServices();
        
        boolean passThru = getParameters().getRaw().contains("--pass-thru");
        primaryStage.setTitle("JSON Table Viewer");
        JsonElement dataEl = getData();
        // Parent root = null;
        if (dataEl.isJsonArray()) {
            var jsonArr = dataEl.getAsJsonArray();
            for (int i = 0; i < jsonArr.size(); i++)
                elementToOriginalIndex.put(jsonArr.get(i), i);

            tabPane.getTabs().add(new Tab("root",getTableNode(jsonArr, passThru)));
        } else if (dataEl.isJsonObject()) {
            var objectWithArrays = dataEl.getAsJsonObject();
            for (var entry : objectWithArrays.entrySet()) {
                if (entry.getValue().isJsonArray()) {
                    String tableName = entry.getKey();
                    JsonArray data = entry.getValue().getAsJsonArray();
                    var tab = new Tab(tableName, getTableNode(data, passThru));
                    tabPane.getTabs().add(tab);
                }
            }
        }
        
        Scene scene = new Scene(tabPane,500,300);
        primaryStage.setScene(scene);
        primaryStage.show();
        
    }

    private BorderPane getTableNode(JsonArray data) {
        return getTableNode(data, false);
    }
    private BorderPane getTableNode(JsonArray data, boolean passThru) {
        TableViewJson tableView = new TableViewJson(data,getHostServices());
        tableView.setOnArrayClicked(me -> {

            TableCell<JsonObject, JsonArray> cell = (TableCell<JsonObject, JsonArray>) me.getSource();
            JsonArray chosenArray = cell.getItem();
            tabPane.getTabs().add(new Tab("array", getTableNode(chosenArray)));
            tabPane.getSelectionModel().selectLast();
        });
        var zp = buildZoomingPane(tableView);
        tableView.setTableMenuButtonVisible(true);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);

        // final Pane spacer = new Pane();
        // HBox.setHgrow(spacer, Priority.ALWAYS);
        var sf = buildSearchField(tableView);
        HBox.setHgrow(sf, Priority.ALWAYS);
        HBox topRow = new HBox(sf);
        if (passThru) {
            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            var submitButton = new Button("Submit");
            submitButton.setStyle("-fx-base: green;");
            submitButton.setOnAction(ea -> {
                tableView.getSelectionModel().getSelectedItems().forEach(el -> {
                    if (elementToOriginalIndex.containsKey(el))
                        System.out.println(elementToOriginalIndex.get(el));
                    else System.out.println(el);
                });
                Platform.exit();
            });
            topRow.getChildren().add(submitButton);
        }
        BorderPane root = new BorderPane();
        var controlPanel = buildCommonControl(tableView);
        
        var scrollPane = new ScrollPane(controlPanel);
        
        root.setBottom(scrollPane);
        scrollPane.setMinHeight(0);
        scrollPane.prefHeightProperty().set(0);
        
        var settingsButton = new ToggleButton("⚙"); // \u2699 = ⚙️
        
        settingsButton.setOnAction(e -> {
            double contentHeight = scrollPane.getContent().prefHeight(-1)+5;
            double parentHeightRel = root.getHeight();
            double expandedHeight = Math.min(contentHeight,parentHeightRel/2);

            double target = scrollPane.getHeight() <= 0 ? expandedHeight : 0 ;
            if (target != 0) {
                scrollPane.setVisible(true);
            }
            KeyValue keyValue = new KeyValue(scrollPane.prefHeightProperty(), target);
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), keyValue));
            timeline.play();
            if (target == 0) {
                timeline.setOnFinished(ae -> {
                    scrollPane.setVisible(false);
                });
            }
        });
        topRow.getChildren().add(settingsButton);
        
        var chartsButton = new Button("Charts");
        chartsButton.setOnAction(e -> new ChartsController(tableView).showWindow());
        topRow.getChildren().add(chartsButton);
        
        root.setCenter(zp);
        // root.setTop(getSearchField(tableView));
        root.setTop(topRow);
        // root.setBottom();
        return root;
    }

    public static ZoomingPane buildZoomingPane(TableView<?> tableView) {
        ZoomingPane zp = new ZoomingPane(tableView);
        tableView.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                if (e.getDeltaY() == 0d) return;
                
                Double zf = zp.getZoomFactor();
                System.out.println("zf="+zf+"deltaY="+e.getDeltaY());
                boolean zoomOut = e.getDeltaY() < 0;
                float delta = 0.1f;
                if (zoomOut) {
                    zp.zoomFactorProperty().setValue(zf*(1f-delta));
                } else {
                    zp.zoomFactorProperty().setValue(zf*(1f+delta));
                }
                e.consume();
            }
        });
        return zp;
    }

    public static Node buildSearchField(TableView<JsonObject> tableView) {
        var filteredList = new FilteredList<>(tableView.getItems());
        SortedList<JsonObject> sortableData = new SortedList<>(filteredList);
        tableView.setItems(sortableData);
        sortableData.comparatorProperty().bind(tableView.comparatorProperty());
        // tableView.setItems(filteredList);
        var tf = new TextField();
        tf.setPromptText(String.format("Search across %s rows...",sortableData.size()));
        tf.textProperty().addListener((obsVal,oldVal,newVal) -> {
            filteredList.setPredicate((obj) -> {
                tableView.refresh();    // prevents dead cells issue with nested columns
                return obj.toString().toLowerCase().contains(newVal.toLowerCase());
            });
        });
        var clearButton = new Button("✕");
        clearButton.setFont(new Font(8));
        clearButton.setVisible(false);
        clearButton.setOnAction((ActionEvent actionEvent) -> {
            tf.setText("");
            // tf.requestFocus();
        });
        final ChangeListener<String> textListener =
            (ObservableValue<? extends String> observable,
             String oldValue, String newValue) -> {
                clearButton.setVisible(tf.getText().length() != 0);
            };
        tf.textProperty().addListener(textListener);
        
        HBox region = new HBox() {
            @Override
            protected void layoutChildren() {
                getChildren().get(0).resize(getWidth(), getHeight());
                // System.out.printf("%s %s%n",getWidth(), getHeight());
                double side = getHeight()*0.8;
                double offset = getHeight()/2-side/2;
                getChildren().get(1).resizeRelocate(getWidth() - side - offset, offset, side, side);
                // getChildren().get(1).relocate(getWidth() - 18, 0);
            }
        };
        region.getChildren().addAll(tf,clearButton);
        return region;
    }

    

    public static Node buildCommonControl(TableView<JsonObject> tableView) {
        final GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        int row = 0;

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


        var policyLabel = new Label("Resize Policy:");
        var cb = new ChoiceBox<Callback<TableView.ResizeFeatures, Boolean>>();
        // var cb = new ChoiceBox<String>();
        cb.setPrefWidth(160);
        cb.getItems().addAll(resizePolicies);
        cb.setConverter(
            new StringConverter<>() {
                @Override
                public String toString(Callback<TableView.ResizeFeatures,Boolean> resizePolicy) {
                    int i = resizePolicies.indexOf(resizePolicy);
                    return resizePoliciesNames[i];
                }
                @Override
                public Callback<TableView.ResizeFeatures,Boolean> fromString(String string) {
                    int i = List.of(resizePoliciesNames).indexOf(string);
                    return resizePolicies.get(i);
                }
            }
        );
        
        cb.valueProperty().bindBidirectional(tableView.columnResizePolicyProperty());
        
        
        grid.add(policyLabel, 0, row++);
        grid.add(cb, 0, row++);
    
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
        
        /* //TODO: implement zooming in addition to scaling
        Slider sliderZoom = new Slider(1, 2, 1);
        tableView.scaleZProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleXProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleYProperty().bindBidirectional(sliderZoom.valueProperty());
        grid.add(sliderZoom, 0, row++);
         Slider sliderX = new Slider(1, 2, 1);
        tableView.scaleZProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleXProperty().bindBidirectional(sliderZoom.valueProperty());
        tableView.scaleYProperty().bindBidirectional(sliderZoom.valueProperty()); */
        // grid.add(sliderZoom, 0, row++);
        /* Slider sliderY = new Slider(0, 100, 0);
        tableView.translateYProperty().bindBidirectional(sliderY.valueProperty());
        grid.add(sliderY, 0, row++); */
        return grid;
    }

    private static String getSampleData() {
        try (InputStream in = JsonTableViewer.class.getResourceAsStream("/sample.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {e.printStackTrace();}
        return """
        [{"el":null,"elLabel":"Charles III","born":"1948-11-14","died":null,"links":163,"yearsLived":null},{"el":null,"elLabel":"Nicholas II of Russia","born":"1868-05-18","died":"1918-07-17","links":128,"yearsLived":50.1945205479452},{"el":"http://www.wikidata.org/entity/Q80976","elLabel":"Prince Philip, Duke of Edinburgh","born":"1921-06-10","died":"2021-04-09","links":125,"yearsLived":99.8986301369863},{"el":null,"elLabel":"Felipe VI of Spain","born":"1968-01-30","died":null,"links":113,"yearsLived":null},{"el":"http://www.wikidata.org/entity/Q102139","elLabel":"Margrethe II of Denmark","born":"1940-04-16","died":null,"links":112,"yearsLived":null}]
        """;
    }
    private static String readFromStdin() throws IOException {
        // System.out.print("Reading input... ");
        var sj = new StringJoiner(System.lineSeparator());
        try (var br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) {
                sj.add(line);
            }
        }/*  catch (IOException e) {
            e.printStackTrace();
        } */

        // System.out.println(" Done.");
        return sj.toString();
    }

    private JsonElement getData() {
        var params = IOUtils.parseArgs(getParameters().getRaw());
        String dataJson = null;
        
        try {
            if (!params.containsKey("in")) {
                dataJson = getSampleData();
            }
            else if (params.get("in").equals("-")) {
                dataJson = readFromStdin();
            }
            else if (!params.get("in").contains(";")) {
                dataJson = Files.readString(Path.of(params.get("in")), Charset.forName("UTF-8"));
            } 
            else if (params.get("in").contains(";")) {
                var gsonObj = new JsonObject();
                int i = 0;
                for (String filePath : params.get("in").split(";")) {
                    String arrJsonStr = Files.readString(Path.of(filePath), Charset.forName("UTF-8"));
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
    private static void printHelp() {
        System.out.println(String.join(System.lineSeparator(),
        "Display given json as a table. Can display multiple tables in separate tabs. Can expand array of objects into subcolumns.",
        "",
        "--in <path...>        # Path to json file or '-' for stdin. Json should be an array or map of arrays. Can provide multiple files for this key in separate args or in single arg delimited with a semicolon.",
        "--pass-thru        # Window will have 'Submit' button, clicking it will print sequence numbers of selected items and exit. If input wasn't a plain array, will print items."
        ));
    }

    public static void main(String[] args) {
        if (Set.of(args).contains("--help")) {
            printHelp();
            System.exit(0);
        }
        launch(args);
    }
}