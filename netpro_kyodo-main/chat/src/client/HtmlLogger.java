package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * チャットログをHTML形式で保存するクラス
 */
public class HtmlLogger {
    private final StringBuilder html = new StringBuilder();

    public HtmlLogger() {
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <title>Chat Log</title>
              <style>
                body { font-family: sans-serif; background: #fff; color: #333; }
                .message { margin: 10px; }
                .bubble { padding: 10px 14px; border-radius: 12px; max-width: 300px; display: inline-block; word-wrap: break-word; }
                .me { background-color: #d4f0ff; }
                .other { background-color: #f0f0f0; }
                img.avatar { vertical-align: middle; width: 40px; height: 40px; border-radius: 50%; }
              </style>
            </head>
            <body>
        """);
    }

    public void append(String time, String user, String hobby, String img64, String msg, boolean isMe) {
        String sideClass = isMe ? "me" : "other";
        html.append("<div class='message'>\n");
        html.append(String.format("<img class='avatar' src='data:image/png;base64,%s'> ", img64));
        html.append(String.format("<strong>%s</strong>（%s） [%s]<br>\n",
                HtmlUtil.escapeHTML(user),
                HtmlUtil.escapeHTML(hobby),
                HtmlUtil.escapeHTML(time)));
        html.append(String.format("<div class='bubble %s'>%s</div>\n", sideClass,
                HtmlUtil.wrapMessage(HtmlUtil.escapeHTML(msg), 30)));
        html.append("</div>\n");
    }

    public void save(Component parent) {
        html.append("</body>\n</html>");
        JFileChooser chooser = new JFileChooser();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        chooser.setSelectedFile(new File("chatlog_" + timestamp + ".html"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try (Writer w = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(chooser.getSelectedFile()), "UTF-8"))) {
                w.write(html.toString());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "ログの保存に失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
