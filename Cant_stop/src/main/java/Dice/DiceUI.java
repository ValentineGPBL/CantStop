package Dice;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiceUI extends JFrame {


    final JPanel dicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    final JPanel sumsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    final JButton rollBtn = new JButton("Roll");
    final JButton moveBtn = new JButton("Move");

       final Dice d1 = new Dice();
    final Dice d2 = new Dice();
    final Dice d3 = new Dice();
    final Dice d4 = new Dice();

    Integer selectedSum = null;


    final List<JPanel> slotPanels = new ArrayList<>();
    final List<JButton> slotButtons = new ArrayList<>();
    final List<JLabel> slotRemLabels = new ArrayList<>();
    final Map<Integer, Integer> sumIndexMap = new HashMap<>(); // sum -> index

    private final DiceUIController controller;

    public DiceUI() {
        super("Dice - Pair Sums");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Rolled:"));
        top.add(dicePanel);
        add(top, BorderLayout.NORTH);

        JScrollPane sumsScroll = new JScrollPane(sumsPanel);
        sumsScroll.setPreferredSize(new Dimension(300, 140));
        add(sumsScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.add(rollBtn);
        bottom.add(moveBtn);
        add(bottom, BorderLayout.SOUTH);

        controller = new DiceUIController(this);
        rollBtn.addActionListener(e -> controller.rollDice());
        moveBtn.addActionListener(e -> controller.onMove());
        moveBtn.setEnabled(false);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DiceUI().setVisible(true));
    }
}