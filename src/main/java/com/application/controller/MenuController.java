package com.application.controller;

import com.application.db.DAO.DAOImplementation.BookmarksDAOImpl;
import com.application.db.DAO.DAOImplementation.HighlightDAOImpl;
import com.application.db.DAO.DAOImplementation.MethodDefDAOImpl;
import com.application.db.DTO.BookmarkDTO;
import com.application.db.DatabaseUtil;
import com.application.db.TableNames;
import com.application.fxgraph.graph.ColorProp;
import com.application.fxgraph.graph.SizeProp;
import com.application.presentation.CustomProgressBar;
import com.application.service.files.FileNames;
import com.application.service.files.LoadedFiles;
import com.application.service.tasks.ConstructTreeTask;
import com.application.service.tasks.ParseFileTask;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MenuController {
    // Information panel.
    private boolean methodDefnFileSet;
    private boolean callTraceFileSet;

    private Glyph methodDefnInfoGlyph;
    private String methodDefnInfoString = "Select Method Definition log file.";
    private Label methodDefnInfoLabel;

    private Glyph callTraceInfoGlyph;
    private String callTraceInfoString = "Select Call Trace log file.";
    private Label callTraceInfoLabel;

    private Glyph dbInfoGlyph;
    private String dbInfoString = "Select database to load.";
    private Label dbInfoLabel;

    private Glyph runInfoGlyph;

    private FlowPane instructionsNode;

    // Menu bar
    @FXML
    private MenuBar menuBar;

    // File Menu
    @FXML
    private MenuItem chooseMethodDefMenuItem;
    @FXML
    private MenuItem chooseCallTraceMenuItem;
    @FXML
    private MenuItem openDBMenuItem;
    private Glyph methodDefnGlyph;
    private Glyph callTraceGlyph;
    private Glyph openDBGlyph;

    // Run Menu
    @FXML
    private MenuItem runAnalysisMenuItem;
    @FXML
    private MenuItem resetMenuItem;

    private Glyph runAnalysisGlyph;
    private Glyph resetGlyph;

    // View Menu
    private Glyph saveImageGlyph;
    private Glyph refreshGlyph;

    @FXML
    private Menu viewMenu;

    @FXML
    private MenuItem saveImageMenuItem;

    @FXML
    private MenuItem refreshMenuItem;

    // Highlights Menu
    private Glyph highlightItemsGlyph;

    @FXML
    private MenuItem highlightMenu;
    @FXML
    private MenuItem addHighlightMenuItem;


    // Debug menu button
    @FXML
    private MenuItem printViewPortDimsMenuItem;


    // Bookmarks menu button
    private Glyph bookmarksGlyph;

    @FXML
    private Menu bookmarksMenu;

    private MainController mainController;

    /**
     * app start - file enabled. all else disabled.
     * select file - run -> reset enabled. all else disabled.
     * both files selected - > run -> run analysis enabled. all else disabled.
     * run analysis -> enable everything.
     */
    @FXML
    private void initialize() {
        // autoRun();
        setUpFileMenu();
        setUpRunMenu();
        setUpBookmarksMenu();
        setUpHighlightsMenu();
        setUpDebugMenu();

        ControllerLoader.register(this);
    }

    private void setUpHighlightsMenu() {
        addHighlightMenuItem.setOnAction(event -> showHighlightsWindow());
    }



    private void autoRun() {
        setFiles();
    }

    private void setFiles() {
        File methodDefLogFile = new File("/Users/skhureshi/Documents/Logs/MD1.txt");
        LoadedFiles.setFile(FileNames.METHOD_DEF.getFileName(), methodDefLogFile);

        File callTraceLogFile = new File("/Users/skhureshi/Documents/Logs/CT1.txt");
        LoadedFiles.setFile(FileNames.Call_Trace.getFileName(), callTraceLogFile);

        onRun();
    }

    private void setUpFileMenu() {
        chooseMethodDefMenuItem.setOnAction(event -> {
            try {
                File methodDefLogFile = ControllerUtil.fileChooser("Choose method definition log fileMenu.", "Text Files", "*.txt");
                LoadedFiles.setFile(FileNames.METHOD_DEF.getFileName(), methodDefLogFile);
                System.out.println("MenuController.setUpFileMenu file: " + methodDefLogFile.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        chooseCallTraceMenuItem.setOnAction(event -> {
            try {
                File callTraceLogFile = ControllerUtil.fileChooser("Choose call trace log fileMenu.", "Text Files", "*.txt");
                LoadedFiles.setFile(FileNames.Call_Trace.getFileName(), callTraceLogFile);
                System.out.println("MenuController.setUpFileMenu file: " + callTraceLogFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        openDBMenuItem.setOnAction(event -> {
            try {
                File dbFile = ControllerUtil.directoryChooser("Choose an existing database.");
                LoadedFiles.setFile("db", dbFile);
                LoadedFiles.setFreshLoad(false);
                System.out.println("MenuController.setUpFileMenu db: " + dbFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setUpRunMenu() {
        runAnalysisMenuItem.setOnAction(event -> {
            onRun();
        });

        resetMenuItem.setOnAction(event -> {
            System.out.println("reset clicked.");
            this.mainController.showInstructionsPane();
        });
    }

    private void setUpDebugMenu() {
        printViewPortDimsMenuItem.setOnAction(event -> {
            System.out.println("ScrollPane viewport dimensions");
            System.out.println(ControllerLoader.canvasController.scrollPane.getViewportBounds());
            System.out.println(ControllerLoader.canvasController.getViewPortDims());
        });
    }

    private void onRun() {
        // No need to parse log file and compute graph if loading from DB.
        if (!LoadedFiles.IsFreshLoad()) {
            return;
        }

        Task<Void> parseTask = new ParseFileTask();
        Task<Void> constructTreeTask = new ConstructTreeTask();

        CustomProgressBar customProgressBar = new CustomProgressBar("", "",
                Arrays.asList(parseTask, constructTreeTask));

        ExecutorService es = Executors.newSingleThreadExecutor();
        es.submit(parseTask);
        es.submit(constructTreeTask);
        es.shutdown();

        customProgressBar.bind(parseTask);

        parseTask.setOnSucceeded((e) -> {
            customProgressBar.bind(constructTreeTask);
        });

        constructTreeTask.setOnSucceeded((e) -> {
            customProgressBar.close();
            this.mainController.loadGraphPane();
        });

    }

    private void setUpBookmarksMenu() {
        MenuItem noBookmarksMenuItem = new MenuItem("No bookmarks");
        noBookmarksMenuItem.setDisable(true);
        bookmarksMenu.getItems().add(noBookmarksMenuItem);
    }

    public void updateBookmarksMenu() {
        bookmarksMenu.getItems().clear();

        Map<String, BookmarkDTO> bookmarkDTOs = getBookmarkDTOs();
        MenuItem noBookmarksMenuItem = new MenuItem("No bookmarks");

        System.out.println("MenuController.updateBookmarksMenu: bookmarkDTOs size: " + bookmarkDTOs.size());
        if (bookmarkDTOs.size() == 0) {
            noBookmarksMenuItem.setDisable(true);
            bookmarksMenu.getItems().add(noBookmarksMenuItem);

            return;
        }

        bookmarkDTOs.forEach((id, bookmarkDTO) -> {
            Rectangle icon = new Rectangle(15, 15);
            icon.setFill(Color.web("#6699CC"));
            icon.setStrokeWidth(3);
            icon.setStroke(Paint.valueOf(bookmarkDTO.getColor()));
            icon.setArcWidth(3);
            icon.setArcHeight(3);

            MenuItem bookmarkMenuItem = new MenuItem(
                    " Id:" + bookmarkDTO.getElementId() +
                            "  |  Method:" + bookmarkDTO.getMethodName() +
                            "  |  Thread:" + bookmarkDTO.getThreadId(), icon);

            bookmarkMenuItem.setOnAction(event -> ControllerLoader.canvasController.jumpTo(
                    bookmarkDTO.getElementId(),
                    bookmarkDTO.getThreadId(),
                    bookmarkDTO.getCollapsed()));

            bookmarksMenu.getItems().add(bookmarkMenuItem);
        });

        SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

        bookmarksMenu.getItems().add(separatorMenuItem);

        // clear bookmarks button and logic
        Glyph clearBookmarksGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.TRASH);
        clearBookmarksGlyph.setColor(ColorProp.ENABLED);
        clearBookmarksGlyph.setDisable(bookmarkDTOs.size() == 0);

        MenuItem clearBookmarksMenuItem = new MenuItem("Delete all", clearBookmarksGlyph);

        clearBookmarksMenuItem.setOnAction(event -> {
            ControllerLoader.menuController.deleteAllBookmarks();
            bookmarksMenu.getItems().clear();
            bookmarksMenu.getItems().add(noBookmarksMenuItem);
            noBookmarksMenuItem.setDisable(true);
        });

        bookmarksMenu.getItems().add(clearBookmarksMenuItem);
    }

    void setParentController(MainController mainController) {
        this.mainController = mainController;
    }


    private Stage mStage;
    private Button applyButton;
    private Button cancelButton;
    private VBox vBox;

    private boolean firstTimeSetUpHighlightsWindowCall = true;

    public Map<String, CheckBox> firstCBMap;
    public Map<String, CheckBox> secondCBMap;
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
        headingCol1.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol1.getFont().getSize() * 1.1));
        GridPane.setConstraints(headingCol1, 0, 0);
        GridPane.setHalignment(headingCol1, HPos.CENTER);

        Label headingCol2 = new Label("Highlight method only");
        headingCol2.setWrapText(true);
        headingCol2.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol2.getFont().getSize() * 1.1));
        GridPane.setConstraints(headingCol2, 1, 0);
        GridPane.setHalignment(headingCol2, HPos.CENTER);

        Label headingCol3 = new Label("Highlight subtree");
        headingCol3.setWrapText(true);
        headingCol3.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol3.getFont().getSize() * 1.1));
        GridPane.setConstraints(headingCol3, 2, 0);
        GridPane.setHalignment(headingCol3, HPos.CENTER);


        Label headingCol4 = new Label("Choose color");
        headingCol4.setWrapText(true);
        headingCol4.setFont(Font.font("Verdana", FontWeight.BOLD, headingCol4.getFont().getSize() * 1.1));
        GridPane.setConstraints(headingCol4, 3, 0);
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
                methodNamesList.addAll(MethodDefDAOImpl.getMethodPackageString());
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
                        // System.out.println(colorPicker.getValue());
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

    private void showHighlightsWindow() {

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
                System.out.println("MenuController.showHighlightsWindow.succeeded: ");
                ControllerLoader.canvasController.clearAndUpdate();

                // Stack highlights so that the larger ones are behind smaller ones.
                ControllerLoader.canvasController.stackRectangles();
            }
        };

        applyButton.setOnAction(event -> {
            new Thread(taskOnApply).start();
            mStage.close();
        });

        cancelButton.setOnAction(event -> mStage.close());
    }

    private void addInsertQueryToStatement(String fullName, Statement statement, String highlightType) {
        // System.out.println("Main.addInsertQueryToStatement: crafting query for " + fullName);
        double startXOffset = 30;
        double widthOffset = 0;
        double startYOffset = -10;
        double heightOffset = -20;

        String[] arr = fullName.split("\\.");
        String methodName = arr[arr.length - 1];
        String packageName = fullName.substring(0, fullName.length() - methodName.length() - 1);

        HighlightDAOImpl.insert(startXOffset, startYOffset, widthOffset, heightOffset, methodName, packageName, highlightType, colorsMap, fullName, statement);


        // System.out.println("Main::addInsertQueryToStatement: sql : " + sql);

    }

    private void addDeleteQueryToStatement(String fullNames, Statement statement, String highlightType) {
        String sql = "DELETE FROM " + TableNames.HIGHLIGHT_ELEMENT + " " +
                "WHERE HIGHLIGHT_TYPE = '" + highlightType + "' AND METHOD_ID NOT IN " +
                "(SELECT ID FROM " + TableNames.METHOD_DEFINITION_TABLE + " " +
                "WHERE (" + TableNames.METHOD_DEFINITION_TABLE + ".PACKAGE_NAME || '.' || " + TableNames.METHOD_DEFINITION_TABLE + ".METHOD_NAME) " +
                "IN (" + fullNames + "))";

        // System.out.println( "Main::addDeleteQueryToStatement: sql: " + sql);


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
                    "WHERE METHOD_ID = (SELECT ID FROM " + TableNames.METHOD_DEFINITION_TABLE + " " +
                    "WHERE PACKAGE_NAME || '.' || METHOD_NAME = '" + fullName + "')";
            try {
                statement.addBatch(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // private void resetHighlights() {
    //     // firstTimeSetUpMethodsWindowCall = true;
    //     firstTimeSetUpHighlightsWindowCall = true;
    //     highlightMenu.setDisable(true);
    // }

    public Map<String, BookmarkDTO> getBookmarkDTOs() {
        return BookmarksDAOImpl.getBookmarkDTOs();
    }

    public void insertBookmark(BookmarkDTO bookmarkDTO) {
        BookmarksDAOImpl.insertBookmark(bookmarkDTO);
        ControllerLoader.canvasController.addBookmarks();
        ControllerLoader.menuController.updateBookmarksMenu();

    }

    public void deleteBookmark(String elementId) {
        ControllerLoader.canvasController.removeBookmarkFromUI(elementId);
        BookmarksDAOImpl.deleteBookmark(elementId);
        ControllerLoader.menuController.updateBookmarksMenu();
    }

    public void deleteAllBookmarks() {
        ControllerLoader.canvasController.removeAllBookmarksFromUI();
        BookmarksDAOImpl.deleteBookmarks();
    }

    public void updateUIOnEvent(String eventType) {
        switch (eventType) {
            case "reset":

        }
    }

    private void setMenuUp() {
        String font = "FontAwesome";
        List<Glyph> glyphsStyling = new ArrayList<>();
        List<MenuItem> menuItemsStyling = new ArrayList<>();

        menuBar = new MenuBar();
        menuBar.setStyle(SizeProp.PADDING_MENU);

        // *****************
        // File Menu
        // *****************
        methodDefnGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.PLUS);
        methodDefnGlyph.setColor(ColorProp.ENABLED);
        chooseMethodDefMenuItem.setGraphic(methodDefnGlyph);

        callTraceGlyph = new Glyph(font, FontAwesome.Glyph.PLUS);
        callTraceGlyph.setColor(Color.DIMGRAY);
        chooseCallTraceMenuItem.setGraphic(callTraceGlyph);

        openDBGlyph = new Glyph(font, FontAwesome.Glyph.FOLDER_OPEN);
        openDBGlyph.setColor(Color.DIMGRAY);
        openDBMenuItem.setGraphic(openDBGlyph);

        SeparatorMenuItem separatorMenuItem = new SeparatorMenuItem();

        menuItemsStyling.add(chooseMethodDefMenuItem);
        menuItemsStyling.add(chooseCallTraceMenuItem);
        menuItemsStyling.add(openDBMenuItem);

        // *****************
        // Run Menu
        // *****************
        resetGlyph = new Glyph(font, FontAwesome.Glyph.RETWEET);
        resetGlyph.setColor(ColorProp.ENABLED);
        resetMenuItem.setGraphic(resetGlyph);
        // resetMenuItem.setStyle(SizeProp.PADDING_SUBMENU);

        runAnalysisGlyph = new Glyph(font, FontAwesome.Glyph.PLAY);
        runAnalysisGlyph.setColor(ColorProp.DISABLED);
        runAnalysisMenuItem.setGraphic(runAnalysisGlyph);
        // runAnalysisMenuItem.setStyle(SizeProp.PADDING_SUBMENU);
        runAnalysisMenuItem.setDisable(true);

        menuItemsStyling.add(runAnalysisMenuItem);
        menuItemsStyling.add(resetMenuItem);

        // *****************
        // View Menu
        // *****************
        viewMenu.setDisable(true);

        saveImageGlyph = new Glyph(font, FontAwesome.Glyph.PICTURE_ALT);
        saveImageGlyph.setColor(ColorProp.ENABLED);
        saveImageMenuItem.setGraphic(saveImageGlyph);

        saveImageMenuItem.setDisable(true);
        menuItemsStyling.add(saveImageMenuItem);

        refreshGlyph = new Glyph(font, FontAwesome.Glyph.REFRESH);
        refreshGlyph.setColor(ColorProp.ENABLED);
        refreshMenuItem.setGraphic(refreshGlyph);

        menuItemsStyling.add(refreshMenuItem);
        glyphsStyling.add(refreshGlyph);

        // *****************
        // Highlights Menu
        // *****************
        highlightItemsGlyph = new Glyph(font, FontAwesome.Glyph.FLAG);
        highlightItemsGlyph.setColor(ColorProp.ENABLED);
        addHighlightMenuItem = new MenuItem("Highlight method invocations", highlightItemsGlyph);

        highlightMenu.setDisable(true);
        menuItemsStyling.add(addHighlightMenuItem);

        // *****************
        // Bookmarks Menu
        // *****************
        bookmarksMenu = new Menu("Bookmarks");
        bookmarksMenu.setDisable(true);
        bookmarksGlyph = new Glyph(font, FontAwesome.Glyph.BOOKMARK);
        bookmarksGlyph.setColor(ColorProp.ENABLED);
        bookmarksSubMenu = new Menu("Bookmarks", bookmarksGlyph);


        bookmarksMenu.getItems().add(bookmarksSubMenu);
        // bookmarksMenu.setDisable(true);
        menuItemsStyling.add(bookmarksMenu);
        glyphsStyling.add(bookmarksGlyph);

        // *****************
        // Debug Menu
        // *****************
        debugMenu = new Menu("Debug");
        debugMenu.setDisable(true);
        printCellsMenuItem = new MenuItem("Print circles on canvas to console");
        printEdgesMenuItem = new MenuItem("Print edges on canvas to console");
        printBarMarksItem = new MenuItem("Print bookmark marks to console");
        printHighlightsMenuItem = new MenuItem("Print highlights on canvas to console");

        debugMenu.getItems().addAll(printCellsMenuItem, printEdgesMenuItem, printHighlightsMenuItem, printBarMarksItem);

        // *****************
        // Main Menu
        // *****************
        menuBar.getMenus().addAll(fileMenu, runMenu, viewMenu, saveImgMenu, goToMenu, bookmarksMenu, highlightMenu, debugMenu);
        glyphsStyling.addAll(Arrays.asList(methodDefnGlyph, callTraceGlyph, openDBGlyph, resetGlyph, runAnalysisGlyph,
                saveImageGlyph, recentsGlyph, clearHistoryGlyph, highlightItemsGlyph));

        menuItemsStyling.forEach(menuItem -> menuItem.setStyle(SizeProp.PADDING_SUBMENU));
        glyphsStyling.forEach(glyph -> glyph.setStyle(SizeProp.PADDING_ICONS));
        root.setTop(menuBar);
    }
}
