package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

/**
 * ユーザーにプロフィール情報（ニックネーム、趣味、アバター画像）を入力させるダイアログ
 */
public class ProfileDialog {

    public static Profile show(Component parent) {
        JTextField nameField = new JTextField();
        JTextField hobbyField = new JTextField();
        JLabel imgLabel = new JLabel("未選択", SwingConstants.CENTER);
        JButton imgButton = new JButton("画像選択");

        final String[] base64Image = {null};

        imgButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
            try {
                BufferedImage src = ImageIO.read(chooser.getSelectedFile());
                if (src == null) {
                    JOptionPane.showMessageDialog(parent, "画像ファイルが無効です。", "エラー", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                BufferedImage dst = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = dst.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, 50, 50));
                g.drawImage(src, 0, 0, 50, 50, null);
                g.dispose();

                imgLabel.setIcon(new ImageIcon(dst));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(dst, "png", baos);
                base64Image[0] = Base64.getEncoder().encodeToString(baos.toByteArray());

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent, "画像の読み込みに失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setPreferredSize(new Dimension(300, 300));
        panel.add(new JLabel("ニックネーム:"));
        panel.add(nameField);
        panel.add(new JLabel("趣味:"));
        panel.add(hobbyField);
        panel.add(imgButton);
        panel.add(imgLabel);

        int result = JOptionPane.showConfirmDialog(parent, panel, "プロフィール", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return null;

        String nickname = nameField.getText().trim().isEmpty() ? "名無しさん" : nameField.getText().trim();
        String hobby = hobbyField.getText().trim();

        if (base64Image[0] == null) {
            base64Image[0] = HtmlUtil.createDefaultAvatarBase64(50, 50);
        }

        return new Profile(nickname, hobby, base64Image[0]);
    }
}
