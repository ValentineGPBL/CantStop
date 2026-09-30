package GUI;

import AI.Hybrid.HybridMCTSAgent;
import AI.Hybrid.HybridPlayer;
import AI.Qlearning.QTable;
import Dice.Dice;
import Player.Player;
import Turn.Turn;
import Conditions.BustCondition;
import Conditions.WinCondition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import AI.MCTS.MCTSAgent;
import AI.MCTS.MCTSPlayer;
import AI.GameAction;
import AI.GameState;
import AI.MCTS.PlayerViewAdapter;
import AI.AIPlayer;
import AI.Qlearning.QLearningPlayer;
import GUI.AIConfig;


import java.util.*;

public class GameView {

    // UI Elements
    private final BorderPane root;
    private final ImageView diceView1 = new ImageView();
    private final ImageView diceView2 = new ImageView();
    private final ImageView diceView3 = new ImageView();
    private final ImageView diceView4 = new ImageView();
    private final GridPane sumsGrid;
    private final Button moveButton;
    private final Button rollButton;
    private final Button finishButton;

    // Game State
    private final List<Player> players;
    // private final MCTSAgent mctsAgent; // Removed

    private int currentPlayerIndex = 0;
    private Turn currentTurn;
    private Integer selectedSum = null;
    private boolean gameOver = false;
    private final Set<Integer> globallyClaimedColumns = new HashSet<>();
    private final Random rand = new Random();
    private boolean rollSinceMove = false;

    // MCTS AI

    private final int winTarget = 3;

    // map from player index to its AI controller
    private final Map<Integer, AIPlayer> aiPlayers = new HashMap<>();



    //  store counts
    private final int numHumanPlayers;
    private final int numAIPlayers;


    private final String[] PLAYER_PALETTE = {
            "#E53935", // red
            "#1E88E5", // blue
            "#43A047", // green
            "#FDD835"  // yellow
    };
    private final Set<String> assignedColors = new HashSet<>();
    private int paletteIndex = 0;

    private final Map<Integer, Node> playerUIMap = new HashMap<>();
    private final List<Button> sumButtons = new ArrayList<>();
    private final Map<Integer, Integer> sumIndexMap = new HashMap<>();

    // Game Logic
    private final Dice d1, d2, d3, d4;
    private final WinCondition winCondition;
    private final BustCondition bustCondition;

    // Board Drawing
    private Rectangle[][] groupedRectangles;
    private StackPane[] groupPanes;
    private Rectangle[][] overlays;

