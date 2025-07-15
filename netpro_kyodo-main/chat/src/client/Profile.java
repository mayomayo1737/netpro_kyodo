package client;

/**
 * ユーザーのプロフィール情報を保持するクラス
 */
public class Profile {
    private final String nickname;
    private final String hobby;
    private final String base64Image;

    public Profile(String nickname, String hobby, String base64Image) {
        this.nickname = nickname;
        this.hobby = hobby;
        this.base64Image = base64Image;
    }

    public String getNickname() {
        return nickname;
    }

    public String getHobby() {
        return hobby;
    }

    public String getBase64Image() {
        return base64Image;
    }

    /**
     * プロフィールが有効かどうかを判定
     */
    public boolean isValid() {
        return nickname != null && !nickname.isEmpty()
            && base64Image != null && !base64Image.isEmpty();
    }

    @Override
    public String toString() {
        return "Profile{" +
                "nickname='" + nickname + '\'' +
                ", hobby='" + hobby + '\'' +
                ", base64Image(length)=" + (base64Image != null ? base64Image.length() : "null") +
                '}';
    }
}

