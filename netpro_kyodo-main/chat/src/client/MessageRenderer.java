package client;

import javax.swing.*;
import java.awt.*;
import java.util.Base64;

/**
 * チャットメッセージをSwingコンポーネントとして描画するクラス
 */
public class MessageRenderer {

    public static JPanel render(String time, String user, String hobby, String img64, String msg, boolean isMe) {
        // アバター画像
        ImageIcon avatarIcon = HtmlUtil.iconFromBase64(img64);
        Image scaledImage = avatarIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel avatar = new JLabel(new ImageIcon(scaledImage));
        avatar.setPreferredSize(new Dimension(40, 40));

        // 名前と趣味ラベル
        JLabel nameLabel = new JLabel("<html><b>" + HtmlUtil.escapeHTML(user) + "</b>（趣味: " + HtmlUtil.escapeHTML(hobby) + "）</html>");
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // 上段：アイコン＋名前
        JPanel headerPanel = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);
        if (isMe) {
            headerPanel.add(nameLabel);
            headerPanel.add(avatar);
        } else {
            headerPanel.add(avatar);
            headerPanel.add(nameLabel);
        }

        // メッセージ吹き出し
        String bubbleStyle = "padding:10px 14px; border-radius:15px; box-shadow:2px 2px 5px rgba(0,0,0,0.1);"
                + "background-color:" + (isMe ? "#d4f0ff" : "#f0f0f0") + "; max-width:400px; word-wrap:break-word;";
        String html = "<html><div style='" + bubbleStyle + "'>" + HtmlUtil.wrapMessage(HtmlUtil.escapeHTML(msg), 40)
                + "<div style='text-align:right; font-size:10px; color:#888;'>"
                + HtmlUtil.escapeHTML(time) + "</div></div></html>";
        JLabel messageLabel = new JLabel(html);
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // 下段：メッセージ
        JPanel messagePanel = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 2));
        messagePanel.setOpaque(false);
        messagePanel.add(messageLabel);

        // 全体を縦に並べる
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        wrapper.add(headerPanel);
        wrapper.add(Box.createVerticalStrut(2));
        wrapper.add(messagePanel);

        return wrapper;
    }
}

