package dk.kb.datahandler.preservica;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class AccessResponseObject {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("token")
    private String token;

    @JsonProperty("refresh-token")
    private String refreshToken;

    @JsonProperty("validFor")
    private int validFor;

    @JsonProperty("user")
    private String user;

    /**
     *
     * @param success
     * @param token
     * @param refreshToken
     * @param validFor
     * @param user
     */
    public AccessResponseObject(@JsonProperty("success") boolean success,
                          @JsonProperty("token") String token,
                          @JsonProperty("refresh-token") String refreshToken,
                          @JsonProperty("validFor") int validFor,
                          @JsonProperty("user") String user) {
        this.success = success;
        this.token = token;
        this.refreshToken = refreshToken;
        this.validFor = validFor;
        this.user = user;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public int getValidFor() {
        return validFor;
    }

    public void setValidFor(int validFor) {
        this.validFor = validFor;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "ResponseObject{" +
                "success=" + success +
                ", token='" + token + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", validFor=" + validFor +
                ", user='" + user + '\'' +
                '}';
    }
}

