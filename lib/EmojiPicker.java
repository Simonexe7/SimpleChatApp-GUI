package lib;

// import com.vdurmont.emoji.Emoji;
// import com.vdurmont.emoji.EmojiManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class EmojiPicker {
    private final Map<String, List<String>> emojiMap = new LinkedHashMap<>();

    public EmojiPicker() {
        loadEmojisFromJson();
    }

    private void loadEmojisFromJson() {
        try (InputStream is = new FileInputStream("resources/emoji.json")) {
            JSONArray array = new JSONArray(new JSONTokener(is));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String emoji = obj.getString("emoji");
                String category = obj.optString("category", "Others");

                emojiMap.computeIfAbsent(category, k -> new ArrayList<>()).add(emoji);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void show(Component invoker, JTextField inputField) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(invoker), "Pick Emoji");
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(null);
        dialog.getContentPane().setBackground(new Color(245, 245, 245));
        dialog.setUndecorated(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        for (Map.Entry<String, List<String>> entry : emojiMap.entrySet()) {
            String category = entry.getKey();
            List<String> emojis = entry.getValue();

            JPanel gridPanel = new JPanel(new GridLayout(0, 10, 6, 6));
            gridPanel.setBackground(new Color(250, 250, 250));
            gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            for (String emoji : emojis) {
                JButton btn = new JButton(emoji);
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                btn.setPreferredSize(new Dimension(40, 40));
                btn.setFocusable(false);
                btn.setBackground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
                btn.setToolTipText(emoji);

                btn.addActionListener(e -> insertEmojiAtCaret(inputField, emoji, dialog));
                btn.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        btn.setBackground(new Color(230, 230, 230));
                    }

                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        btn.setBackground(Color.WHITE);
                    }
                });

                gridPanel.add(btn);
            }

            JScrollPane scrollPane = new JScrollPane(gridPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
            scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                protected void configureScrollBarColors() {
                    this.thumbColor = new Color(200, 200, 200);
                }
            });
            
            int buttonHeight = 40 + 6;
            int visibleRows = 5;
            int topBottomPadding = 20;
            
            int preferredHeight = (buttonHeight * visibleRows) + topBottomPadding;
            
            scrollPane.setPreferredSize(new Dimension(
                10 * 50,
                preferredHeight 
            ));
            
            tabs.addTab(category, scrollPane);
        }
        
        dialog.add(tabs, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    
    private void insertEmojiAtCaret(JTextField inputField, String emoji, JDialog dialog) {
        int pos = inputField.getCaretPosition();
        StringBuilder sb = new StringBuilder(inputField.getText());
        sb.insert(pos, emoji);
        inputField.setText(sb.toString());
        inputField.setCaretPosition(pos + emoji.length());
        dialog.dispose();
    }
}