    public GameView(int initialHumanCount, List<AIConfig> aiConfigs) {
        d1 = new Dice(); d2 = new Dice(); d3 = new Dice(); d4 = new Dice();
        winCondition = new WinCondition();
        bustCondition = new BustCondition();
        
        int totalPlayers = initialHumanCount + aiConfigs.size();
        
        int aiCount = 0;
        int humanCount = initialHumanCount;
        for (AIConfig cfg : aiConfigs) {
            if (cfg.getModel() != AIConfig.ModelType.HUMAN) {
                aiCount++;
            } else {
                humanCount++;
            }
        }

        this.numHumanPlayers = humanCount;
        this.numAIPlayers = aiCount;

        players = new ArrayList<>();
        for (int i = 0; i < totalPlayers; i++) {
            players.add(new Player(i + 1, generateRandomColor()));
        }

        for (int i = 0; i < aiConfigs.size(); i++) {
             AIConfig config = aiConfigs.get(i);
             int playerListIndex = initialHumanCount + i; 
             
             if (config.getModel() != AIConfig.ModelType.HUMAN) {
                 AIPlayer aiPlayer = null;
                 
                 if (config.getModel() == AIConfig.ModelType.MCTS) {
                      // Pass difficulty if applicable or standard params
                      MCTSAgent agent = new MCTSAgent(3000, Math.sqrt(2), 25);
                      aiPlayer = new MCTSPlayer(playerListIndex, winTarget, agent);
                 } else if(config.getModel() == AIConfig.ModelType.Q_LEARNING){
                      aiPlayer = new QLearningPlayer(playerListIndex, winTarget);
                 } else{
                     HybridMCTSAgent hybAgent = new HybridMCTSAgent(new QTable(), 3000, 25);
                     aiPlayer = new HybridPlayer(playerListIndex, winTarget, hybAgent);
                 }
                 aiPlayers.put(playerListIndex, aiPlayer);
             }
        }

        initDiceView(diceView1, "/GUI/Textures/DiceWhite/diceWhite1.png");
        initDiceView(diceView2, "/GUI/Textures/DiceWhite/diceWhite1.png");
        initDiceView(diceView3, "/GUI/Textures/DiceRed/diceRed1.png");
        initDiceView(diceView4, "/GUI/Textures/DiceRed/diceRed1.png");

        rollButton = new Button("Roll Dice");
        rollButton.setId("dice-button");
        rollButton.setOnAction(e -> { if (!gameOver) rollDiceAnimation(); });

        finishButton = new Button("Finish Turn");
        finishButton.setId("dice-button");
        finishButton.setOnAction(e -> { if (!gameOver) handleFinishTurn(); });

        moveButton = new Button("Move");
        moveButton.setId("dice-button");
        moveButton.setDisable(true);
        moveButton.setOnAction(e -> { if (!gameOver) performMove(); });

        HBox diceImages = new HBox(10, diceView1, diceView2, diceView3, diceView4);
        diceImages.setAlignment(Pos.CENTER);

        sumsGrid = new GridPane();
        sumsGrid.setHgap(6);
        sumsGrid.setVgap(6);
        sumsGrid.setVisible(false);
        sumsGrid.setMinHeight(120);

        StackPane sumsPane = new StackPane(sumsGrid);
        sumsPane.setMinHeight(120);

        VBox diceArea = new VBox(12, rollButton, diceImages, sumsPane, moveButton, finishButton);
        diceArea.setAlignment(Pos.TOP_CENTER);
        diceArea.setPadding(new Insets(20));
        diceArea.setPrefWidth(350);
        diceArea.setId("dice-area");

        VBox playersArea = new VBox(20);
        playersArea.setAlignment(Pos.CENTER);
        playersArea.setPadding(new Insets(20));
        playersArea.setPrefWidth(180);
        playersArea.setId("players-area");
        initPlayersUI(playersArea);

        StackPane centerPane = new StackPane();
        centerPane.setStyle("-fx-background-color: transparent;");
        initBoard(centerPane, this.numHumanPlayers, this.numAIPlayers);

        root = new BorderPane();
        root.setLeft(playersArea);
        root.setCenter(centerPane);
        root.setRight(diceArea);
        root.setId("game-root");

        this.currentTurn = new Turn(players.get(currentPlayerIndex));
        drawBoardStateBetweenTurns();

        checkTriggerAI();
    }

    public void checkTriggerAI(){
        if (aiPlayers.containsKey(currentPlayerIndex) && !gameOver) {
            rollButton.setDisable(true);
            moveButton.setDisable(true);
            finishButton.setDisable(true);

            rollDiceAnimation();
        }
    }

    public void move(int columnSum, int playerID, Color color, double heightFraction) {
        if (groupedRectangles == null) return;

        int g = columnSum - 2;
        int j = playerID - 1;

        if (g < 0 || g >= groupedRectangles.length || j < 0 || j >= groupedRectangles[0].length) {
            System.err.println("Invalid move parameters: column=" + columnSum + ", player=" + playerID);
            return;
        }

        if (overlays == null) {
            overlays = new Rectangle[groupedRectangles.length][groupedRectangles[0].length];
        }

        // remove the existing marker
        Rectangle existing = overlays[g][j];
        if (existing != null) {
            groupPanes[g].getChildren().remove(existing);
            overlays[g][j] = null;
        }


        if (heightFraction > 0) {
            Rectangle targetBar = groupedRectangles[g][j];
            Rectangle overlay = new Rectangle(targetBar.getWidth() * 0.8, 0);
            overlay.setFill(color);
            overlay.setStroke(Color.BLACK);
            overlay.setStrokeWidth(1);
            overlay.heightProperty().bind(Bindings.max(2, targetBar.heightProperty().multiply(heightFraction)));
            // Fix: Align to bottom to match the bar alignment
            overlay.setTranslateX(targetBar.getTranslateX());
            StackPane.setAlignment(overlay, Pos.BOTTOM_CENTER);
            groupPanes[g].getChildren().add(overlay);
            overlays[g][j] = overlay;
        }
    }

    private void advanceToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        currentTurn = new Turn(players.get(currentPlayerIndex));
        updatePlayerHighlight();
        drawBoardStateBetweenTurns();


