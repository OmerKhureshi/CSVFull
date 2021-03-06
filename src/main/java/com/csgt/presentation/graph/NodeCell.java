package com.csgt.presentation.graph;

import com.csgt.controller.ControllerLoader;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

/**
 * This class represents the nodes in the graph on UI.
 */
public class NodeCell extends Cell {

    private Label label;
    private StackPane idBubble;
    private Label methodNameLabel;
    private Shape nodeShape;

    private StackPane minMaxStackPane;
    Glyph minMaxGlyph;

    private StackPane infoStackPane;

    private double bookmarkStrokeWidth = 3;
    // private double firstPortion = 0.7;
    // private double secondPortion = 1 - firstPortion;

    private double rectWidth = 100;
    private double rectHeight = 35;

    private Rectangle bookmarkBar;

    private boolean collapsed = false;

    // Color smallButtonsColor = Color.valueOf("#e5a2d0");
    Color minMaxButtonColor = Color.valueOf("#FF4C4C");
    Color infoButtonColor = Color.valueOf("#262626");
    // Color infoButtonColor = Color.valueOf("#001f3f");
    Color idBubbleBackgroundColor = Color.valueOf("#f6dfef");
    Color cell1Color = Color.valueOf("#b2baf0");
    Color cell2Color = Color.valueOf("#bab2f0");
    Color cellShadowColor = Color.rgb(96, 112, 224, .47);
    Color idBubbleShadowColor = Color.rgb(106,30,83, 0.30);

    public NodeCell(String id) {
        super(id);
    }

    public NodeCell(String id, float xCoordinate, float yCoordinate, int collapsed) {
        this(id);
        boolean isLite = ControllerLoader.menuController.isLiteModeEnabled;

        // Uncomment to see a colored background on the whole circle cell stack pane.
        // setStyle("-fx-background-smallButtonsColor: mediumslateblue");

        nodeShape = createRectangle();

        this.collapsed = collapsed != 0;
        label = new Label("");

        setUpDropShadow(isLite);
        setUpIdLabel(id, isLite);
        setUpMethodName();
        setUpButtons();
        setFill(isLite);
        setUpBookmark();

        getChildren().addAll(nodeShape, methodNameLabel, idBubble, minMaxStackPane, infoStackPane, bookmarkBar);

        idBubble.toFront();
        bookmarkBar.toBack();
        this.toFront();
        // guess is, drop shadows block the mouse events. set pick on bounds to false to make mouse events work.
        this.setPickOnBounds(false);

        this.setOnMouseEntered(event -> {
            if (!ControllerLoader.eventHandlers.popOver.isShowing()) {
                infoStackPane.setVisible(true);
                minMaxStackPane.setVisible(true);
            }
        });

        this.setOnMouseExited(event -> {
            if (!ControllerLoader.eventHandlers.popOver.isShowing()) {
                infoStackPane.setVisible(false);
                minMaxStackPane.setVisible(false);
            }
        });

        this.relocate(xCoordinate , yCoordinate);
    }

    private void setUpBookmark() {
        bookmarkBar = new Rectangle(rectWidth - 2, 30);
        bookmarkBar.relocate(1, -3);
        bookmarkBar.setFill(Color.TRANSPARENT);
        bookmarkBar.setStroke(Color.TRANSPARENT);
        bookmarkBar.setStrokeWidth(0.1);
        bookmarkBar.setArcWidth(20);
        bookmarkBar.setArcHeight(20);
    }

    private void setUpButtons() {
        setUpMinMaxButton();
        setUpInfoButton();
    }

    private void setUpMinMaxButton() {
        setMinMaxIcon();

        minMaxStackPane = new StackPane(minMaxGlyph);
        minMaxStackPane.relocate(rectWidth - 6, -8);
        minMaxStackPane.setVisible(false);
    }

    private void setUpInfoButton() {
        Glyph infoGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.INFO_CIRCLE);
        infoGlyph.setColor(infoButtonColor);

