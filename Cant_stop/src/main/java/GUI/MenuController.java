package GUI;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.collections.FXCollections;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;

public class MenuController {

    Button StartGame;
    VBox playerSetupBox;
    Button addPlayerButton;
    Button Rules;
    Button Exit;

    private Pane root;
    private VBox gameMode;
    private VBox gameOptions;

    public MenuController() {
        menuSetup();
        hideOptions();
        setupActions();

        gameMode.setId("mode");
        gameOptions.setId("options");


        gameMode.setAlignment(Pos.CENTER);
        gameOptions.setAlignment(Pos.CENTER);

        Region centerSpacer = new Region();
        centerSpacer.setMinWidth(800);
        centerSpacer.setPrefWidth(800);
        centerSpacer.setMaxWidth(800);

        HBox mainLayout = new HBox();
        mainLayout.getChildren().addAll(gameMode, centerSpacer, gameOptions);
        mainLayout.setAlignment(Pos.CENTER);


        HBox.setHgrow(gameMode, Priority.ALWAYS);
        HBox.setHgrow(gameOptions, Priority.ALWAYS);


        root = new StackPane(mainLayout);
        root.setId("pane");
    }

    VBox createHumanPlayerRow() {
        VBox row = new VBox(5);
        row.getStyleClass().add("ai-player-row");

        Label lbl = new Label("Player 1 (You)");
        lbl.getStyleClass().add("game-text");

        // Styling checking to look somewhat like the ComboBox but static
        Label typeLabel = new Label("Human");
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; " +
                           "-fx-padding: 5; -fx-border-color: #660000; -fx-border-radius: 5; " +
                           "-fx-border-width: 1; -fx-pref-width: 270; -fx-alignment: center; " +
                           "-fx-background-color: rgba(0,0,0,0.3);");

