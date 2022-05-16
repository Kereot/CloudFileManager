package gui.client.requests;

public class AuthRequest implements Wrapper {
    @Override
    public Request getType() {
        return Request.AUTH;
    }

    private String login;
    private int password;

    public AuthRequest(String login, int password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public int getPassword() {
        return password;
    }
}
