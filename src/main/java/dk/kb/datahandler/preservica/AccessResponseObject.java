package dk.kb.datahandler.preservica;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JAVA Object representation of JSON response from Preservica Access API. The Object is mapped from the following JSON
 * structure, delivered by the {@code /login}- and {@code /refresh}-endpoints of the Access API.:
 * <pre>
 * {
 *   "success": true,
 *   "token": "664c455f-59f2-4c83-9d7a-ddfe5e4b363d",
 *   "refresh-token": "3bb58740-db8e-4332-bcff-7a7435f5686e",
 *   "validFor": 15,
 *   "user": "manager"
 * }
 * </pre>
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
     * Constructor for creating the AccessResponseObject through the JACKSON {@link com.fasterxml.jackson.databind.ObjectMapper}
     * @param success boolean value representing if the authentication is successful.
     * @param token the accessToken provided by the Access API.
     * @param refreshToken the refreshToken provided by the Access API.
     * @param validFor the amount of minutes this accessToken is valid.
     * @param user the user, which the accessToken has been issued for.
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
                ", token='" + token + "'" +
                ", refreshToken='" + refreshToken + "'" +
                ", validFor=" + validFor +
                ", user='" + user + "'" +
                '}';
    }
}

