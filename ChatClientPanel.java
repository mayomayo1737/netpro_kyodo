import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.*;

public class ChatClientPanel extends JFrame {
    private JPanel messageContainer;
    private JTextField inputField;
    private PrintWriter out;
    private String nickname, hobby, base64Image;

    // 自動スクロール用
    private JScrollPane scrollPane;

    // HTMLログ用バッファ
    private StringBuilder htmlLog;

    public ChatClientPanel(String host, int port) {
        showProfileDialog();

        // HTMLログのヘッダー初期化
        htmlLog = new StringBuilder();
        htmlLog.append("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>Chat Log</title>
              <style>
                body { font-family: sans-serif; }
                .message { margin: 10px; }
                .bubble { padding: 8px 12px; border-radius: 15px; max-width: 300px; display: inline-block; }
                .me { background-color: #d4f0ff; }
                .other { background-color: #f0f0f0; }
                img.avatar { vertical-align: middle; width: 50px; height: 50px; border-radius: 5px; }
              </style>
            </head>
            <body>
            """);

        // 閉じる時にHTMLログを保存
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveHtmlLog();
            }
        });

        setTitle("Chat - " + nickname);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 800);
        setLayout(new BorderLayout());

        messageContainer = new JPanel();
        messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));

        // フィールド化したスクロールペイン
        scrollPane = new JScrollPane(messageContainer);
        add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());
        add(inputField, BorderLayout.SOUTH);

        connect(host, port);
        setVisible(true);
    }

    private void showProfileDialog() {
        JTextField nameF = new JTextField();
        JTextField hobbyF = new JTextField();
        JLabel imgLabel = new JLabel("未選択", SwingConstants.CENTER);
        JButton btn = new JButton("画像選択");

        btn.addActionListener(e -> {
            JFileChooser ch = new JFileChooser();
            if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                BufferedImage src = ImageIO.read(ch.getSelectedFile());
                BufferedImage dst = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = dst.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, 50, 50, null);
                g.dispose();
                imgLabel.setIcon(new ImageIcon(dst));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(dst, "png", baos);
                base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.setPreferredSize(new Dimension(300, 300));
        p.add(new JLabel("ニックネーム:"));
        p.add(nameF);
        p.add(new JLabel("趣味:"));
        p.add(hobbyF);
        p.add(btn);
        p.add(imgLabel);

        if (JOptionPane.showConfirmDialog(this, new JScrollPane(p), "プロフィール", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        nickname = nameF.getText().trim().isEmpty() ? "名無しさん" : nameF.getText().trim();
        hobby = hobbyF.getText().trim();

        if (base64Image == null) {
            try {
                BufferedImage b = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = b.createGraphics();
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, 50, 50);
                g.dispose();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(b, "png", baos);
                base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connect(String host, int port) {
        try {
            Socket sock = new Socket(host, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            out = new PrintWriter(sock.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> addMessage(finalLine));
                    }
                } catch (IOException ignored) {
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String txt = inputField.getText().trim();
        if (txt.isEmpty()) return;

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String payload = String.join("|||", "[" + time + "]", nickname, hobby, base64Image, txt);
        out.println(payload);
        inputField.setText("");
    }

    private void addMessage(String raw) {
        String[] parts = raw.split("\\|\\|\\|", 5);
        if (parts.length < 5) return;

        String time = parts[0], user = parts[1], hobbyR = parts[2], img64 = parts[3], msg = parts[4];
        boolean isMe = user.equals(nickname);

        // アイコン画像
        byte[] imgBytes = Base64.getDecoder().decode(img64);
        ImageIcon icon = new ImageIcon(imgBytes);
        JLabel imgLabel = new JLabel(icon);
        imgLabel.setPreferredSize(new Dimension(50, 50));

        // 名前と趣味
        JLabel nameLabel = new JLabel("<html><b>" + user + "</b>（趣味: " + hobbyR + "）</html>");
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // メッセージ吹き出し
        String wrappedMsg = wrapMessage(escapeHTML(msg), 25);
        String bubbleStyle = "background-color:#d4f0ff;padding:8px 12px;border-radius:15px;max-width:300px;";
        JLabel messageLabel = new JLabel("<html><div style='" + bubbleStyle + "'>" + wrappedMsg + "</div></html>");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // レイアウト構築
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(messageLabel);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        if (isMe) {
            row.add(textPanel, BorderLayout.EAST);
            row.add(imgLabel, BorderLayout.WEST);
        } else {
            row.add(imgLabel, BorderLayout.WEST);
            row.add(textPanel, BorderLayout.CENTER);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        if (isMe) wrapper.add(row, BorderLayout.EAST);
        else     wrapper.add(row, BorderLayout.WEST);

        // HTMLログ追記
        appendToHtmlLog(time, user, hobbyR, img64, msg, isMe);

        // 画面に追加
        messageContainer.add(wrapper);
        messageContainer.revalidate();
        messageContainer.repaint();

        // 自動スクロール
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void appendToHtmlLog(String time,
                                 String user,
                                 String hobby,
                                 String img64,
                                 String msg,
                                 boolean isMe) {
        String sideClass = isMe ? "me" : "other";
        htmlLog.append("<div class='message'>\n");
        htmlLog.append(String.format(
            "<img class='avatar' src='data:image/png;base64,%s'> ",
            img64));
        htmlLog.append(String.format(
            "<strong>%s</strong>（趣味: %s） [%s]<br>\n",
            escapeHTML(user),
            escapeHTML(hobby),
            escapeHTML(time)
        ));
        htmlLog.append(String.format(
            "<div class='bubble %s'>%s</div>\n",
            sideClass,
            wrapMessage(escapeHTML(msg), 25)
        ));
        htmlLog.append("</div>\n");
    }

    private void saveHtmlLog() {
        htmlLog.append("</body>\n</html>");

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("chatlog.html"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (Writer w = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                w.write(htmlLog.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String wrapMessage(String message, int maxChars) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < message.length(); i++) {
            sb.append(message.charAt(i));
            count++;
            if (count >= maxChars) {
                sb.append("<br>");
                count = 0;
            }
        }
        return sb.toString();
    }

    private String escapeHTML(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static void main(String[] args) {
        String host = JOptionPane.showInputDialog("サーバーIP:", "localhost");
        new ChatClientPanel(host, 12345);
    }
}