        rollButton.setDisable(false);
        finishButton.setDisable(true);
        rollSinceMove = false;

        if (!gameOver) {
            checkTriggerAI();
        }
    }


    private void handleFinishTurn() {
        Player currentPlayer = players.get(currentPlayerIndex);
        boolean winOccurred = false;


        List<Integer> newlyCompleted = currentTurn.stopAndCommit();

        if (!newlyCompleted.isEmpty()) {
            ArrayList<Integer> playerClaims = currentPlayer.getColumnsClaimed();
            for (int newColumn : newlyCompleted) {
                if (!playerClaims.contains(newColumn)) {
                    playerClaims.add(newColumn);
                    currentPlayer.setColumnsClaimed(playerClaims);

                    globallyClaimedColumns.add(newColumn);
                    ejectOtherPlayers(newColumn, currentPlayer.getPlayerID());

                    if (winCondition.hasPlayerWon(currentPlayer)) {
                        winOccurred = true;
                        break;
                    }
                }
            }
        }

        drawBoardStateBetweenTurns();

        if (winOccurred) {
            gameOver = true;
            showWinDialog(currentPlayer);
            rollButton.setDisable(true);
            moveButton.setDisable(true);
            finishButton.setDisable(true);
        } else {
            advanceToNextPlayer();
        }
    }


    private void runAiAfterRoll(int[] diceValues) {
        AIPlayer ai = aiPlayers.get(currentPlayerIndex);
        if (ai == null) return;

        List<int[]> candidates = generatePairingsForDice(diceValues);
        ArrayList<int[]> legalPairs = currentTurn.legalPairings(new ArrayList<>(candidates), globallyClaimedColumns);

        List<PlayerViewAdapter> views = players.stream().map(PlayerViewAdapter::new).toList();
        GameState stateWithRoll = GameState.fromLive(
                views,
                currentPlayerIndex,
                winTarget,
                currentTurn.getTempStepsArray(),
                currentTurn.getActiveColsArray()
        ).withPairingsAndDice(legalPairs, diceValues);


        GameAction pairingAction = ai.chooseAction(stateWithRoll);

        if (!(pairingAction instanceof GameAction.Pairing)) {
            if (!legalPairs.isEmpty()) {
                int[] first = legalPairs.get(0);
                pairingAction = new GameAction.Pairing(first[0], first[1]);
            } else {
                handleFinishTurn();
                return;
            }
        }

        GameAction.Pairing p = (GameAction.Pairing) pairingAction;
        currentTurn.applyPairing(p.sumA(), p.sumB(), globallyClaimedColumns);

        updateBoardWithTurnState();
        resetDiceUI();

        GameState stateAfterMove = GameState.fromLive(
                views,
                currentPlayerIndex,
                winTarget,
                currentTurn.getTempStepsArray(),
                currentTurn.getActiveColsArray()
        );

        GameAction nextDecision = ai.chooseAction(stateAfterMove);


        if (nextDecision instanceof GameAction.Roll) {
            System.out.println("AI Player " + (currentPlayerIndex + 1) + " decides to ROLL AGAIN.");

             rollDiceAnimation();

        } else {
            System.out.println("AI Player " + (currentPlayerIndex + 1) + " decides to STOP/FINISH.");
            handleFinishTurn();
        }
    }


    private void rollDiceAnimation() {
        rollButton.setDisable(true);
        finishButton.setDisable(true);

        int final1 = d1.roll();
        int final2 = d2.roll();
        int final3 = d3.roll();
        int final4 = d4.roll();

        Timeline timeline = new Timeline();

        for (int i = 0; i < 15; i++) {
            KeyFrame kf = new KeyFrame(Duration.millis(i * 100), ignored -> {
                diceView1.setImage(new Image(getClass().getResource(
                        "/GUI/Textures/DiceWhite/diceWhite" + (rand.nextInt(6) + 1) + ".png").toExternalForm()));
                diceView2.setImage(new Image(getClass().getResource(
                        "/GUI/Textures/DiceWhite/diceWhite" + (rand.nextInt(6) + 1) + ".png").toExternalForm()));
                diceView3.setImage(new Image(getClass().getResource(
                        "/GUI/Textures/DiceRed/diceRed" + (rand.nextInt(6) + 1) + ".png").toExternalForm()));
                diceView4.setImage(new Image(getClass().getResource(
                        "/GUI/Textures/DiceRed/diceRed" + (rand.nextInt(6) + 1) + ".png").toExternalForm()));
            });
            timeline.getKeyFrames().add(kf);
        }


        KeyFrame finalFrame = new KeyFrame(Duration.millis(1600), ignored -> {
            diceView1.setImage(new Image(getClass().getResource("/GUI/Textures/DiceWhite/diceWhite" + final1 + ".png").toExternalForm()));
            diceView2.setImage(new Image(getClass().getResource("/GUI/Textures/DiceWhite/diceWhite" + final2 + ".png").toExternalForm()));
            diceView3.setImage(new Image(getClass().getResource("/GUI/Textures/DiceRed/diceRed" + final3 + ".png").toExternalForm()));
            diceView4.setImage(new Image(getClass().getResource("/GUI/Textures/DiceRed/diceRed" + final4 + ".png").toExternalForm()));

            Platform.runLater(() -> {
                int[] diceValues = {d1.getValue(), d2.getValue(), d3.getValue(), d4.getValue()};
                Map<Integer, Integer> columnRemaining = getColumnRemainingSteps();
                Set<Integer> claimedColumns = getGloballyClaimedColumns();
                Map<Integer, Integer> tempMarkers = getTempMarkersAsMap();
                boolean[] activeCols = currentTurn.getActiveColsArray();

                BustCondition.BustResult result = bustCondition.evaluate(diceValues, columnRemaining, claimedColumns, tempMarkers, activeCols);
                rollSinceMove = true;

                if (result.busted) {
                    System.out.println("--- BUSTED! No possible moves. ---");
                    currentTurn.bust();
                    showBustDialog();
                    drawBoardStateBetweenTurns();
                    advanceToNextPlayer();
                } else {
                    System.out.println("Possible moves: " + result.possibleMoves);

                    if (aiPlayers.containsKey(currentPlayerIndex)) {
                        // let MCTS choose Stop or a Pairing from this roll
                        runAiAfterRoll(diceValues);
                    } else {
                        // human: show pairing buttons like before
                        sumsGrid.setVisible(true);
                        buildPairSumSlots();
                    }
                }
            });
        });
        timeline.getKeyFrames().add(finalFrame);
        timeline.play();
    }


    private void performMove() {
        if (selectedSum == null || currentTurn == null) return;

        int total = Dice.sumAll(d1, d2, d3, d4);
        int remaining = total - selectedSum;

        Turn.MoveResult result = currentTurn.applyPairing(selectedSum, remaining, globallyClaimedColumns);

        if (result.ok) {
            rollSinceMove = false;
            updateBoardWithTurnState();
            resetDiceUI();
            rollButton.setDisable(false);
            finishButton.setDisable(false);
        } else {
            System.err.println("Illegal move: " + result.reason);
            showIllegalMoveDialog(result.reason);
            selectedSum = null;
            buildPairSumSlots();
        }
    }

    private void initPlayersUI(VBox playersArea) {
        playersArea.getChildren().clear();
        for (Player player : players) {
            Circle colorPlayer = new Circle(12, Color.web(player.getColor()));
            colorPlayer.setStroke(Color.BLACK);
            Label playerName = new Label("Player " + player.getPlayerID());
            // Changed text to white for dark theme
            playerName.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");
            HBox playerBox = new HBox(10, colorPlayer, playerName);
            playerBox.setAlignment(Pos.CENTER_LEFT);
            playerBox.setPadding(new Insets(8));
            playerBox.setStyle("-fx-background-radius: 8;");
            playersArea.getChildren().add(playerBox);
            playerUIMap.put(player.getPlayerID(), playerBox);
        }
        updatePlayerHighlight();
    }

    private void updatePlayerHighlight() {
        Player currentPlayer = players.get(currentPlayerIndex);
        for (Map.Entry<Integer, Node> entry : playerUIMap.entrySet()) {
            int playerId = entry.getKey();
            Node uiNode = entry.getValue();
            if (playerId == currentPlayer.getPlayerID()) {
                // Dark Red highlight for active player
                uiNode.setStyle(
                        "-fx-background-color: rgba(255, 77, 77, 0.3);" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: #ff4d4d;" +
                                "-fx-border-width: 2;" +
                                "-fx-border-radius: 8;"
                );
            } else {
                uiNode.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-border-color: transparent;"
                );
            }
        }
    }

    private void ejectOtherPlayers(int claimedColumn, int claimingPlayerId) {
        int columnIndex = claimedColumn - 2;

        for (Player player : players) {
            // Skip player claimed the column
            if (player.getPlayerID() == claimingPlayerId) {
                continue;
            }

            // Get player progress+rollback
            ArrayList<Integer> positions = player.getPermanentStepPositions();
            if (positions.get(columnIndex) > 0) {
                positions.set(columnIndex, 0);
                player.setPermanentStepPositions(positions);
            }
        }
    }
    private List<int[]> generatePairingsForDice(int[] d) {
        List<int[]> list = new ArrayList<>();
        list.add(new int[]{d[0] + d[1], d[2] + d[3]});
        list.add(new int[]{d[0] + d[2], d[1] + d[3]});
        list.add(new int[]{d[0] + d[3], d[1] + d[2]});
        return list;
    }
    private String generateRandomColor() {
        for (int i = 0; i < PLAYER_PALETTE.length; i++) {
            String candidate = PLAYER_PALETTE[(paletteIndex + i) % PLAYER_PALETTE.length];
            if (!assignedColors.contains(candidate)) {
                assignedColors.add(candidate);
                paletteIndex = (paletteIndex + i + 1) % PLAYER_PALETTE.length;
                return candidate;
            }
        }
        // Fallback reuse
        String fallback = PLAYER_PALETTE[paletteIndex];
        paletteIndex = (paletteIndex + 1) % PLAYER_PALETTE.length;
        return fallback;
    }

    private Rectangle[][] createGroupedRectangles(
            StackPane centerPane, int groups, int perGroup, double barWidth,
            Color barColor, double baseRatio, double step) {
        Rectangle[][] grouped = new Rectangle[groups][perGroup];
        for (int g = 0; g < groups; g++) {
            int symmetricIndex = Math.min(g, groups - 1 - g);
            double factor = baseRatio + step * symmetricIndex;
            for (int j = 0; j < perGroup; j++) {
                Rectangle r = new Rectangle(barWidth, 0);
                r.setFill(barColor);
                // Add Red Border to columns for that "Black and Red" futuristic look
                r.setStroke(Color.web("#800000")); // Dark Red Border
                r.setStrokeWidth(1);
                r.heightProperty().bind(centerPane.heightProperty().multiply(factor));
                r.setMouseTransparent(true);
                grouped[g][j] = r;
            }
        }
        return grouped;
    }

    private void initBoard(StackPane centerPane,int numPlayers, int numAIs) {
        int groups = 11;
        int perGroup = Math.max(1, numPlayers + numAIs);
        
        // Thinner bars to avoid overflowing UI with 4 players
        double barWidth = 12; // Increased slightly from 10 to be "fatter"
        
        // Dark metallic/glassy color for columns to fit the theme
        Color barColor = Color.web("#2b2b2b"); 
        
        // Make them taller to reach the peak even when sitting lower
        // Decreasing height ratio to lower them relative to the mountain peak
        double barHeightRatio = 0.22; // Decreased from 0.25
        
        // Tighter spacing between bars in a group
        double intraBarOffset = 14; // Increased from 12 to match fatter bars
        
        double groupSpacing = (perGroup + 1) * barWidth; 
        
        // Average Step to make the curve smoother
        groupedRectangles = createGroupedRectangles(centerPane, groups, perGroup,
                barWidth, barColor, barHeightRatio, 0.12); // Decreased step from 0.13
        
        groupPanes = new StackPane[groups];
        HBox groupRow = new HBox(groupSpacing);
        // Align columns to the bottom to create the "Mountain" shape
        groupRow.setAlignment(Pos.BOTTOM_CENTER);
        
        // Create Mountain Background
        javafx.scene.shape.Polygon mountain = new javafx.scene.shape.Polygon();
        
        // Gradient Fill for a "Fancier" look
        javafx.scene.paint.Stop[] stops = new javafx.scene.paint.Stop[] { 
            new javafx.scene.paint.Stop(0, Color.web("#4d0000")),  // Dark Red top
            new javafx.scene.paint.Stop(1, Color.web("#1a1a1a"))   // Black/Grey bottom
        };
        javafx.scene.paint.LinearGradient lg1 = new javafx.scene.paint.LinearGradient(
            0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE, stops);
        
        mountain.setFill(lg1); 
        mountain.setStroke(Color.web("#800000")); // Brighter dark red outline 
        mountain.setStrokeWidth(3);
        
        // Snow Cap (Top Peak)
        javafx.scene.shape.Polygon snowCap = new javafx.scene.shape.Polygon();
        snowCap.setFill(Color.web("#e0e0e0")); // Light grey/white snow
        snowCap.setOpacity(0.9);
        snowCap.setManaged(false);

        // Critical: Set managed to false so the Polygon doesn't dictate the size of the StackPane
        mountain.setManaged(false);

        // Bind mountain shape to pane size
        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> {
            double w = centerPane.getWidth();
            double h = centerPane.getHeight();
            if (w > 0 && h > 0) {
                mountain.getPoints().clear();
                snowCap.getPoints().clear();
                
                java.util.List<Double> topPoints = new java.util.ArrayList<>();
                
                // Generate jagged mountain profile
                int steps = 60; 
                double stepW = w / steps;
                
                for (int i = 0; i <= steps; i++) {
                    double x = i * stepW;
                    double distFromCenter = Math.abs(x - w/2.0) / (w/2.0); // 0 (center) to 1 (edge)
                    
                    // Shape function: 1.0 at edges, 0.1 at center (High Peak)
                    // Bell-like curve combined with linear slope
                    double shapeBase = 0.15 + (0.85 * Math.pow(distFromCenter, 1.5));
                    
                    // Add "Teeth" noise
                    // Mix of sin waves
                    double noise = 0.03 * Math.sin(i * 1.5) + 0.02 * Math.cos(i * 4.2);
                    // Add sharp spikes
                    double spike = ((i * 7) % 3 == 0) ? -0.04 : 0.0;
                    
                    double yFactor = shapeBase + noise + spike;
                    // Clamp
                    double y = h * Math.max(0.05, Math.min(1.0, yFactor));
                    
                    topPoints.add(x);
                    topPoints.add(y);
                }
                
                // Build Mountain
                mountain.getPoints().addAll(topPoints);
                // Close the loop at bottom
                mountain.getPoints().addAll(w, h, 0.0, h);
                
                // Build Snow Caps
                // Create a snow layer that sits on top of the ridge
                java.util.List<Double> snowPoints = new java.util.ArrayList<>(topPoints);
                
                // Walk backwards to close the snow polygon, but slightly lower
                for (int i = steps; i >= 0; i--) {
                     double x = i * stepW;
                     // Retrieve corresponding Top Y
                     double topY = topPoints.get(i*2 + 1);
                     
                     // Snow thickness calculation
                     // Thicker at high altitudes (low Y), non-existent at bottom
                     double altitude = 1.0 - (topY / h); // 0 (bottom) to ~0.9 (peak)
                     double snowDepth = 0;
                     
                     if (altitude > 0.4) { // Only snow on top ~60% of mountain
                         snowDepth = 50 * (altitude - 0.4); 
                         // Add some jaggedness to bottom of snow
                         snowDepth += 5 * Math.sin(i * 12.0);
                     }
                     
                     snowPoints.add(x);
                     snowPoints.add(topY + snowDepth);
                }
                snowCap.getPoints().addAll(snowPoints);
            }
        };
        
        // Initial sizing
        javafx.application.Platform.runLater(() -> {
             resizeListener.changed(null, null, null);
        });

        centerPane.widthProperty().addListener(resizeListener);
        centerPane.heightProperty().addListener(resizeListener);
        
        centerPane.getChildren().add(mountain);
        centerPane.getChildren().add(snowCap);

        // Ensure mountain is behind using toBack? Add order matters. 
        // We added mountain first, so it's at index 0 (bottom).
        // Then we add groupRow.

        double[] offsets = new double[perGroup];
        double start = -((perGroup - 1) * intraBarOffset) / 2.0;
        for (int i = 0; i < perGroup; i++) offsets[i] = start + i * intraBarOffset;
        for (int g = 0; g < groups; g++) {
            StackPane groupPane = new StackPane();
            // Allow clicks anywhere in the group pane (including empty space between thin bars)
            groupPane.setPickOnBounds(true);
            groupPanes[g] = groupPane;
            for (int j = 0; j < perGroup; j++) {
                Rectangle bar = groupedRectangles[g][j];
                bar.setTranslateX(offsets[j]);
                // Align bars to the bottom of the group pane
                StackPane.setAlignment(bar, Pos.BOTTOM_CENTER);
                groupPane.getChildren().add(bar);
            }
            Label numberLabel = new Label(String.valueOf(g+2));
            numberLabel.setStyle(
                    "-fx-font-size: 24px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-text-fill: white; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 1);"
            );
            groupPane.getChildren().add(numberLabel);
            
            // Align label to the top of the StackPane (which wraps the tall bars)
            // But with BOTTOM_CENTER alignment for bars, the StackPane height might be defined by the tallest bar.
            // If we want the label ON TOP of the bar, we use TOP_CENTER with a negative margin to push it UP,
            // OR simply put it at TOP_CENTER with a positive margin if the StackPane is tall enough?
            // Actually, if the StackPane shrinks to fit the Bars, then TOP_CENTER is the top of the bars.
            // So negative margin moves it UP (outside the pane). This is good, provided no clipping.
            StackPane.setAlignment(numberLabel, Pos.TOP_CENTER);
            StackPane.setMargin(numberLabel, new Insets(-40, 0, 0, 0));

            // Align the GroupPane itself to the bottom of the Row
            // (The Row HBox is aligned Bottom, but let's be explicit if needed, though HBox aligns children based on its alignment property usually)
            
            groupRow.getChildren().add(groupPane);
        }
        centerPane.getChildren().add(groupRow);
        // Important: Use a Margin to push the whole board UP a bit so the bottom numbers/bars aren't stuck exactly at screen edge
        // And ensure the top numbers (Peak of 7) don't get cut off.
        StackPane.setAlignment(groupRow, Pos.BOTTOM_CENTER);
        // Reduced margin so bars sit much lower, closer to the bottom of the mountain/screen
        StackPane.setMargin(groupRow, new Insets(0, 0, 10, 0)); 
        
        // Add top padding to the centerPane itself to force space for labels if needed
        centerPane.setPadding(new Insets(50, 0, 0, 0));
    }
    private void initDiceView(ImageView diceView, String initialPath) {
        diceView.setImage(new Image(getClass().getResource(initialPath).toExternalForm()));
        diceView.setFitWidth(60);
        diceView.setFitHeight(60);
    }

    public BorderPane getView() { return root; }

    private void buildPairSumSlots() {
        sumsGrid.getChildren().clear();
        sumButtons.clear();
        sumIndexMap.clear();
        int[] sums = Dice.pairSums(d1, d2, d3, d4);
        if (selectedSum == null) {
            int cols = Math.min(3, Math.max(1, sums.length));
            for (int i = 0; i < sums.length; i++) {
                int s = sums[i];
                StackPane card = new StackPane();
                card.setPrefSize(80, 36);
                Button btn = new Button(String.valueOf(s));
                btn.setFocusTraversable(false);
                btn.setOnAction(_ev -> {
                    selectedSum = s;
                    buildPairSumSlots();
                });
                card.getChildren().add(btn);
                sumButtons.add(btn);
                sumIndexMap.put(s, i);
                int r = i / cols;
                int c = i % cols;
                sumsGrid.add(card, c, r);
            }
        } else {
            int total = Dice.sumAll(d1, d2, d3, d4);
            int remaining = total - selectedSum;
            List<Integer> toShow = new ArrayList<>();
            List<Boolean> isRemaining = new ArrayList<>();
            toShow.add(selectedSum); isRemaining.add(false);
            toShow.add(remaining); isRemaining.add(true);
            for (int i = 0; i < toShow.size(); i++) {
                int s = toShow.get(i);
                boolean rem = isRemaining.get(i);
                StackPane card = new StackPane();
                card.setPrefSize(100, 36);
                String label = String.valueOf(s);
                Button btn = new Button(label);
                btn.setFocusTraversable(false);
                btn.setOnAction(_ev -> {
                    if (selectedSum != null && selectedSum.equals(s)) {
                        selectedSum = null;
                    } else {
                        selectedSum = s;
                    }
                    buildPairSumSlots();
                });
                if (selectedSum != null && selectedSum.equals(s) && !rem) {
                    // Selected First: Dark Red with Gold Border
                    btn.setStyle("-fx-border-color: #ffd700; -fx-border-width: 2; -fx-background-color: #8b0000; -fx-text-fill: white;");
                } else if (selectedSum != null && selectedSum.equals(s) && rem) {
                    // Selected Second: Dark Green with bright Green Border
                    btn.setStyle("-fx-border-color: #00ff00; -fx-border-width: 2; -fx-background-color: #006400; -fx-text-fill: white;");
                }
                card.getChildren().add(btn);
                sumButtons.add(btn);
                sumIndexMap.put(s, i);
                sumsGrid.add(card, i, 0);
            }
        }
        if (moveButton != null) moveButton.setDisable(!(selectedSum != null && rollSinceMove));
    }

    private void updateBoardWithTurnState() {
        Player player = currentTurn.getPlayer();
        ArrayList<Integer> permanentSteps = player.getPermanentStepPositions();
        int[] tempSteps = currentTurn.getTempStepsArray();
        boolean[] activeCols = currentTurn.getActiveColsArray();
        final int[] HEIGHTS = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};

        drawBoardStateBetweenTurns();
        for (int i = 0; i < activeCols.length; i++) {
            if (activeCols[i]) {
                int columnSum = i + 2;
                int totalProgress = permanentSteps.get(i) + tempSteps[i];
                double fraction = (double) totalProgress / HEIGHTS[i];
                Color tempColor = Color.web("#808080");
                move(columnSum, player.getPlayerID(), tempColor, fraction);
            }
        }
    }

    private void drawBoardStateBetweenTurns() {
        if (overlays != null) {
            for (int g = 0; g < overlays.length; g++) {
                for (int j = 0; j < overlays[g].length; j++) {
                    if (overlays[g][j] != null) {
                        groupPanes[g].getChildren().remove(overlays[g][j]);
                        overlays[g][j] = null;
                    }
                }
            }
        }

        final int[] HEIGHTS = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};
        for (Player player : players) {
            ArrayList<Integer> permanentSteps = player.getPermanentStepPositions();
            for (int i = 0; i < permanentSteps.size(); i++) {
                int progress = permanentSteps.get(i);
                if (progress > 0) {
                    int columnSum = i + 2;
                    double fraction = (double) progress / HEIGHTS[i];

                    move(columnSum, player.getPlayerID(), Color.web(player.getColor()), fraction);
                }
            }
        }
    }

    private Set<Integer> getGloballyClaimedColumns() {
        Set<Integer> allClaimed = new HashSet<>();
        for (Player p : players) {
            allClaimed.addAll(p.getColumnsClaimed());
        }
        return allClaimed;
    }
    private Map<Integer, Integer> getColumnRemainingSteps() {
        Map<Integer, Integer> remaining = new HashMap<>();
        Player player = currentTurn.getPlayer();

        ArrayList<Integer> permanentSteps = player.getPermanentStepPositions();
        int[] tempSteps = currentTurn.getTempStepsArray();

        final int[] HEIGHTS = {3, 5, 7, 9, 11, 13, 11, 9, 7, 5, 3};

        for (int i = 0; i < HEIGHTS.length; i++) {
            int columnSum = i + 2;

            int totalProgress = permanentSteps.get(i) + tempSteps[i];
            int stepsLeft = HEIGHTS[i] - totalProgress;

            remaining.put(columnSum, stepsLeft);
        }
        return remaining;
    }
    private Map<Integer, Integer> getTempMarkersAsMap() {
        Map<Integer, Integer> map = new HashMap<>();
        int[] tempSteps = currentTurn.getTempStepsArray();
        for (int i = 0; i < tempSteps.length; i++) {
            if (tempSteps[i] > 0) {
                int columnSum = i + 2;
                map.put(columnSum, tempSteps[i]);
            }
        }
        return map;
    }
    private void showWinDialog(Player winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over!");
        alert.setHeaderText("Player " + winner.getPlayerID() + " has WONNN!!!!");
        alert.setContentText("Congratulations! Player " + winner.getPlayerID() + " was the first to claim 3 columns.");
        alert.showAndWait();
    }
    private void showBustDialog() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Bust!");
        alert.setHeaderText("No legal moves available");
        alert.setContentText("You have busted. Your turn is over and you lose any progress made this turn.");
        alert.showAndWait();
    }

    private void showIllegalMoveDialog(String reason) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Illegal Move");
        alert.setHeaderText("That move isn't possible.");
        alert.setContentText(reason + ". Please choose a different combination if available.");
        alert.showAndWait();
    }

    private void resetDiceUI() {
        selectedSum = null;
        moveButton.setDisable(true);
        sumsGrid.getChildren().clear();
        sumsGrid.setVisible(false);
        finishButton.setDisable(false);
    }
}