        infoStackPane = new StackPane(infoGlyph);
        infoStackPane.relocate(rectWidth - 6, (rectHeight - 8));
        infoStackPane.setVisible(false);
    }


    public NodeCell(String id, float xCoordinate, float yCoordinate, String methodName, int collapsed) {
        this(id, xCoordinate, yCoordinate, collapsed);
        this.methodNameLabel.setText(methodName);
    }

    public void setCollapsed(int collapsed) {
        this.collapsed = collapsed != 0;
        setMinMaxIcon();
    }

    private void setMinMaxIcon() {
        if (minMaxGlyph == null) {
            minMaxGlyph = new Glyph("FontAwesome", FontAwesome.Glyph.MINUS_SQUARE);
        }

        if (collapsed) {
            minMaxGlyph.setIcon(FontAwesome.Glyph.PLUS_SQUARE);
        } else {
            minMaxGlyph.setIcon(FontAwesome.Glyph.MINUS_SQUARE);
        }

        minMaxGlyph.setColor(minMaxButtonColor);
    }

    public void setLabel(String text) {
        this.label.setText(text);
    }

    private void setUpMethodName() {
        methodNameLabel = new Label("");
        methodNameLabel.setPrefWidth(85);
        methodNameLabel.setWrapText(true);
        methodNameLabel.setTextFill(Color.BLACK);
        // methodNameLabel.setStyle("-fx-background-smallButtonsColor: papayawhip; -fx-background-radius: 7; -fx-border-smallButtonsColor: burlywood; -fx-border-radius: 7; -fx-border-width: 2");
        methodNameLabel.setAlignment(Pos.CENTER);
        methodNameLabel.setTextAlignment(TextAlignment.CENTER);
        methodNameLabel.setFont(Font.font(12));
        methodNameLabel.setMaxWidth(((Rectangle) nodeShape).getWidth() - 5);
        methodNameLabel.setMinWidth(((Rectangle) nodeShape).getWidth() - 5);
        // methodNameLabel.setMaxHeight(((Rectangle) nodeShape).getHeight() * firstPortion);
        // methodNameLabel.setMinHeight(((Rectangle) nodeShape).getHeight() * firstPortion);
        methodNameLabel.setMaxHeight(((Rectangle) nodeShape).getHeight());
        methodNameLabel.setMinHeight(((Rectangle) nodeShape).getHeight());

        // align the method name to top of the square.
        methodNameLabel.relocate(2.5, 0);//-this.methodNameLabel.getMinHeight()/2);
        // Center the method name label below the circle.
        // this.methodNameLabel.setMinWidth(this.methodNameLabel.getText().length()*2);
        // this.methodNameLabel.relocate(-this.methodNameLabel.getPrefWidth() * .25, 45);//-this.methodNameLabel.getMinHeight()/2);

    }

    public void setMethodNameLabel(String methodName) {
        this.methodNameLabel.setText(methodName);
    }

    public StackPane getMinMaxStackPane() {
        return minMaxStackPane;
    }

    public StackPane getInfoStackPane() {
        return infoStackPane;
    }

    public void bookmarkCell(String color) {
        bookmarkBar.setFill(Paint.valueOf(color));
        bookmarkBar.setStroke(Color.BLACK);
        bookmarkBar.setStrokeWidth(0.1);
    }

    public void removeBookmark() {
        bookmarkBar.setFill(Color.TRANSPARENT);
        bookmarkBar.setStroke(Color.TRANSPARENT);
    }

    @Override
    public String toString() {
        return "NodeCell: id: " + getCellId() + "; x: " + getLayoutX() + "; y: " + getLayoutY();
    }

    private Shape createCircle() {
        Shape circle = new Circle();
        circle = new Circle(20);
        circle.setStroke(Color.BLACK);
        circle.setFill(Color.WHITE);
        circle.relocate(0,0);

        return circle;
    }

    private Shape createRectangle() {
        Shape rect = new Rectangle(rectWidth, rectHeight);
        ((Rectangle) rect).setArcWidth(20);
        ((Rectangle) rect).setArcHeight(20);
        rect.relocate(0,0);

        return rect;
    }

    private void setFill(boolean isLite) {
        if (isLite) {
            nodeShape.setFill(Color.WHITE);
            nodeShape.setStroke(Color.GREY);
            nodeShape.setStrokeWidth(0.5);
        } else {
            Stop[] stops = new Stop[] { new Stop(0, cell1Color), new Stop(1, cell2Color)};
            LinearGradient linearGradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
            nodeShape.setFill(linearGradient);
        }

    }

    private void setUpIdLabel(String id, boolean isLite) {
        double x = 0, y = 0;
        Label idLabel = new Label(id);
        idLabel.setFont(Font.font(10));
        idLabel.setTextFill(Color.DIMGREY);

        double height = 15;
        double width = idLabel.getText().length() * 6 + 6;

        Shape background = new Rectangle();
        ((Rectangle) background).setWidth(width);
        ((Rectangle) background).setHeight(height);
        ((Rectangle) background).setArcHeight(10);
        ((Rectangle) background).setArcWidth(10);
            background.setFill(idBubbleBackgroundColor);

        if (isLite) {
            // background.setFill(Color.WHITE);
            // background.setStroke(ColorProp.GREY);
            // background.setStrokeWidth(.5);
        } else {
            DropShadow dropShadow = new DropShadow();
            dropShadow.setWidth(width);
            dropShadow.setHeight(width);
            dropShadow.setOffsetX(2);
            dropShadow.setOffsetY(2);
            dropShadow.setRadius(5);
            dropShadow.setColor(idBubbleShadowColor);
            background.setEffect(dropShadow);
        }

        // background.setStroke(CustomColors.DARK_GREY.getPaint());

        idBubble = new StackPane();
        idBubble.getChildren().addAll(background, idLabel);
        idBubble.relocate(-((Rectangle) background).getWidth() * 0.5, -((Rectangle) background).getHeight() * 0.5);
    }

    private void setUpDropShadow(boolean isLite) {
        if (!isLite) {
            DropShadow dropShadow = new DropShadow();
            dropShadow.setWidth(rectWidth);
            dropShadow.setHeight(rectHeight);
            dropShadow.setOffsetX(12);
            dropShadow.setOffsetY(12);
            dropShadow.setRadius(40);
            dropShadow.setColor(cellShadowColor);
            this.setEffect(dropShadow);
        }
    }

    public void blink() {
        System.out.println("NodeCell.blink");
        double duration = 10;
        double min = nodeShape.getStrokeWidth();
        double max = 4;
        final boolean[] grow = {true};
        nodeShape.setStroke(Color.RED);

        Platform.runLater(() -> {
            final int[] noOfPluses = {3 * 2};

            Timeline timeline = new Timeline();
            KeyFrame pulseNode = new KeyFrame(Duration.millis(duration),
                    new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            double strokeWidth = nodeShape.getStrokeWidth();

                            if (noOfPluses[0] == 0) {
                                timeline.stop();
                                return;
                            }

                            if (grow[0]) {
                                strokeWidth = strokeWidth + 0.1;
                                if (strokeWidth > max) {
                                    grow[0] = false;
                                    noOfPluses[0]--;
                                }
                            } else {
                                strokeWidth = strokeWidth - 0.1;
                                if (strokeWidth < min) {
                                    grow[0] = true;
                                    noOfPluses[0]--;
                                }
                            }

                            nodeShape.setStrokeWidth(strokeWidth);
                        }
                    });

            timeline.getKeyFrames().add(pulseNode);
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();

            timeline.setOnFinished(event -> {
                nodeShape.setStroke(Color.GREY);
                nodeShape.setStrokeWidth(0.5);
            });
        });

    }

}