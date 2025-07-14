package client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * HTML エスケープやメッセージの改行、アイコン生成などのユーティリティクラス
 */
public class HtmlUtil {

    /**
     * HTMLエスケープ処理
     */
    public static String escapeHTML(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * メッセージを指定文字数で改行（日本語向けに句読点やスペースで改行）
     */
    public static String wrapMessage(String msg, int maxChars) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (char c : msg.toCharArray()) {
            sb.append(c);
            count++;
            if (count >= maxChars && (c == '。' || c == '、' || c == ' ')) {
                sb.append("<br>");
                count = 0;
            }
        }
        return sb.toString();
    }

    /**
     * Base64文字列からImageIconを生成
     */
    public static ImageIcon iconFromBase64(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new ImageIcon(bytes);
        } catch (Exception e) {
            System.err.println("画像のデコードに失敗しました: " + e.getMessage());
            return new ImageIcon(); // 空のアイコンを返す
        }
    }

    /**
     * デフォルトのアバター画像をBase64で生成
     */
    public static String createDefaultAvatarBase64(int w, int h) {
        try {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, w, h);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            System.err.println("デフォルト画像の生成に失敗しました: " + e.getMessage());
            return "";
        }
    }
}

