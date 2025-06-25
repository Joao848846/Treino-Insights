package com.projeto.strava.strava_api.model.strava;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime; // usar LocalDateTime para armazenar data e hora
import java.time.ZoneOffset; // usar ZoneOffset para armazenar o fuso horário

@Data
@Document(collection = "strava_auth")
public class StravaAuth {

    @Id // Este campo será o ID primário do documento no MongoDB
    private String id; // Vamos usar o athleteId do Strava como ID do documento para facilitar a busca e garantir unicidade

    private Long athleteId; // O ID numérico real do atleta no Strava
    private String accessToken;
    private String refreshToken;
    private Long expiresAt; // Timestamp Unix (segundos desde a Época) de expiração do access token
    private String scope;   // Os scopes concedidos (ex: "activity:read_all,profile:read_all")

    private LocalDateTime createdAt; // Timestamp de quando este registro foi criado no SEU banco de dados
    private LocalDateTime updatedAt; // Timestamp da última atualização (útil após um token refresh)

    // Construtor padrão sem argumentos (necessário para o Spring Data)
    public StravaAuth() {}

    // Construtor para criar uma instância de StravaAuth
    public StravaAuth(Long athleteId, String accessToken, String refreshToken, Long expiresAt, String scope) {
        this.athleteId = athleteId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.scope = scope;
        this.createdAt = LocalDateTime.now(); // Define a data de criação
        this.updatedAt = LocalDateTime.now(); // Define a data de atualização inicial
        this.id = String.valueOf(athleteId); // Usa o athleteId como o ID do documento no MongoDB
    }

    // Método auxiliar para verificar se o access token expirou
    public boolean isAccessTokenExpired() {

        return LocalDateTime.now().plusMinutes(5)
                .isAfter(LocalDateTime.ofEpochSecond(expiresAt, 0, ZoneOffset.UTC));
    }

    // Método auxiliar para atualizar os tokens após um refresh
    public void updateTokens(String newAccessToken, String newRefreshToken, Long newExpiresAt) {
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
        this.expiresAt = newExpiresAt;
        this.updatedAt = LocalDateTime.now(); // Atualiza a data de modificação
    }
}
