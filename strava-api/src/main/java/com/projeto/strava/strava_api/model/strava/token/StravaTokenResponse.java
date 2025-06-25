package com.projeto.strava.strava_api.model.strava.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StravaTokenResponse {

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_at")
    private Long expiresAt;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("athlete")
    private AthleteInfo  athlete;

    public String getScope() {
        return "read,activity:read,activity:write";
    }

    @Data
    public static class AthleteInfo {

        private Long id;
        private String firstname;
        private String lastname;
    }
}
