package keycloakverfiyapi.dto;

public  class SendMailCodeRequest {
    private String username;
    private String mailHeader;

    public String getMailHeader() {
        return mailHeader;
    }

    public void setMailHeader(String mailHeader) {
        this.mailHeader = mailHeader;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}