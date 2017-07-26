package com.application;

import com.application.db.DAOImplementation.*;
import com.application.db.DatabaseUtil;
import com.application.db.TableNames;
import com.application.fxgraph.ElementHelpers.ConvertDBtoElementTree;
import com.application.fxgraph.ElementHelpers.Element;
import com.application.fxgraph.cells.CircleCell;
import com.application.fxgraph.graph.*;
import com.application.logs.fileHandler.CallTraceLogFile;
import com.application.logs.fileHandler.MethodDefinitionLogFile;
import com.application.logs.fileIntegrity.CheckFileIntegrity;
import com.application.logs.parsers.ParseCallTrace;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {

    // Main UI screen
    private Graph graph;
    Model model;
    private BorderPane root;
    private ConvertDBtoElementTree convertDBtoElementTree;
    private Stage primaryStage;

    // Information panel.
    private boolean methodDefnFileSet;
    private boolean callTraceFileSet;

    private Glyph methodDefnInfoGlyph;
    private String methodDefnInfoString = "Select Method Definition log file.";
    private Label methodDefnInfoLabel;

    private Glyph callTraceInfoGlyph;
    private String callTraceInfoString = "Select Call Trace log file.";
    private Label callTraceInfoLabel;

    private Glyph runInfoGlyph;

    private FlowPane instructionsNode;

    // Menu bar
    private MenuBar menuBar;

    private Menu fileMenu; // File menu button
    private MenuItem chooseMethodDefnMenuItem;
    private MenuItem chooseCallTraceMenuItem;
    private Glyph methodDefnGlyph;
    private Glyph callTraceGlyph;

    private Menu runMenu;  // Run menu button
    private MenuItem runAnalysisMenuItem;
    private Glyph runAnalysisGlyph;
    private MenuItem resetMenuItem;
    private Glyph resetGlyph;

    private Menu saveImgMenu;  // Save Image menu button
    private MenuItem saveImgMenuItem;
    private Glyph saveImgGlyph;

    private Menu goToMenu;
    private Menu recentMenu;
    private Glyph recentsGlyph;
    private MenuItem clearHistoryMenuItem;
    private Glyph clearHistoryGlyph;

    private Menu highlight;
    private MenuItem highlightMeneItem;
    private Glyph highlightItemsGlyph;

    // Status bar
    private Group statusBar;
    private Label statusBarLabel = new Label();

    // Thread list panel on left.
    static ListView<String> threadListView;
    ObservableList<String> threadsObsList;

    // Progress bar
    Stage pStage;
    Scene pScene;
    ProgressBar progressBar;
    VBox pVBox;
    Label title;
    Label progressText;
    private static final int PROGRESS_BAR_WIDTH = 676;

    // Background tasks
    Task task;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // graph = new Graph();
        root = new BorderPane();
        EventHandlers.saveRef(this);

        // Original.
        // addGraphComponents();
        // Layout layout = new RandomLayout(graph);
        // layout.execute();

        System.out.println("Max memory: " + Runtime.getRuntime().maxMemory() / 1000000);
        System.out.println("Free memory: " + Runtime.getRuntime().freeMemory() / 1000000);
        System.out.println("Total memory: " + Runtime.getRuntime().totalMemory() / 1000000);

        // Create Menu Bar
        setUpMenu();

        // Create Status Bar
        // setUpStatusBar();

        // Create a navigation bar. Future release
        // setUpNavigationBar();

        Scene scene = new Scene(root, 1000, 300);

        // URL url = getClass().getClassLoader().getResource("css/application.css");
        // String css = url.toExternalForm();
        // scene.getStylesheets().add(css);
        // scene.getStylesheets().add(getClass().getResource("css/application.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Call Stack Visualization");
        primaryStage.show();
    }

    private void setUpMenu() {
        List<Glyph> glyphs = new ArrayList<>();
        List<MenuItem> menuItems = new ArrayList<>();

        menuBar = new MenuBar();
        menuBar.setStyle(SizeProp.PADDING_MENU);

        // *****************
        // File Menu
        // *****************

        fileMenu = new Menu("File");
        methodDefnGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.PLUS);
        methodDefnGlyph.setColor(ColorProp.ENABLED);
        // methodDefnGlyph.setPadding(SizeProp.INSETS_ICONS);
        chooseMethodDefnMenuItem = new MenuItem("Select Method Definition log file", methodDefnGlyph);
        // chooseMethodDefnMenuItem.setStyle(SizeProp.PADDING_SUBMENU);

        String font = "FontAwesome";
        callTraceGlyph = new Glyph(font, FontAwesome.Glyph.PLUS);
        callTraceGlyph.setColor(Color.DIMGRAY);
        // callTraceGlyph.setPadding(new Insets(2, 2, 2, 2));
        chooseCallTraceMenuItem = new MenuItem("Select Call Trace log file", callTraceGlyph);
        // chooseCallTraceMenuItem.setStyle(SizeProp.PADDING_SUBMENU);

        fileMenu.getItems().addAll(chooseMethodDefnMenuItem, chooseCallTraceMenuItem);
        menuItems.add(chooseMethodDefnMenuItem);
        menuItems.add(chooseCallTraceMenuItem);

        // *****************
        // Run Menu
        // *****************
        runMenu = new Menu("Run");

        resetGlyph = new Glyph(font, FontAwesome.Glyph.RETWEET);
        resetGlyph.setColor(ColorProp.ENABLED);
        resetMenuItem = new MenuItem("Reset", resetGlyph);
        // resetMenuItem.setStyle(SizeProp.PADDING_SUBMENU);

        runAnalysisGlyph = new Glyph(font, FontAwesome.Glyph.PLAY);
        runAnalysisGlyph.setColor(ColorProp.DISABLED);
        runAnalysisMenuItem = new MenuItem("Run", runAnalysisGlyph);
        // runAnalysisMenuItem.setStyle(SizeProp.PADDING_SUBMENU);
        runAnalysisMenuItem.setDisable(true);

        runMenu.getItems().addAll(runAnalysisMenuItem, resetMenuItem);
        menuItems.add(runAnalysisMenuItem);
        menuItems.add(resetMenuItem);


        // *****************
        // Save Image Menu
        // *****************
        saveImgMenu = new Menu("Save Image");
        saveImgGlyph = new Glyph(font, FontAwesome.Glyph.PICTURE_ALT);
        saveImgGlyph.setColor(ColorProp.ENABLED);
        saveImgMenuItem = new MenuItem("Save Image", saveImgGlyph);

        saveImgMenu.getItems().add(saveImgMenuItem);
        saveImgMenu.setDisable(true);
        menuItems.add(saveImgMenuItem);


        // *****************
        // Go To Menu
        // *****************
        goToMenu = new Menu("Go To");
        recentsGlyph = new Glyph(font, FontAwesome.Glyph.HISTORY);
        recentsGlyph.setColor(ColorProp.ENABLED);
        recentMenu = new Menu("Recent nodes", recentsGlyph);
        recentMenu.setStyle(SizeProp.PADDING_SUBMENU);

        clearHistoryGlyph = new Glyph(font, FontAwesome.Glyph.TRASH);
        clearHistoryGlyph.setColor(ColorProp.ENABLED);
        clearHistoryMenuItem = new MenuItem("Clear history", clearHistoryGlyph);

        goToMenu.getItems().addAll(recentMenu, clearHistoryMenuItem);
        goToMenu.setDisable(true);
        menuItems.add(clearHistoryMenuItem);

        // Highlight method invocations menu.
        highlight = new Menu("Highlights");
        highlightItemsGlyph = new Glyph(font, FontAwesome.Glyph.FLAG);
        highlightItemsGlyph.setColor(ColorProp.ENABLED);
        highlightMeneItem = new MenuItem("Highlight method invocations", highlightItemsGlyph);

        highlight.getItems().add(highlightMeneItem);
        highlight.setDisable(true);
        menuItems.add(highlightMeneItem);

        // Main menu
        menuBar.getMenus().addAll(fileMenu, runMenu, saveImgMenu, goToMenu, highlight);
        glyphs.addAll(Arrays.asList(methodDefnGlyph, callTraceGlyph, resetGlyph, runAnalysisGlyph, saveImgGlyph, recentsGlyph, clearHistoryGlyph, highlightItemsGlyph));

        menuItems.forEach(menuItem -> menuItem.setStyle(SizeProp.PADDING_SUBMENU));
        glyphs.forEach(glyph -> glyph.setStyle(SizeProp.PADDING_ICONS));

        populateInstructions();
        setMenuActions();

        root.setTop(menuBar);
    }

    private void setMenuActions() {
        chooseMethodDefnMenuItem.setOnAction(event -> {
//            DatabaseUtil.resetDB();
            File methodDefnFile = chooseLogFile("MethodDefinition");
            if (methodDefnFile != null) {

                // Menu buttons related
                MethodDefinitionLogFile.setFile(methodDefnFile);
                methodDefnGlyph.setIcon(FontAwesome.Glyph.CHECK);
                methodDefnInfoLabel.setText(methodDefnInfoString + "  File selected : " + methodDefnFile.getName());

                // Change icons and colors in instructions panel
                methodDefnInfoGlyph.setIcon(FontAwesome.Glyph.CHECK);
                methodDefnInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);
                callTraceInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);

                changeBool("methodDefnFileSet", true);
            }
        });

        chooseCallTraceMenuItem.setOnAction(event -> {
//            DatabaseUtil.resetDB();
            File callTraceFile = chooseLogFile("CallTrace");
            if (callTraceFile != null) {
                // Menu buttons related
                CallTraceLogFile.setFile(callTraceFile);
                callTraceGlyph.setIcon(FontAwesome.Glyph.CHECK);
                callTraceInfoLabel.setText(callTraceInfoString + "  File selected : " + callTraceFile.getName());

                // Change icons and colors in instructions panel
                methodDefnInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);
                callTraceInfoGlyph.setIcon(FontAwesome.Glyph.CHECK);
                callTraceInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);

                changeBool("callTraceFileSet", true);
            }
        });

        runAnalysisMenuItem.setOnAction(event -> {
            setUpProgressBar();
            reload();

            // Change icons and colors in instructions panel
            runInfoGlyph.setIcon(FontAwesome.Glyph.CHECK);
            runInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);

            saveImgMenu.setDisable(false);
            goToMenu.setDisable(false);
            highlight.setDisable(false);
        });

        resetMenuItem.setOnAction(event -> {
            reset();
            methodDefnGlyph.setIcon(FontAwesome.Glyph.PLUS);
            callTraceGlyph.setIcon(FontAwesome.Glyph.PLUS);
        });

        // Capture and save the currently loaded UI tree.
        saveImgMenuItem.setOnAction(event -> saveUIImage());


        // Populate recentMenu.
        goToMenu.setOnShowing(event -> {
            recentMenu.getItems().clear();
            graph.getRecentLocationsMap().entrySet().forEach(entry -> {
                MenuItem nodeLocation = new MenuItem();
                nodeLocation.setText(entry.getKey());
                nodeLocation.setOnAction(event1 -> {
                    String targetThreadId = String.valueOf(entry.getValue().threadId);
                    double hValue = graph.getHValue(entry.getValue().x);
                    double vValue = graph.getVValue(entry.getValue().y);

                    ConvertDBtoElementTree.resetRegions();

                    // Platform.runLater(() -> {
                        System.out.println("---------ONE show thread");
                        showThread(targetThreadId);
                        System.out.println("---------ONE END");
                    // });

                    // try {
                    //     Thread.sleep(5000);
                    // } catch (InterruptedException e) {
                    //     e.printStackTrace();
                    // }


                    // Platform.runLater(() -> {
                        System.out.println("---------TWO make selection");
                        makeSelection(targetThreadId);
                        System.out.println("---------TWO END");
                    // });


                    // try {
                    //     Thread.sleep(5000);
                    // } catch (InterruptedException e) {
                    //     e.printStackTrace();
                    // }

                    // Platform.runLater(() -> {
                        System.out.println("---------THREE move scrollpane");
                        graph.moveScrollPane(hValue, vValue);
                        System.out.println("---------THREE END");
                    // });

                });

                recentMenu.getItems().add(nodeLocation);
            });
        });

        clearHistoryMenuItem.setOnAction(event -> graph.clearRecents());

        // highlightMenuItem.setOnAction(event -> setUpMethodsWindow());
        highlightMeneItem.setOnAction(event -> setUpHighlightsWindow());
    }

    public void resetFromOutside() {
        reset();
        methodDefnGlyph.setIcon(FontAwesome.Glyph.PLUS);
        callTraceGlyph.setIcon(FontAwesome.Glyph.PLUS);
    }


    public void setUpStatusBar() {
        statusBar = new Group();
        statusBarLabel.setText("Application ready.");
        statusBar.getChildren().add(statusBarLabel);
        root.setBottom(statusBar);
    }

    private void reset() {
        root.setCenter(null);
        root.setLeft(null);
        runAnalysisMenuItem.setDisable(true);
        goToMenu.setDisable(true);
        saveImgMenu.setDisable(true);

        resetInstructionsPanel();
        resetHighlights();


        ConvertDBtoElementTree.resetRegions();
    }

    private void reload() {
        // statusBarLabel.setText("Loading. Please wait.");

        if (!methodDefnFileSet || !callTraceFileSet) {
            System.out.println("Returning without effect");
            return;
        }

        addGraphCellComponents();
    }

    private void resetCenterLayout() {
        // Layout Center
        graph = null;
        root.setCenter(null);

        graph = new Graph();
        root.setCenter(graph.getScrollPane());
        ((ZoomableScrollPane) graph.getScrollPane()).saveRef(this);
    }

    public void setUpNavigationBar() {
        HBox hBox = new HBox();
        Button goToParent = new Button("Parent");
        Button goToChildren = new Button("Children");
        Button goUpSigbling = new Button("Up Sibling");
        Button goDownSigbling = new Button("Down Sibling");

        hBox.getChildren().addAll(goToParent, goUpSigbling, goDownSigbling, goToChildren);
        root.setBottom(hBox);

        goToParent.setOnAction(event -> {
            Node node = (Node) event.getSource();
            CircleCell cell = (CircleCell) node;
            getNextElementToGO("up", cell);

            // Go To Parent
            // When an element is clicked. Highlight it.
            // when go to parent is clicked.
            // unhighlight element.
            // From element table
            // Get its element id. Get its parent id.
            // Get clicked cell's parent cell coordinates.
            // scroll to parent.
            // highlight parent.

            // Go To First child.
            // When an element is clicked. Highlight it.
            // when go to child is clicked.
            // unhighlight element.
            // From element table
            // Get its element id. Get its child's id.
            // Get clicked cell's child cell coordinates.
            // scroll to child
            // highlight parent.


        });
    }

    private void getNextElementToGO(String direction, CircleCell cell) {
        switch (direction) {
            case "up":
                // Get parent element record.
                ResultSet rs = ElementDAOImpl.selectWhere("id = (Select PARENT_ID FROM " + TableNames.ELEMENT_TABLE + " WHERE ID = " + cell.getCellId());

                try {
                    if (rs.next()) {
                        double xCord = rs.getDouble("BOUND_BOX_X_COORDINATE");
                        double yCord = rs.getDouble("BOUND_BOX_Y_COORDINATE");

                        double hValue = graph.getHValue(xCord);
                        double vValue = graph.getVValue(yCord);
                        graph.moveScrollPane(hValue, vValue);

                    }

                    break;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
    }

    private void setUpThreadsView() {
        // Layout Left
        threadsObsList = FXCollections.observableArrayList();
        threadListView = new ListView<>();
        threadListView.setItems(threadsObsList);

        threadListView.setOnMouseClicked(event -> {
            String selectedItem = threadListView.getSelectionModel().getSelectedItem();
            String threadId = selectedItem.split(" ")[1];
            ConvertDBtoElementTree.resetRegions();
            showThread(threadId);
        });

        // Get thread list and populate
        threadsObsList.clear();

        ConvertDBtoElementTree.greatGrandParent.getChildren().forEach(element -> {
            Element child = element.getChildren().get(0);
            int callTraceId = -1;
            if (child != null) callTraceId = child.getFkEnterCallTrace();
            try (ResultSet rs = CallTraceDAOImpl.selectWhere("id = " + callTraceId)) {
                if (rs.next()) {
                    int threadId = rs.getInt("thread_id");
                    threadsObsList.add("Thread: " + threadId);
                }
            } catch (SQLException ignored) {
            }
        });

        root.setLeft(threadListView);
    }

    private int imgId = 0;

    private void saveUIImage() {
        System.out.println("In saveUIImage.");
        ScrollPane scrollPane = graph.getScrollPane();
        WritableImage image = scrollPane.snapshot(new SnapshotParameters(), null);

        File file = new File("screenshot-" + imgId + ".png");
        imgId++;
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            System.out.println("saveUIImage exception");
        }
    }

    private void addGraphCellComponents() {
        convertDBtoElementTree = new ConvertDBtoElementTree();
        CheckFileIntegrity.saveRef(this);

        task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Reset Database
                updateTitle("Resetting the Database.");
                DatabaseUtil.resetDB();

                // Check fileMenu integrity.
                BytesRead bytesRead = new BytesRead(
                        0,
                        MethodDefinitionLogFile.getFile().length() + 2 * CallTraceLogFile.getFile().length()
                );
                updateTitle("Checking call trace fileMenu for errors.");
                updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
                updateProgress(bytesRead.readSoFar, bytesRead.total);
                CheckFileIntegrity.checkFile(CallTraceLogFile.getFile(), bytesRead);

                // Parse Log files.
                new ParseCallTrace().readFile(MethodDefinitionLogFile.getFile(), bytesRead, MethodDefnDAOImpl::insert);
                updateTitle("Parsing log files.");
                new ParseCallTrace().readFile(CallTraceLogFile.getFile(), bytesRead,
                        parsedLineList -> {
                            try {
                                int autoIncrementedId = CallTraceDAOImpl.insert(parsedLineList);
                                convertDBtoElementTree.StringToElementList(parsedLineList, autoIncrementedId);
                                updateMessage("Please wait... total Bytes: " + bytesRead.total + " bytes processed: " + bytesRead.readSoFar);
                                updateProgress(bytesRead.readSoFar, bytesRead.total);
                                Main.this.updateProgress(bytesRead);
                            } catch (SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {  // Todo Create a custom exception class and clean this.
                                e.printStackTrace();
                            }
                        });


                // Inserting log files into Database.
                LinesInserted linesInserted = new LinesInserted(
                        0,
                        2 * ParseCallTrace.countNumberOfLines(CallTraceLogFile.getFile())
                );

                updateTitle("Writing to DB.");
                updateMessage("Please wait... total records: " + linesInserted.total + " records processed: " + linesInserted.insertedSoFar);
                updateProgress(linesInserted.insertedSoFar, linesInserted.total);
                convertDBtoElementTree.calculateElementProperties();

                // Insert elements and properties into database
//                convertDBtoElementTree.recursivelyInsertElementsIntoDB(convertDBtoElementTree.greatGrandParent);

                Element root = ConvertDBtoElementTree.greatGrandParent;
                if (root == null)
                    return null;

                Queue<Element> queue = new LinkedList<>();
                queue.add(root);

                Element element;
                while ((element = queue.poll()) != null) {
                    ElementDAOImpl.insert(element);
                    ElementToChildDAOImpl.insert(
                            element.getParent() == null ? -1 : element.getParent().getElementId(),
                            element.getElementId());

                    if (element.getChildren() != null) {
                        element.getChildren().forEach(queue::add);
                    }

                    linesInserted.insertedSoFar++;
                    updateMessage("Please wait... total records: " + linesInserted.total + " records processed: " + linesInserted.insertedSoFar);
                    updateProgress(linesInserted.insertedSoFar, linesInserted.total);
                }

                // Insert lines and properties into database.
                convertDBtoElementTree.recursivelyInsertEdgeElementsIntoDB(ConvertDBtoElementTree.greatGrandParent);
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                // close the progress bar screen.
                pStage.close();

                // Load UI.
                postDatabaseLoad();
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        title.textProperty().bind(task.titleProperty());
        progressText.textProperty().bind(task.messageProperty());
        new Thread(task).start();
    }

    private void setUpProgressBar() {
        progressBar = new ProgressBar();
        progressBar.setPrefWidth(PROGRESS_BAR_WIDTH);

        pStage = new Stage();
        pStage.initModality(Modality.APPLICATION_MODAL);
        pStage.initOwner(null);

        title = new Label("");
        progressText = new Label("");

        pVBox = new VBox();
        pVBox.getChildren().addAll(title, progressText, progressBar);
        pVBox.setSpacing(SizeProp.SPACING);
        pVBox.setPadding(new Insets(10,5,5,5));
        pVBox.setAlignment(Pos.CENTER);
        pScene = new Scene(pVBox);
        pStage.setScene(pScene);
        pStage.setTitle("Please wait while we crunch the logs");
        pStage.show();
    }

    private void updateProgress(BytesRead bytesRead) {
    }

    private void postDatabaseLoad() {
        resetCenterLayout();
        setUpThreadsView();
        // setUpCheckTreeView();

        Graph.drawPlaceHolderLines();

        updateUi("postDatabaseLoad");

        String firstThreadID = threadsObsList.get(0).split(" ")[1];
        showThread(firstThreadID);
        threadListView.getSelectionModel().select(0);
    }

    // private void createCircleCellsRecursively(Element root, Model model) {
    //     if (root == null) {
    //         return;
    //     }
    //     createCircleCell(root, model);
    //
    //     if (root.getChildren() != null) {
    //         root.getChildren()
    //                 .forEach(ele -> createCircleCellsRecursively(ele, model));
    //     }
    // }

    // public void createCircleCell(Element root, Model model) {
    //     CircleCell targetCell = model.addCircleCell(String.valueOf(root.getElementId()), root);
    //     if (root.getParent() != null) {
    //         CircleCell sourceCell = root.getParent().getCircleCell();
    //         model.addEdge(sourceCell, targetCell);
    //     }
    // }

    // public Map<Integer, CircleCell> fromDBToUI() {
    //     Map resMap = new HashMap<Integer, CircleCell>();
    //     // Do fast
    //     // monitor scroll hvalue changes and load more circles.
    //     try {
    //         ResultSet rs = ElementDAOImpl.selectWhere("parent_id = -1");
    //         rs.next();
    //         int grandParentId = rs.getInt("id");
    //         float grandParentXCoordinate = rs.getFloat("bound_box_x_coordinate");
    //         float grandParentYCoordinate = rs.getFloat("bound_box_y_coordinate");
    //         CircleCell grandParentCell = new CircleCell(String.valueOf(grandParentId), grandParentXCoordinate, grandParentYCoordinate);
    //         model.addCell(grandParentCell);
    //
    //         rs = ElementDAOImpl.selectWhere("parent_id = " + grandParentId);
    //         while (rs.next()) {
    //             int cellId = rs.getInt("id");
    //             float cellXCoordinate = rs.getFloat("bound_box_x_coordinate");
    //             float cellYCoordinate = rs.getFloat("bound_box_y_coordinate");
    //             // For each level 1 element, draw on UI.
    //             CircleCell targetCell = new CircleCell(String.valueOf(cellId), cellXCoordinate, cellYCoordinate);
    //             model.addCell(targetCell);
    //             model.addEdge(grandParentCell, targetCell);
    //             resMap.put(cellId, targetCell);
    //         }
    //     } catch (SQLException e) {
    //         e.printStackTrace();
    //     }
    //     return resMap;
    // }

    public void showThread(String threadId) {


        // Prevent triggering listeners from modifying circleCellsOnUI, edgesOnUI and highlightsOnUI HashMaps
        ZoomableScrollPane.turnOffListeners();

        // graph.getModel().uiUpdateRequired = true;
        // System.out.println("Main::showThread: new updates show");
        convertDBtoElementTree.setCurrentThreadId(threadId);
        // System.out.println("Main::showThread: before clearUI");
        convertDBtoElementTree.clearUI();
        // System.out.println("Main::showThread: after clearUI");
        updateUi("showThread");
        // System.out.println("Main::showThread: END");

        // Prevent triggering listeners from modifying circleCellsOnUI, edgesOnUI and highlightsOnUI HashMaps
        ZoomableScrollPane.turnOnListeners();
        graph.getModel().stackRectangles("showThread");
    }

    public void updateUi(String caller) {
        if (convertDBtoElementTree != null && graph != null) {
            // System.out.println("Main::updateUi: called by " + caller + " thread " + Thread.currentThread().getName());
            convertDBtoElementTree.loadUIComponentsInsideVisibleViewPort(graph);
            convertDBtoElementTree.removeUIComponentsFromInvisibleViewPort(graph);
            // graph.myEndUpdate();
            graph.updateCellLayer();
        }
        // System.out.println("Main::updateUi: END called by " + caller);
    }

    // private void addGraphComponents() {
    //
    //     Model model = graph.getModel();
    //
    //     graph.beginUpdate();
    //
    //     //        model.addCell("Cell A", CellType.RECTANGLE);
    //     //        model.addCell("Cell B", CellType.RECTANGLE);
    //     //        model.addCell("Cell C", CellType.RECTANGLE);
    //     //        model.addCell("Cell D", CellType.TRIANGLE);
    //     //        model.addCell("Cell E", CellType.TRIANGLE);
    //     //        model.addCell("Cell F", CellType.RECTANGLE);
    //     //        model.addCell("Cell G", CellType.RECTANGLE);
    //     model.addCell("Cell A", CellType.RECTANGLE);
    //     model.addCell("Cell B", CellType.RECTANGLE);
    //     model.addCell("Cell C", CellType.RECTANGLE);
    //     model.addCell("Cell D", CellType.RECTANGLE);
    //     model.addCell("Cell E", CellType.RECTANGLE);
    //     model.addCell("Cell F", CellType.RECTANGLE);
    //     model.addCell("Cell G", CellType.RECTANGLE);
    //     model.addCell("Cell H", CellType.RECTANGLE);
    //     model.addCell("Cell I", CellType.RECTANGLE);
    //     model.addCell("Cell J", CellType.RECTANGLE);
    //     model.addCell("Cell K", CellType.RECTANGLE);
    //
    //     model.addEdge("Cell A", "Cell B");
    //     model.addEdge("Cell A", "Cell C");
    //     //        model.addEdge("Cell B", "Cell C");
    //     model.addEdge("Cell C", "Cell D");
    //     model.addEdge("Cell B", "Cell E");
    //     model.addEdge("Cell D", "Cell F");
    //     model.addEdge("Cell D", "Cell G");
    //     model.addEdge("Cell G", "Cell H");
    //     model.addEdge("Cell G", "Cell I");
    //     model.addEdge("Cell G", "Cell J");
    //     model.addEdge("Cell G", "Cell K");
    //
    //     graph.endUpdate();
    // }

    public static void main(String[] args) {
        launch(args);
    }

    public static void makeSelection(String threadId) {
        // Platform.runLater(() -> threadListView.getSelectionModel().select("Thread: " + threadId));
        threadListView.getSelectionModel().select("Thread: " + threadId);
    }

    private File chooseLogFile(String logType) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        if (logType.equalsIgnoreCase("CallTrace")) {
            fileChooser.setTitle("Choose call trace log fileMenu.");
        } else {
            fileChooser.setTitle("Choose method definition log fileMenu.");
        }

        File logFile = fileChooser.showOpenDialog(primaryStage);
        if (logFile != null) {
            return logFile;
        }

        return null;
    }

    private void changeBool(String type, boolean val) {
        if (type.equalsIgnoreCase("methodDefnFileSet"))
            methodDefnFileSet = val;
        else if (type.equalsIgnoreCase("callTraceFileSet"))
            callTraceFileSet = val;

        if (methodDefnFileSet && callTraceFileSet) {
            runAnalysisMenuItem.setDisable(false);

            // Menu buttons related.
            runAnalysisGlyph.setColor(ColorProp.ENABLED);

            // Change icons and colors in instructions panel
            runInfoGlyph.setIcon(FontAwesome.Glyph.ARROW_RIGHT);
            runInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);
        }
    }

    private void resetInstructionsPanel() {
        changeBool("methodDefnFileSet", false);
        changeBool("callTraceFileSet", false);
        populateInstructions();
    }

    private void populateInstructions() {
        instructionsNode = new FlowPane();
        root.setCenter(instructionsNode);

        methodDefnInfoGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.ARROW_RIGHT);
        methodDefnInfoGlyph.setColor(ColorProp.ENABLED_COLORFUL);
        methodDefnInfoLabel = new Label(methodDefnInfoString, methodDefnInfoGlyph);

        callTraceInfoGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.ARROW_RIGHT);
        callTraceInfoGlyph.setColor(ColorProp.ENABLED);
        callTraceInfoLabel = new Label(callTraceInfoString, callTraceInfoGlyph);

        runInfoGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.ARROW_RIGHT);
        runInfoGlyph.setColor(ColorProp.DISABLED);
        String runInfoString = "Click run.";
        Label runInfoLabel = new Label(runInfoString, runInfoGlyph);

        instructionsNode.getChildren().addAll(methodDefnInfoLabel, callTraceInfoLabel, runInfoLabel);
        instructionsNode.setAlignment(Pos.CENTER);
        instructionsNode.setOrientation(Orientation.VERTICAL);
        instructionsNode.setPadding(new Insets(5, 5, 5, 5));
        instructionsNode.setVgap(10);
    }

    public class BytesRead {
        public long readSoFar = 0;
        long total = 0;

        BytesRead(long readSoFar, long totalBytes) {
            this.readSoFar = readSoFar;
            this.total = totalBytes;
        }
    }

    public class LinesInserted {
        long insertedSoFar = 0;
        long total = 0;

        LinesInserted(long insertedSoFar, long totalBytes) {
            this.insertedSoFar = insertedSoFar;
            this.total = totalBytes;
        }
    }


    private Stage mStage;
    private Button applyButton;
    private Button cancelButton;
    private VBox vBox;

    private boolean firstTimeSetUpHighlightsWindowCall = true;

    private Map<String, CheckBox> firstCBMap;
    private Map<String, CheckBox> secondCBMap;
    private Map<String, Color> colorsMap;
    private boolean anyColorChange = false;

    private void firstTimeSetUpHighlightsWindow() {
        if (!firstTimeSetUpHighlightsWindowCall)
            return;

        firstTimeSetUpHighlightsWindowCall = false;

        firstCBMap = new HashMap<>();
        secondCBMap = new HashMap<>();
        colorsMap = new HashMap<>();
        anyColorChange = false;

        GridPane gridPane = new GridPane();
        gridPane.setPadding(SizeProp.INSETS);
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setMinHeight(GridPane.USE_PREF_SIZE);
        gridPane.setAlignment(Pos.TOP_CENTER);

        Label headingCol1 = new Label("Package and method name");
        headingCol1.setWrapText(true);
        headingCol1.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol1.getFont().getSize()*1.1));
        GridPane.setConstraints(headingCol1, 0, 0);
        GridPane.setHalignment(headingCol1, HPos.CENTER);

        Label headingCol2 = new Label("Highlight method only");
        headingCol2.setWrapText(true);
        headingCol2.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol2.getFont().getSize()*1.1));
        GridPane.setConstraints(headingCol2, 1, 0);
        GridPane.setHalignment(headingCol2, HPos.CENTER);

        Label headingCol3 = new Label("Highlight subtree");
        headingCol3.setWrapText(true);
        headingCol3.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol3.getFont().getSize()*1.1));
        GridPane.setConstraints(headingCol3, 2, 0);
        GridPane.setHalignment(headingCol3, HPos.CENTER);


        Label headingCol4 = new Label("Choose color");
        headingCol4.setWrapText(true);
        headingCol4.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol4.getFont().getSize()*1.1));
        GridPane.setConstraints(headingCol4, 3,0);
        GridPane.setHalignment(headingCol4, HPos.CENTER);


        gridPane.getChildren().addAll(
                headingCol1, headingCol2, headingCol3, headingCol4
        );

        applyButton = new Button("Apply");
        applyButton.setAlignment(Pos.CENTER_RIGHT);

        cancelButton = new Button("Cancel");
        cancelButton.setAlignment(Pos.CENTER_RIGHT);

        Pane hSpacer = new Pane();
        hSpacer.setMinSize(10, 1);
        HBox.setHgrow(hSpacer, Priority.ALWAYS);


        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(gridPane);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        Pane vSpacer = new Pane();
        vSpacer.setMinSize(10, 1);
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        HBox hBox = new HBox(hSpacer, cancelButton, applyButton);
        hBox.setPadding(SizeProp.INSETS);
        hBox.setSpacing(20);
        hBox.setAlignment(Pos.BOTTOM_CENTER);

        vBox = new VBox(SizeProp.SPACING);
        vBox.setPrefHeight(VBox.USE_PREF_SIZE);
        vBox.setPadding(SizeProp.INSETS);

        vBox.getChildren().addAll(scrollPane, vSpacer, hBox);

        // For debugging purposes. Shows backgrounds in colors.
        // hBox.setStyle("-fx-background-color: #ffb85f");
        // vBox.setStyle("-fx-background-color: yellow");
        // gridPane.setStyle("-fx-background-color: #4dfff3");
        // scrollPane.setStyle("-fx-background-color: #ffb2b3");


        // mRootGroup.getChildren().add();
        Scene mScene = new Scene(vBox, 1000, 500);
        mStage = new Stage();
        mStage.setTitle("Choose highlighting options");
        mStage.setScene(mScene);

        LinkedList<String> methodNamesList = new LinkedList<>();

        // Fetch all methods from method definition fileMenu and display on UI.
        Task<Void> onStageLoad = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Fetch all methods from method definition fileMenu.
                ResultSet rs = DatabaseUtil.select("SELECT * FROM " + TableNames.METHOD_DEFINITION_TABLE);
                while (rs.next()) {
                    String methodName = rs.getString("METHOD_NAME");
                    String packageName = rs.getString("PACKAGE_NAME");
                    methodNamesList.add(packageName + "." + methodName);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                final AtomicInteger rowInd = new AtomicInteger(1);

                // Display the methods on UI
                methodNamesList.forEach(fullName -> {

                    // Label
                    Label name = new Label(fullName);
                    name.setWrapText(true);
                    // name.setMaxWidth(250);
                    GridPane.setConstraints(name, 0, rowInd.get());
                    GridPane.setHalignment(name, HPos.CENTER);
                    GridPane.setHgrow(name, Priority.ALWAYS);


                    // First checkbox
                    CheckBox firstCB = new CheckBox();
                    GridPane.setConstraints(firstCB, 1, rowInd.get());
                    GridPane.setHalignment(firstCB, HPos.CENTER);
                    GridPane.setValignment(firstCB, VPos.CENTER);
                    GridPane.setHgrow(firstCB, Priority.ALWAYS);
                    firstCB.selectedProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue) {
                            firstCBMap.put(fullName, firstCB);
                        } else {
                            firstCBMap.remove(fullName);
                        }
                    });


                    // Second checkbox
                    CheckBox secondCB = new CheckBox();
                    // secondCB.setAlignment(Pos.CENTER);
                    GridPane.setConstraints(secondCB, 2, rowInd.get());
                    GridPane.setHalignment(secondCB, HPos.CENTER);
                    GridPane.setValignment(secondCB, VPos.CENTER);
                    GridPane.setHgrow(secondCB, Priority.ALWAYS);
                    secondCB.selectedProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue) secondCBMap.put(fullName, secondCB);
                        else secondCBMap.remove(fullName);
                    });


                    // Color picker
                    ColorPicker colorPicker = new ColorPicker(Color.AQUAMARINE);
                    colorPicker.setOnAction(event -> {
                        anyColorChange = true;
                        colorsMap.put(fullName, colorPicker.getValue());
                        System.out.println(colorPicker.getValue());
                        System.out.println(colorPicker.getValue().toString());
                    });
                    colorPicker.getStyleClass().add("button");
                    colorPicker.setStyle(
                            "-fx-color-label-visible: false; " +
                                    "-fx-background-radius: 15 15 15 15;");
                    GridPane.setConstraints(colorPicker, 3, rowInd.get());
                    GridPane.setHalignment(colorPicker, HPos.CENTER);
                    GridPane.setValignment(colorPicker, VPos.CENTER);
                    GridPane.setHgrow(colorPicker, Priority.ALWAYS);

                    // For debugging.
                    // Pane colorPane = new Pane();
                    // colorPane.setStyle("-fx-background-color: #99ff85");
                    // GridPane.setConstraints(colorPane, 2, rowInd.get());
                    // Pane colorPane2 = new Pane();
                    // colorPane2.setStyle("-fx-background-color: #e383ff");
                    // GridPane.setConstraints(colorPane2, 1, rowInd.get());
                    // gridPane.getChildren().addAll(name, colorPane2, firstCB, colorPane, secondCB, colorPicker);

                    rowInd.incrementAndGet();

                    // Put every thing together
                    gridPane.getChildren().addAll(name, firstCB, secondCB, colorPicker);

                });
            }
        };

        new Thread(onStageLoad).start();

    }

    private void setUpHighlightsWindow() {

        if (firstTimeSetUpHighlightsWindowCall)
            firstTimeSetUpHighlightsWindow();

        mStage.show();

        /*
        On Apply button click behaviour.
        For each of the selected methods, insert the bound box properties into Highlights table if not already present.
        */
        Task<Void> taskOnApply = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (!HighlightDAOImpl.isTableCreated()) {
                    HighlightDAOImpl.createTable();
                }

                // For each of the selected methods, insert the bound box properties into Highlights table if not already present.
                Statement statement = DatabaseUtil.getConnection().createStatement();
                firstCBMap.forEach((fullName, checkBox) -> addInsertQueryToStatement(fullName, statement, "SINGLE"));

                secondCBMap.forEach((fullName, checkBox) -> addInsertQueryToStatement(fullName, statement, "FULL"));

                // Delete records from HIGHLIGHT_ELEMENT if that method is not checked in the stage.
                StringJoiner firstSJ = new StringJoiner("','", "'", "'");
                firstCBMap.forEach((fullName, checkBox) -> firstSJ.add(fullName));
                addDeleteQueryToStatement(firstSJ.toString(), statement, "SINGLE");

                StringJoiner secondSJ = new StringJoiner("','", "'", "'");
                secondCBMap.forEach((fullName, checkBox) -> secondSJ.add(fullName));
                addDeleteQueryToStatement(secondSJ.toString(), statement, "FULL");

                updateColors(statement);

                statement.executeBatch();

                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                // System.out.println("Main::setUpHighlightsWindow::succeeded");
                graph.getModel().uiUpdateRequired = true;
                updateUi("taskOnApply");

                // Stack highlights so that the larger ones are behind smaller ones.
                graph.getModel().stackRectangles("succeeded");
            }
        };

        applyButton.setOnAction(event -> {
            new Thread(taskOnApply).start();
            mStage.close();
        });

        cancelButton.setOnAction(event -> mStage.close());
    }

    private void addInsertQueryToStatement(String fullName, Statement statement, String highlightType) {

        String[] arr = fullName.split("\\.");
        String methodName = arr[arr.length - 1];
        String packageName = fullName.substring(0, fullName.length() - methodName.length() - 1);

        String sqlSingle = "INSERT INTO " + TableNames.HIGHLIGHT_ELEMENT + " " +
                "(METHOD_ID, THREAD_ID, HIGHLIGHT_TYPE, START_X, START_Y, WIDTH, HEIGHT, COLOR) " +

                "SELECT " +

                // METHOD_ID
                TableNames.METHOD_DEFINITION_TABLE + ".ID, " +

                // THREAD_ID
                TableNames.CALL_TRACE_TABLE + ".THREAD_ID, " +

                // HIGHLIGHT_TYPE
                "'" + highlightType + "', " +

                // START_X
                TableNames.ELEMENT_TABLE + ".BOUND_BOX_X_COORDINATE - 15 , " +

                // START_X
                TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_COORDINATE - 15, " +

                // WIDTH
                (BoundBox.unitWidthFactor - 15) + ", " +

                // HEIGHT
                (BoundBox.unitHeightFactor - 15) + ", " +
                // "(" + TableNames.ELEMENT_TABLE + ".BOUND_BOX_X_TOP_RIGHT - " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_X_TOP_LEFT), " +
                // "(" + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_BOTTOM_LEFT - " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_TOP_LEFT), " +

                // HIGHLIGHT COLOR
                "'" + colorsMap.getOrDefault(fullName, Color.AQUAMARINE) + "' " +

                "FROM " + TableNames.ELEMENT_TABLE + " " +
                "JOIN " + TableNames.CALL_TRACE_TABLE + " ON " + TableNames.ELEMENT_TABLE + ".ID_ENTER_CALL_TRACE = " + TableNames.CALL_TRACE_TABLE + ".ID " +
                "JOIN " + TableNames.METHOD_DEFINITION_TABLE + " ON " + TableNames.CALL_TRACE_TABLE + ".METHOD_ID = " + TableNames.METHOD_DEFINITION_TABLE + ".ID " +
                "WHERE " + TableNames.METHOD_DEFINITION_TABLE + ".METHOD_NAME = '" + methodName + "' " +
                "AND " + TableNames.METHOD_DEFINITION_TABLE + ".PACKAGE_NAME = '" + packageName + "' " +
                "AND NOT EXISTS " +
                "(SELECT * FROM " + TableNames.HIGHLIGHT_ELEMENT + " " +
                "WHERE " + TableNames.HIGHLIGHT_ELEMENT + ".METHOD_ID = " + TableNames.METHOD_DEFINITION_TABLE + ".ID " +
                "AND " + TableNames.HIGHLIGHT_ELEMENT + ".HIGHLIGHT_TYPE = '" + highlightType + "')";


        // Get thread id for the method. There can only be a single thread.
        // If method with same name was invoked by another thread, then its package name would different.
        String getThreadSQL = "SELECT thread_id " +
                "FROM " + TableNames.CALL_TRACE_TABLE + " " +
                "JOIN method_defn ON " + TableNames.CALL_TRACE_TABLE + ".method_id " +
                "= " +
                TableNames.METHOD_DEFINITION_TABLE + ".id " +
                "AND " + TableNames.METHOD_DEFINITION_TABLE + ".METHOD_NAME = '" + methodName + "' " +
                "AND " + TableNames.METHOD_DEFINITION_TABLE + ".PACKAGE_NAME = '" + packageName + "'";

        ResultSet getThreadInfoRS = DatabaseUtil.select(getThreadSQL);

        // System.out.println("get thread query:");
        // System.out.println(getThreadSQL);

        int threadId=0;
        try {
            while (getThreadInfoRS.next()) {
                threadId = getThreadInfoRS.getInt("THREAD_ID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // System.out.println("Result threadId " + threadId);

        String sqlFull = "INSERT INTO " + TableNames.HIGHLIGHT_ELEMENT + " " +
                "(METHOD_ID, THREAD_ID, HIGHLIGHT_TYPE, START_X, START_Y, WIDTH, HEIGHT, COLOR) " +
                "SELECT " +
                // METHOD_ID
                TableNames.METHOD_DEFINITION_TABLE + ".ID, " +

                // THREAD_ID
                TableNames.CALL_TRACE_TABLE + ".THREAD_ID, " +

                // HIGHLIGHT_TYPE
                "'" + highlightType + "', " +

                // START_X
                TableNames.ELEMENT_TABLE + ".BOUND_BOX_X_TOP_LEFT + 30, " +

                // START_Y
                TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_TOP_LEFT + 30, " +

                // WIDTH
                "(SELECT MAX(E1.BOUND_BOX_X_TOP_RIGHT) FROM " + TableNames.ELEMENT_TABLE + " AS E1 " +
                "JOIN " + TableNames.CALL_TRACE_TABLE + " AS CT ON E1.ID_ENTER_CALL_TRACE = CT.ID " +
                "WHERE E1.BOUND_BOX_Y_COORDINATE >= " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_TOP_LEFT " +
                "AND E1.BOUND_BOX_Y_COORDINATE <= " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_BOTTOM_LEFT " +
                "AND CT.THREAD_ID = " + threadId + ") - " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_X_TOP_LEFT - 15, " +

                // HEIGHT
                "(SELECT MAX(E1.BOUND_BOX_Y_BOTTOM_RIGHT) FROM " + TableNames.ELEMENT_TABLE + " AS E1 " +
                "JOIN " + TableNames.CALL_TRACE_TABLE + " AS CT ON E1.ID_ENTER_CALL_TRACE = CT.ID " +
                "WHERE E1.BOUND_BOX_Y_COORDINATE >= " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_TOP_LEFT " +
                "AND E1.BOUND_BOX_Y_COORDINATE <= " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_BOTTOM_LEFT " +
                "AND E1.BOUND_BOX_X_COORDINATE >= " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_X_TOP_LEFT " +
                "AND CT.THREAD_ID = " + threadId + ") - " + TableNames.ELEMENT_TABLE + ".BOUND_BOX_Y_TOP_LEFT - 15, " +

                // COLOR
                "'" + colorsMap.getOrDefault(fullName, Color.AQUAMARINE) + "' " +

                "FROM " + TableNames.ELEMENT_TABLE + " " +
                "JOIN " + TableNames.CALL_TRACE_TABLE + " ON " + TableNames.ELEMENT_TABLE + ".ID_ENTER_CALL_TRACE = " + TableNames.CALL_TRACE_TABLE + ".ID " +
                "JOIN " + TableNames.METHOD_DEFINITION_TABLE + " ON " + TableNames.CALL_TRACE_TABLE + ".METHOD_ID = " + TableNames.METHOD_DEFINITION_TABLE + ".ID " +
                "WHERE " + TableNames.METHOD_DEFINITION_TABLE + ".METHOD_NAME = '" + methodName + "' " +
                "AND " + TableNames.METHOD_DEFINITION_TABLE + ".PACKAGE_NAME = '" + packageName + "' " +
                "AND NOT EXISTS " +
                "(SELECT * FROM " + TableNames.HIGHLIGHT_ELEMENT + " " +
                "WHERE " + TableNames.HIGHLIGHT_ELEMENT + ".METHOD_ID = " + TableNames.METHOD_DEFINITION_TABLE + ".ID " +
                "AND " + TableNames.HIGHLIGHT_ELEMENT + ".HIGHLIGHT_TYPE = '" + highlightType + "')";

        String sql = highlightType.equalsIgnoreCase("SINGLE") ? sqlSingle : sqlFull;

        // System.out.println("-------------");
        // System.out.println(sql);
        try {
            statement.addBatch(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addDeleteQueryToStatement(String fullNames, Statement statement, String highlightType) {
        String sql = "DELETE FROM " + TableNames.HIGHLIGHT_ELEMENT + " " +
                "WHERE HIGHLIGHT_TYPE = '" + highlightType + "' AND METHOD_ID NOT IN " +
                "(SELECT ID FROM " + TableNames.METHOD_DEFINITION_TABLE + " " +
                "WHERE (" + TableNames.METHOD_DEFINITION_TABLE + ".PACKAGE_NAME || '.' || " + TableNames.METHOD_DEFINITION_TABLE + ".METHOD_NAME) " +
                "IN (" + fullNames + "))";

        // System.out.println("-------------");
        // System.out.println(sql);

        try {
            statement.addBatch(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateColors(Statement statement) {
        if (!anyColorChange)
            return;

        anyColorChange = false;

        colorsMap.forEach((fullName, color) -> {
            String sql = "UPDATE " + TableNames.HIGHLIGHT_ELEMENT + " SET COLOR = '" + color + "' " +
                    "WHERE METHOD_ID = (SELECT ID FROM " + TableNames.METHOD_DEFINITION_TABLE + " "+
                    "WHERE PACKAGE_NAME || '.' || METHOD_NAME = '" + fullName + "')";
            try {
                statement.addBatch(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void resetHighlights() {
        // firstTimeSetUpMethodsWindowCall = true;
        firstTimeSetUpHighlightsWindowCall = true;
        highlight.setDisable(true);
    }

}