package Dice;

import javax.swing.*;
import java.awt.*;

public class DiceUIController {

    private final DiceUI ui;

    public DiceUIController(DiceUI ui) {
        this.ui = ui;
    }

    public void rollDice() {
        Dice.rollAll(ui.d1, ui.d2, ui.d3, ui.d4);
        ui.selectedSum = null;
        ui.moveBtn.setEnabled(false);
        refreshDiceDisplay();
        buildPairSumSlots();
    }

    public void refreshDiceDisplay() {
        ui.dicePanel.removeAll();
        int[] vals = Dice.values(ui.d1, ui.d2, ui.d3, ui.d4);
        for (int v : vals) {
            JLabel l = new JLabel(String.valueOf(v), SwingConstants.CENTER);
            l.setPreferredSize(new Dimension(36, 36));
            l.setOpaque(true);
            l.setBackground(Color.WHITE);
            l.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            ui.dicePanel.add(l);
        }
        ui.dicePanel.revalidate();
        ui.dicePanel.repaint();
        ui.pack();
    }

    public void buildPairSumSlots() {
        ui.sumsPanel.removeAll();
        ui.slotPanels.clear();
        ui.slotButtons.clear();
        ui.slotRemLabels.clear();
        ui.sumIndexMap.clear();

        int[] sums = Dice.pairSums(ui.d1, ui.d2, ui.d3, ui.d4);

        int cols = Math.min(3, Math.max(1, sums.length));
        ui.sumsPanel.setLayout(new GridLayout(0, cols, 6, 6));

        for (int i = 0; i < sums.length; i++) {
            int s = sums[i];

            JPanel card = new JPanel(new CardLayout());
            card.setPreferredSize(new Dimension(80, 36));

            JButton btn = new JButton(String.valueOf(s));
            btn.setFocusable(false);
            btn.setMargin(new Insets(4, 8, 4, 8));
            // toggle selection/deselection
            btn.addActionListener(evt -> {
                if (ui.selectedSum != null && ui.selectedSum.equals(s)) {
                    ui.selectedSum = null;
                    ui.moveBtn.setEnabled(false);
                    buildPairSumSlots();
                } else {
                    ui.selectedSum = s;
                    showSelectedAndRemaining();
                }
            });

            JLabel remLabel = new JLabel("", SwingConstants.CENTER);
            remLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

            JLabel empty = new JLabel(" ", SwingConstants.CENTER);

            card.add(empty, "empty");
            card.add(btn, "btn");
            card.add(remLabel, "rem");

            ((CardLayout) card.getLayout()).show(card, "btn");

            ui.slotPanels.add(card);
            ui.slotButtons.add(btn);
            ui.slotRemLabels.add(remLabel);
            ui.sumIndexMap.put(s, i);

            ui.sumsPanel.add(card);
        }

        ui.sumsPanel.revalidate();
        ui.sumsPanel.repaint();
        ui.pack();
    }

    public void showSelectedAndRemaining() {
        if (ui.selectedSum == null) return;
        int total = Dice.sumAll(ui.d1, ui.d2, ui.d3, ui.d4);
        int remaining = total - ui.selectedSum;

        Integer selectedIndex = ui.sumIndexMap.get(ui.selectedSum);
        Integer remainingIndex = ui.sumIndexMap.get(remaining);

        if (remainingIndex == null && selectedIndex != null && ui.slotPanels.size() > 0) {
            remainingIndex = (selectedIndex + 1) % ui.slotPanels.size();
        }

        for (int i = 0; i < ui.slotPanels.size(); i++) {
            CardLayout cl = (CardLayout) ui.slotPanels.get(i).getLayout();
            JButton btn = ui.slotButtons.get(i);
            JLabel remLabel = ui.slotRemLabels.get(i);

            btn.setBorder(UIManager.getBorder("Button.border"));
            btn.setBackground(null);

            if (selectedIndex != null && i == selectedIndex) {
                cl.show(ui.slotPanels.get(i), "btn");
                btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                btn.setBackground(new Color(180, 215, 255));
            } else if (remainingIndex != null && i == remainingIndex) {
                remLabel.setText(String.valueOf(remaining));
                remLabel.setOpaque(true);
                remLabel.setBackground(Color.WHITE);
                remLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLUE, 2),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
                cl.show(ui.slotPanels.get(i), "rem");
            } else {
                remLabel.setText("");
                remLabel.setOpaque(false);
                remLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                cl.show(ui.slotPanels.get(i), "empty");
            }
        }

        ui.moveBtn.setEnabled(true);
        ui.sumsPanel.revalidate();
        ui.sumsPanel.repaint();
        ui.pack();
    }

    public void onMove() {
        if (ui.selectedSum == null) return;
        int remaining = Dice.sumAll(ui.d1, ui.d2, ui.d3, ui.d4) - ui.selectedSum;
        JOptionPane.showMessageDialog(ui,
                "You can moved: " + ui.selectedSum + "," + remaining,
                "Move",
                JOptionPane.INFORMATION_MESSAGE);
    }
}