package GUI;

public class AIConfig {
    public enum ModelType { MCTS, Q_LEARNING, HUMAN, HYBRID }
    public enum Difficulty { EASY, MEDIUM, HARD }

    private String name;
    private ModelType model;
    private Difficulty difficulty;

    public AIConfig(String name, ModelType model, Difficulty difficulty) {
        this.name = name;
        this.model = model;
        this.difficulty = difficulty;
    }

    public String getName() { return name; }
    public ModelType getModel() { return model; }
    public Difficulty getDifficulty() { return difficulty; }
    
    public void setModel(ModelType model) { this.model = model; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
}