        row.getChildren().addAll(lbl, typeLabel);
        return row;
    }

    private void refreshPlayerIndices() {
        // Start loop from 1 to skip human player
        for (int i = 1; i < playerSetupBox.getChildren().size(); i++) {
            javafx.scene.Node node = playerSetupBox.getChildren().get(i);
            if (node instanceof VBox) {
                VBox row = (VBox) node;
                // Assuming first child is HBox header
                if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof HBox) {
                    HBox header = (HBox) row.getChildren().get(0);
                    // Assuming first child of header is Label
                    if (!header.getChildren().isEmpty() && header.getChildren().get(0) instanceof Label) {
                        Label lbl = (Label) header.getChildren().get(0);
                        lbl.setText("Player " + (i + 1));
                    }
                }
            }
        }
    }

    VBox createPlayerRow(int playerIndex) {
        VBox row = new VBox(5);
        row.getStyleClass().add("ai-player-row");
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label lbl = new Label("Player " + playerIndex);
        lbl.getStyleClass().add("game-text"); 
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4d4d; -fx-font-weight: bold; -fx-border-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 5 0 0;");
        removeBtn.setOnAction(e -> {
            playerSetupBox.getChildren().remove(row);
            refreshPlayerIndices();
        });

        header.getChildren().addAll(lbl, spacer, removeBtn);
        
        ComboBox<AIConfig.ModelType> modelBox = new ComboBox<>(FXCollections.observableArrayList(AIConfig.ModelType.values()));
        modelBox.setValue(AIConfig.ModelType.MCTS);

        // Logic to hide/show difficulty based on model selection
        ComboBox<AIConfig.Difficulty> diffBox = new ComboBox<>(FXCollections.observableArrayList(AIConfig.Difficulty.values()));
        diffBox.setValue(AIConfig.Difficulty.MEDIUM);
        
        modelBox.setOnAction(e -> {
            if (modelBox.getValue() == AIConfig.ModelType.HUMAN) {
                diffBox.setVisible(false);
            } else {
                diffBox.setVisible(true);
            }
        });

        row.getChildren().addAll(header, modelBox, diffBox);
        return row;
    }

    void menuSetup() {

        StartGame = new Button("Play Game"); // Main entry
       
        playerSetupBox = new VBox(10);
        playerSetupBox.setAlignment(Pos.CENTER);
        
        // Add Human immediately
        playerSetupBox.getChildren().add(createHumanPlayerRow());
        // Add one opponent immediately (Player 2)
        playerSetupBox.getChildren().add(createPlayerRow(2));
        
        addPlayerButton = new Button("+ add Player");
        addPlayerButton.setOnAction(e -> {
            int currentCount = playerSetupBox.getChildren().size();
            if (currentCount < 4) { // Max 4 players
                 // Next player index is currentCount + 1
                 playerSetupBox.getChildren().add(createPlayerRow(currentCount + 1));
            }
        });

        Button btnStartGame = new Button("Start Game");
        btnStartGame.setOnAction(e -> startGame(e));

        Rules = new Button("Rules");
        Exit = new Button("Exit");

        gameMode = new VBox(10,
                StartGame,
                Rules, Exit
        );
        gameOptions = new VBox(10,
                playerSetupBox, addPlayerButton, btnStartGame
        );


        double buttonWidth = 300;
        StartGame.setPrefWidth(buttonWidth);
        Rules.setPrefWidth(buttonWidth);
        Exit.setPrefWidth(buttonWidth);
        btnStartGame.setPrefWidth(buttonWidth);
        addPlayerButton.setPrefWidth(buttonWidth);


        gameMode.setFillWidth(false);
        gameOptions.setFillWidth(false);

    }

    public Pane getView() {
        return root;
    }

    void setupActions() {
        StartGame.setOnAction(e -> showGameOptions(e));
        Rules.setOnAction(e -> Rules(e));
        Exit.setOnAction(e -> System.exit(0));
    }


    void hideOptions() {
        if (gameOptions != null) {
            gameOptions.setVisible(false);
        }
        playerSetupBox.setVisible(false);
        addPlayerButton.setVisible(false);
    }


    void showGameOptions(ActionEvent e) {
        hideOptions();
        gameOptions.setVisible(true);
        playerSetupBox.setVisible(true);
        addPlayerButton.setVisible(true);
    }

    void startGame(ActionEvent e) {
        List<AIConfig> configs = new ArrayList<>();
        // Start from index 1 to skip Human row (implicitly Player 1 is Human)
        for (int i = 1; i < playerSetupBox.getChildren().size(); i++) {
             javafx.scene.Node node = playerSetupBox.getChildren().get(i);
             if (node instanceof VBox) {
                 VBox row = (VBox) node;
                 // Assuming structure is HBox(Header), ComboBox, ComboBox
                 if (row.getChildren().size() >= 3 && row.getChildren().get(1) instanceof ComboBox) {
                     ComboBox<AIConfig.ModelType> modelBox = (ComboBox<AIConfig.ModelType>) row.getChildren().get(1);
                     ComboBox<AIConfig.Difficulty> diffBox = (ComboBox<AIConfig.Difficulty>) row.getChildren().get(2);
                     configs.add(new AIConfig("Player " + (i + 1), modelBox.getValue(), diffBox.getValue()));
                 }
             }
        }

        System.out.println("Starting game with 1 Human + " + configs.size() + " Configured Opponents.");
        Stage menuStage = (Stage) root.getScene().getWindow();
        GameView gameView = new GameView(1, configs); // Passing 1 initial human, plus configs (which might be humans too)
        Scene gameScene = new Scene(gameView.getView());

        try {
            URL cssUrl = getClass().getResource("/GUI/styleGame.css");
            if (cssUrl != null) {
                gameScene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Stylesheet not found: /GUI/styleGame.css. Check resource path.");
            }
        } catch (Exception ex) {
            System.err.println("Error loading stylesheet.");
            ex.printStackTrace();
        }

        menuStage.setScene(gameScene);
        menuStage.setTitle("Can't Stop");
        menuStage.setMaximized(true);
        menuStage.show();
    }

    void Rules(ActionEvent e) {
        Stage rulesStage = new Stage();
        BorderPane rulesRoot = new BorderPane();
        Scene rulesScene = new Scene(rulesRoot, 600, 400);

        Button rulesVideo = new Button("Video");
        Button rulesRules = new Button("Rules");

        rulesVideo.setOnAction(event -> {
            WebView webView = new WebView();
            webView.getEngine().load("https://www.youtube.com/watch?v=L6Zk7j1mJDE&ab_channel=Boardgaimz");
            rulesRoot.setCenter(webView);
        });

        rulesRules.setOnAction(event-> {
            TextArea rulesText = new TextArea(
                    "Objective:\n" +
                            "The objective of Can't Stop is to be the first player to claim three columns by reaching the top of them.\n\n" +
                            "Setup:\n" +
                            "1. Each player chooses a color and takes the corresponding markers.\n" +
                            "2. Place the game board in the center of the table.\n" +
                            "3. Each player places one of their markers on the starting position of each column (2-12).\n\n" +
                            "Gameplay:\n" +
                            "1. On a player's turn, they roll four dice.\n" +
                            "2. The player then pairs the dice into two groups and sums each group to determine two column numbers.\n" +
                            "3. The player can then move their markers up the columns corresponding to the sums rolled.\n" +
                            "4. A player can choose to stop rolling at any time during their turn and end their turn, locking in their progress for that turn.\n" +
                            "5. If a player rolls and cannot move any of their markers (because they have already reached the top of those columns or cannot move further), they lose all progress made during that turn.\n\n" +
                            "Winning the Game:\n" +
                            "The first player to reach the top of three different columns wins the game."
            );
            rulesText.setWrapText(true);
            rulesText.setEditable(false);
            rulesText.getStyleClass().add("game-text-area");
            rulesRoot.setCenter(rulesText);
        });

        VBox rulesButtons = new VBox(10, rulesVideo, rulesRules);
        rulesRoot.setLeft(rulesButtons);
        rulesStage.setScene(rulesScene);
        rulesStage.setTitle("Game Rules");
        rulesStage.show();
    }
}