package GUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import chat.client.ChatClient;
import lib.EmojiPicker;

public class ClientUI extends JFrame {
    private JTextPane chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private JLabel header;
    
    private StyledDocument doc;
    private Map<String, Color> userColors = new HashMap<>();
    private Random random = new Random();

    private ChatClient client;
    private String username;
    private boolean isDarkMode = false;

    public ClientUI() {
        setTitle("Simple Chat App");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem toggleThemeItem = new JMenuItem("Switch to Dark Mode");
        JButton modeButton = new JButton("🌙 Dark Mode");
        
        toggleThemeItem.addActionListener(e -> toggleDarkModeAnimated(toggleThemeItem, modeButton));

        settingsMenu.add(toggleThemeItem);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 40));
        headerPanel.setBackground(new Color(70, 130, 180));

        header = new JLabel("Simple Chat App", SwingConstants.CENTER);
        header.setOpaque(true);
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setPreferredSize(new Dimension(0, 40));

        modeButton.setFocusPainted(false);
        modeButton.addActionListener(e -> toggleDarkModeAnimated(toggleThemeItem, modeButton));
        headerPanel.add(header, BorderLayout.CENTER);
        headerPanel.add(modeButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        doc = chatArea.getStyledDocument();

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("Chat"));

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createTitledBorder("List Members Room"));
        
        JSplitPane centerPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            chatScroll,
            userScroll
        );

        centerPane.setDividerLocation(600);
        centerPane.setResizeWeight(0.8);
        add(centerPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        JButton emojiButton = new JButton("😊");
        emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        EmojiPicker picker = new EmojiPicker();
        emojiButton.addActionListener(e -> picker.show(emojiButton, inputField));
        
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(emojiButton, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        add(inputPanel, BorderLayout.SOUTH);

        chatArea.setBackground(new Color(245, 245, 245));
        inputField.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        userList.setFont(new Font("SansSerif", Font.PLAIN, 13));
          
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        setVisible(true);

        connect();
    }

    private void connect() {
        username = JOptionPane.showInputDialog(this, "Enter your username: ");
        client = new ChatClient(username, this);
    }

    private void sendMessage() {
        String msg = inputField.getText();
        if (!msg.isEmpty()) {
            client.sendMessage(msg);
            inputField.setText("");
        }
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("-") || message.startsWith("=") || message.startsWith("<")) {
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, Color.GRAY);
                StyleConstants.setItalic(attr, true);

                try {
                    doc.insertString(doc.getLength(), message + "\n", attr);
                    chatArea.setCaretPosition(doc.getLength());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            } 
            
            if (message.startsWith("(Err)")){
                SimpleAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, Color.RED);
                StyleConstants.setBold(attr, true);

                try {
                    doc.insertString(doc.getLength(), message + "\n", attr);
                    chatArea.setCaretPosition(doc.getLength());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            String sender = extractSender(message);
            String text = message.substring(sender.length() + 2);

            Color color = getUserColor(sender);

            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setForeground(attr, color);
            StyleConstants.setBold(attr, true);

            try {
                doc.insertString(doc.getLength(), sender + ": ", attr);
                doc.insertString(doc.getLength(), text + "\n", null);
                chatArea.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String extractSender(String msg) {
        int idx = msg.indexOf(": ");
        if (idx > 0) return msg.substring(0, idx);
        return "Unknown";
    }

    private Color getUserColor(String user) {
        return userColors.computeIfAbsent(user, k -> {
            return new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200));
        });
    }

    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    public void updateUserList(String[] users) {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String user : users) {
                if (!user.isEmpty()) {
                    listModel.addElement(user);
                }
            }
        });
    }

    private void toggleDarkModeAnimated(JMenuItem menuItem, JButton modeButton) {
        isDarkMode = !isDarkMode;
        Color lightBg = new Color(245, 245, 245);
        Color lightFg = Color.BLACK;

        Color darkBg = new Color(40, 44, 52);
        Color darkFg = Color.WHITE;

        Color bgFrom = isDarkMode ? lightBg : darkBg;
        Color bgTo   = isDarkMode ? darkBg  : lightBg;
        Color bgToBlue = isDarkMode ? darkBg : new Color(70, 130, 180);
        Color fgFrom = isDarkMode ? lightFg : darkFg;
        Color fgTo   = isDarkMode ? darkFg  : lightFg;

        menuItem.setText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
        modeButton.setText(isDarkMode ? "☀️ Light Mode" : "🌙 Dark Mode");

        Timer timer = new Timer(10, null);
        final int steps = 30;
        final int[] currentStep = {0};

        timer.addActionListener(e -> {
            float ratio = (float) currentStep[0] / steps;
            if (ratio > 1f) ratio = 1f;

            header.setBackground(interpolateColor(bgFrom, bgToBlue, ratio));
            chatArea.setBackground(interpolateColor(bgFrom, bgTo, ratio));
            chatArea.setForeground(interpolateColor(fgFrom, fgTo, ratio));
            inputField.setBackground(interpolateColor(bgFrom, bgTo, ratio));
            inputField.setForeground(interpolateColor(fgFrom, fgTo, ratio));
            inputField.setCaretColor(interpolateColor(fgFrom, fgTo, ratio));
            userList.setBackground(interpolateColor(bgFrom, bgTo, ratio));
            userList.setForeground(interpolateColor(fgFrom, fgTo, ratio));

            currentStep[0]++;
            if (currentStep[0] > steps) {   
                timer.stop();
            }
        });

        timer.start();
    }

    private Color interpolateColor(Color c1, Color c2, float ratio) {
        int red = Math.min(255, Math.max(0, (int) (c1.getRed() + ratio * (c2.getRed() - c1.getRed()))));
        int green = Math.min(255, Math.max(0, (int) (c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen()))));
        int blue = Math.min(255, Math.max(0, (int) (c1.getBlue() + ratio * (c2.getBlue() - c1.getBlue()))));
        return new Color(red, green, blue);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientUI::new);
    }
}