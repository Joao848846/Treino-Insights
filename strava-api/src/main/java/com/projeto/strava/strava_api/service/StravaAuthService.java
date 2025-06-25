package com.projeto.strava.strava_api.service; // Adapte este pacote

import com.projeto.strava.strava_api.model.strava.StravaAuth; // Seu documento de DB
import com.projeto.strava.strava_api.model.strava.token.StravaTokenResponse; // Seu POJO de resposta do Strava
import com.projeto.strava.strava_api.Repository.StravaAuthRepository;
import org.springframework.beans.factory.annotation.Value; // Para ler configs do application.properties
import org.springframework.http.MediaType; // Para definir tipo de conteúdo HTTP
import org.springframework.stereotype.Service; // Indica que é um Serviço Spring
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional; // Para lidar com resultados que podem não existir

@Service // Marca esta classe como um componente de serviço Spring
public class StravaAuthService {

    private String clientId;
    private String clientSecret;
    private String tokenUrl; // URL para trocar código por tokens
    // Injetando o repositório para salvar no MongoDB
    private final StravaAuthRepository stravaAuthRepository;

    // Injetando WebClient para fazer as chamadas HTTP (vamos configurar ele depois no pacote 'config')
    private final WebClient webClient;

    // Construtor para injeção de dependências
    public StravaAuthService(
            StravaAuthRepository stravaAuthRepository,
            WebClient.Builder webClientBuilder,
            @Value("${strava.client-id}") String clientId, // Injetado aqui
            @Value("${strava.client-secret}") String clientSecret, // Injetado aqui
            @Value("${strava.api.token-url}") String tokenUrl // Injetado aqui
    ) {
        this.stravaAuthRepository = stravaAuthRepository;
        this.clientId = clientId; // Atribuindo aos campos da classe
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;

        // Agora, 'tokenUrl' terá um valor quando esta linha for executada
        this.webClient = webClientBuilder.baseUrl(this.tokenUrl).build();

        System.out.println("DEBUG: tokenUrl injetada no StravaAuthService: " + this.tokenUrl); // Para confirmar
    }


    // --- Método para trocar o Código de Autorização por Tokens e Salvar no DB ---
    public StravaAuth exchangeCodeForTokens(String authorizationCode) {
        // Log para debug
        System.out.println("Iniciando troca de código por tokens para: " + authorizationCode.substring(0, Math.min(authorizationCode.length(), 10)) + "...");

        // Construindo o corpo da requisição POST para o Strava
        // application/x-www-form-urlencoded
        StravaTokenResponse tokenResponse = webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED) // Tipo de conteúdo esperado pelo Strava
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("code", authorizationCode)
                        .with("grant_type", "authorization_code")) // Tipo de concessão para troca de código
                .retrieve() // Executa a requisição
                .bodyToMono(StravaTokenResponse.class) // Mapeia a resposta para o seu POJO StravaTokenResponse
                .block(); // Bloqueia a execução até a resposta (para simplicidade no MVP. Em apps maiores, usaria reativo)

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Falha ao obter tokens do Strava. Resposta nula ou sem access_token.");
        }

        // Mapeando a resposta do Strava para o seu documento StravaAuth
        Long athleteId = tokenResponse.getAthlete() != null ? tokenResponse.getAthlete().getId() : null;
        if (athleteId == null) {
            throw new RuntimeException("ID do atleta não encontrado na resposta do token do Strava.");
        }

        // Tenta encontrar um registro existente para o atleta
        Optional<StravaAuth> existingAuth = stravaAuthRepository.findByAthleteId(athleteId);
        StravaAuth stravaAuth;

        if (existingAuth.isPresent()) {
            // Se já existe, atualiza os tokens
            stravaAuth = existingAuth.get();
            stravaAuth.updateTokens(
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresAt()
            );
            stravaAuth.setScope(tokenResponse.getScope()); // O escopo pode ter mudado, então atualiza
        } else {
            // Se não existe, cria um novo registro
            stravaAuth = new StravaAuth(
                    athleteId,
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken(),
                    tokenResponse.getExpiresAt(),
                    tokenResponse.getScope()
            );
        }

        // Salva/Atualiza no MongoDB
        stravaAuth = stravaAuthRepository.save(stravaAuth);

        System.out.println("Tokens do atleta " + athleteId + " salvos/atualizados no MongoDB.");
        return stravaAuth;
    }


    // --- Método para Renovar o Access Token usando o Refresh Token ---
    public StravaAuth refreshAccessToken(Long athleteId) {
        Optional<StravaAuth> existingAuthOpt = stravaAuthRepository.findByAthleteId(athleteId);
        if (existingAuthOpt.isEmpty()) {
            throw new RuntimeException("Nenhum registro de autenticação Strava encontrado para o atleta ID: " + athleteId);
        }

        StravaAuth existingAuth = existingAuthOpt.get();
        System.out.println("Iniciando refresh do token para atleta ID: " + athleteId);

        StravaTokenResponse tokenResponse = webClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", existingAuth.getRefreshToken()) // Usa o refresh token salvo
                        .with("grant_type", "refresh_token")) // Tipo de concessão para refresh
                .retrieve()
                .bodyToMono(StravaTokenResponse.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new RuntimeException("Falha ao renovar tokens do Strava para atleta ID: " + athleteId + ". Resposta nula ou sem access_token.");
        }

        // Atualiza o registro existente com os novos tokens
        existingAuth.updateTokens(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(), // O refresh token pode mudar no refresh!
                tokenResponse.getExpiresAt()
        );

        // Salva/Atualiza no MongoDB
        stravaAuthRepository.save(existingAuth);
        System.out.println("Token do atleta " + athleteId + " renovado e salvo no MongoDB.");
        return existingAuth;
    }


    // --- Método para obter o Access Token válido (renovando se necessário) ---
    public String getAccessToken(Long athleteId) {
        Optional<StravaAuth> stravaAuthOpt = stravaAuthRepository.findByAthleteId(athleteId);
        if (stravaAuthOpt.isEmpty()) {
            throw new RuntimeException("Nenhum token Strava encontrado para o atleta ID: " + athleteId + ". Por favor, conecte-se ao Strava primeiro.");
        }

        StravaAuth stravaAuth = stravaAuthOpt.get();

        // Verifica se o token expirou (usando o método do StravaAuth)
        if (stravaAuth.isAccessTokenExpired()) {
            System.out.println("Access Token expirado para atleta ID: " + athleteId + ". Iniciando refresh.");
            // Se expirou, renova e pega o novo token
            stravaAuth = refreshAccessToken(athleteId);
        } else {
            System.out.println("Access Token válido para atleta ID: " + athleteId + ". Usando token existente.");
        }

        return stravaAuth.getAccessToken();
    }

    // Método auxiliar para obter o ID do atleta principal do app (para testes iniciais)
    // Em um app real, isso viria do usuário logado no seu sistema.
    public Optional<StravaAuth> getPrimaryStravaAuth() {
        // Para testar, você pode buscar por um athleteId específico,
        // ou pegar o primeiro que encontrar no DB, ou o mais recente.
        // Por enquanto, vamos buscar o primeiro registro que encontrar.
        return stravaAuthRepository.findAll().stream().findFirst();
    }
}
