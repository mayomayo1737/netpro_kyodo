package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class ChatClientPanel extends JFrame {
    private final JPanel messageContainer = new JPanel();
    private final JTextArea inputArea = new JTextArea(3, 30);
    private final JScrollPane scrollPane;
    private PrintWriter out;
    private final Profile profile;
    private final HtmlLogger logger = new HtmlLogger();

    public ChatClientPanel(String host, int port) {
        profile = ProfileDialog.show(this);
        if (profile == null || !profile.isValid()) {
            JOptionPane.showMessageDialog(this, "プロフィールが無効です。アプリケーションを終了します。");
            System.exit(0);
        }

        setTitle("Chat - " + profile.getNickname());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 800);
        setLayout(new BorderLayout());

        // プロフィール表示
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        profilePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        profilePanel.add(new JLabel(HtmlUtil.iconFromBase64(profile.getBase64Image())));
        JLabel nameLabel = new JLabel(profile.getNickname() + "（趣味: " + profile.getHobby() + "）");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profilePanel.add(nameLabel);
        add(profilePanel, BorderLayout.NORTH);

        // チャット表示エリア
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messageContainer);
        add(scrollPane, BorderLayout.CENTER);

        // 入力エリア
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage(inputArea.getText());
                    inputArea.setText("");
                }
            }
        });

        JButton sendButton = new JButton("送信");
        sendButton.addActionListener(e -> {
            sendMessage(inputArea.getText());
            inputArea.setText("");
        });

        JButton fileButton = new JButton("📎 ファイル");
        fileButton.addActionListener(e -> sendBinary("FILE"));

        JButton imageButton = new JButton("🖼 画像");
        imageButton.addActionListener(e -> sendBinary("IMAGE"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(fileButton);
        buttonPanel.add(imageButton);
        buttonPanel.add(sendButton);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                logger.save(ChatClientPanel.this);
            }
        });

        connect(host, port);
        setVisible(true);
    }

    private void connect(String host, int port) {
        try {
            Socket sock = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintWriter(sock.getOutputStream(), true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            String[] parts = line.split("\\|\\|\\|", 6);
                            if (parts.length < 6) continue;

                            String type = parts[0];
                            String time = parts[1], user = parts[2], hobby = parts[3], img64 = parts[4], content = parts[5];
                            boolean isMe = user.equals(profile.getNickname());

                            JPanel rendered = null;
                            switch (type) {
                                case "TEXT":
                                    rendered = MessageRenderer.render(time, user, hobby, img64, content, isMe);
                                    if (isMe) logger.append(time, user, hobby, img64, content, true);
                                    break;
                                case "FILE":
                                    rendered = renderFileMessage(time, user, hobby, img64, content, isMe);
                                    if (isMe) logger.append(time, user, hobby, img64, "[ファイル]", true);
                                    break;
                                case "IMAGE":
                                    rendered = renderImageMessage(time, user, hobby, img64, content, isMe);
                                    if (isMe) logger.append(time, user, hobby, img64, "[画像]", true);
                                    break;
                                default:
                                    break;
                            }

                            if (rendered != null) {
                                JPanel finalRendered = rendered;
                                SwingUtilities.invokeLater(() -> {
                                    messageContainer.add(finalRendered);
                                    messageContainer.revalidate();
                                    messageContainer.repaint();
                                    scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
                                });
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "サーバーに接続できませんでした。", "接続エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage(String text) {
        String txt = text.trim();
        if (txt.isEmpty()) return;
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String safeText = txt.replace("|||", "｜｜｜");
        String payload = String.join("|||", "TEXT", "[" + time + "]",
                profile.getNickname(), profile.getHobby(), profile.getBase64Image(), safeText);
        out.println(payload);
    }

    private void sendBinary(String type) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try {
            if (type.equals("IMAGE")) {
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    JOptionPane.showMessageDialog(this, "画像ファイルを選択してください。", "エラー", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            byte[] data = Files.readAllBytes(file.toPath());
            String encoded = Base64.getEncoder().encodeToString(data);
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String payload = String.join("|||", type, "[" + time + "]",
                    profile.getNickname(), profile.getHobby(), profile.getBase64Image(), encoded);
            out.println(payload);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "送信に失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel renderFileMessage(String time, String user, String hobby, String img64, String fileBase64, boolean isMe) {
        byte[] fileData = Base64.getDecoder().decode(fileBase64);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel info = new JLabel("<html><b>" + HtmlUtil.escapeHTML(user) + "</b>（趣味: " + HtmlUtil.escapeHTML(hobby)
                + "）[" + HtmlUtil.escapeHTML(time) + "]</html>");
        JLabel fileInfo = new JLabel("ファイルサイズ: " + fileData.length + " バイト");

        JButton saveButton = new JButton("ファイル保存");
        saveButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                    fos.write(fileData);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "ファイルの保存に失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(info);
        content.add(fileInfo);
        content.add(Box.createVerticalStrut(5));
        content.add(saveButton);

        JLabel avatar = new JLabel(HtmlUtil.iconFromBase64(img64));
        avatar.setPreferredSize(new Dimension(50, 50));

        if (isMe) {
            panel.add(content, BorderLayout.EAST);
            panel.add(avatar, BorderLayout.WEST);
        } else {
            panel.add(avatar, BorderLayout.WEST);
            panel.add(content, BorderLayout.CENTER);
        }

        return panel;
    }

    private JPanel renderImageMessage(String time, String user, String hobby, String img64, String imageBase64, boolean isMe) {
        byte[] imageData = Base64.getDecoder().decode(imageBase64);
        ImageIcon imageIcon = new ImageIcon(imageData);
        Image scaled = imageIcon.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaled));

        JLabel info = new JLabel("<html><b>" + HtmlUtil.escapeHTML(user) + "</b>（趣味: " + HtmlUtil.escapeHTML(hobby)
                + "）[" + HtmlUtil.escapeHTML(time) + "]</html>");

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(info);
        content.add(Box.createVerticalStrut(5));
        content.add(imageLabel);

        JLabel avatar = new JLabel(HtmlUtil.iconFromBase64(img64));
        avatar.setPreferredSize(new Dimension(50, 50));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (isMe) {
            panel.add(content, BorderLayout.EAST);
            panel.add(avatar, BorderLayout.WEST);
        } else {
            panel.add(avatar, BorderLayout.WEST);
            panel.add(content, BorderLayout.CENTER);
        }

        return panel;
    }

    public static void main(String[] args) {
        String host = JOptionPane.showInputDialog("サーバーIP:", "localhost");
        if (host != null && !host.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClientPanel(host.trim(), 12345));
        }
    }
}

