import java.awt.*;
import javax.swing.*;
import javax.swing.text.html.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ChatClientGUI extends JFrame {
    private JTextPane chatPane;
    private JTextField inputField;
    private PrintWriter out;
    private BufferedWriter htmlLogWriter;
    private String nickname, hobby;
    private String base64Image = "";

    public ChatClientGUI(String server, int port) {
        showProfileInputDialog();
        setTitle("Chat - " + nickname);
        setSize(600, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ① チャット表示ペイン初期化
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setContentType("text/html");
        chatPane.setText("<html><body style='font-family:sans-serif;'></body></html>");
        add(new JScrollPane(chatPane), BorderLayout.CENTER);

        // ② 入力欄
        inputField = new JTextField();
        inputField.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            String time = getCurrentTime();
            out.println("[" + time + "] [" + nickname + "] " + text);
            inputField.setText("");
        });
        add(inputField, BorderLayout.SOUTH);

        initHtmlLog();
        setVisible(true);
        connectToServer(server, port);
    }

    private void showProfileInputDialog() {
        JTextField nameField  = new JTextField();
        JTextField hobbyField = new JTextField();
        JLabel imgPreview     = new JLabel("未選択", SwingConstants.CENTER);
        JButton imgBtn        = new JButton("画像を選択");

        imgBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                BufferedImage src = ImageIO.read(chooser.getSelectedFile());
                BufferedImage dst = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = dst.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                   RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, 50, 50, null);
                g.dispose();
                imgPreview.setIcon(new ImageIcon(dst));

                // Base64 エンコード
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(dst, "png", baos);
                base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        JPanel p = new JPanel(new GridLayout(0,1,5,5));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        p.setPreferredSize(new Dimension(300, 300));
        p.add(new JLabel("ニックネーム:")); p.add(nameField);
        p.add(new JLabel("趣味:"));        p.add(hobbyField);
        p.add(imgBtn);                     p.add(imgPreview);

        int res = JOptionPane.showConfirmDialog(
            this, p, "プロフィール入力",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (res!=JOptionPane.OK_OPTION) System.exit(0);
        nickname = nameField.getText().trim();
        if (nickname.isEmpty()) nickname = "名無しさん";
        hobby = hobbyField.getText().trim();
    }

    private void initHtmlLog() {
        try {
            File f = new File("chat_log.html");
            boolean n = f.createNewFile();
            htmlLogWriter = new BufferedWriter(new FileWriter(f, true));
            if (n || f.length()==0) {
                htmlLogWriter.write("<html><head><meta charset='UTF-8'></head>"
                                 + "<body style='font-family:sans-serif;'>\n");
                htmlLogWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer(String server, int port) {
        try {
            Socket s = new Socket(server, port);
            out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(s.getInputStream())
            );

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        appendMessageBlock(line);
                    }
                } catch (IOException ex) {
                    appendMessageBlock("[" + getCurrentTime() + "] [system] 接続が切れました。");
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "サーバー接続失敗",
                                          "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // ③ プロフィール付きメッセージ挿入メソッド
    private void appendMessageBlock(String raw) {
        SwingUtilities.invokeLater(() -> {
            try {
                // raw = "[HH:mm:ss] [nickname] message..."
                int t1 = raw.indexOf("]");
                int t2 = raw.indexOf("]", t1+1);
                String time = raw.substring(1, t1);
                String user = raw.substring(raw.indexOf("[", t1+1)+1, t2);
                String msg  = raw.substring(t2+2);

                // HTML ブロック組み立て
                String block = String.format(
                  "<div style='margin:10px; display:flex; align-items:flex-start;'>"
                + "<div style='flex-shrink:0;'>"
                + "<img src='data:image/png;base64,%s'"
                + " style='width:50px;height:50px;border-radius:50%%;'/>"
                + "<div style='text-align:center; font-size:0.8em;'>%s</div>"
                + "<div style='text-align:center; font-size:0.7em; color:#666;'>趣味: %s</div>"
                + "</div>"
                + "<div style='margin-left:10px;'>"
                + "<div style='font-size:0.8em; color:#999;'>%s</div>"
                + "<div style='margin-top:5px; color:%s;'>%s</div>"
                + "</div></div>",
                  base64Image,
                  escapeHTML(user),
                  escapeHTML(hobby),
                  escapeHTML("[" + time + "]"),
                  user.equals(nickname) ? "#0084FF" : "#333",
                  escapeHTML(msg)
                );

                HTMLEditorKit kit = (HTMLEditorKit) chatPane.getEditorKit();
                HTMLDocument doc = (HTMLDocument) chatPane.getDocument();
                kit.insertHTML(doc, doc.getLength(), block, 0, 0, null);
                chatPane.setCaretPosition(doc.getLength());

                // HTMLログにも書き出し
                if (htmlLogWriter != null) {
                    htmlLogWriter.write(block);
                    htmlLogWriter.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String escapeHTML(String s) {
        return s.replace("&","&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;");
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    public static void main(String[] args) {
        String ip = JOptionPane.showInputDialog(
            null, "サーバーIPを入力:", "設定", JOptionPane.PLAIN_MESSAGE
        );
        final String serverIp = (ip==null || ip.isEmpty()) ? "localhost" : ip;
        SwingUtilities.invokeLater(() -> new ChatClientGUI(serverIp, 12345));
    }
}