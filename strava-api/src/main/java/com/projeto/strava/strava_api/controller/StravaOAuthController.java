package com.projeto.strava.strava_api.controller; // Adapte este pacote

import com.projeto.strava.strava_api.model.strava.StravaAuth;
import com.projeto.strava.strava_api.service.StravaAuthService; // Importe seu serviço
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/oauth")
public class StravaOAuthController {

    @Value("${strava.client-id}")
    private String clientId;

    @Value("${strava.redirect-uri}")
    private String redirectUri;

    @Value("${strava.api.auth-url}")
    private String authUrl;

    // --- INJETANDO SEU SERVIÇO DE AUTENTICAÇÃO ---
    private final StravaAuthService stravaAuthService;

    // Construtor para que o Spring injete o StravaAuthService
    public StravaOAuthController(StravaAuthService stravaAuthService) {
        this.stravaAuthService = stravaAuthService;
    }


    @GetMapping("/strava/authorize")
    public ResponseEntity<Void> authorizeStrava() {
        String scope = "activity:read_all,profile:read_all";
        String state = "seu_estado_aleatorio_e_seguro_aqui_pra_testar"; // ANOTE ESTE VALOR!

        String fullAuthUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                authUrl,
                clientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(scope, StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(fullAuthUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // --- MÉTODO handleStravaCallback MODIFICADO ---
    @GetMapping("/callback/strava")
    public String handleStravaCallback(
            @RequestParam("code") String authorizationCode,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            @RequestParam(value = "error", required = false) String error
    ) {
        String expectedState = "seu_estado_aleatorio_e_seguro_aqui_pra_testar"; // MESMO VALOR DO authorizeStrava()
        if (!expectedState.equals(state)) {
            System.err.println("Erro de segurança: 'state' inválido! Recebido: " + state + ", Esperado: " + expectedState);
            return "Erro de segurança: 'state' inválido. Não prossiga.";
        }

        if (error != null) {
            System.err.println("Erro na autorização do Strava: " + error);
            return "Autorização Strava falhou. Erro: " + error;
        }

        try {
            // --- CHAMA O SERVIÇO PARA TROCAR O CÓDIGO E SALVAR OS TOKENS ---
            // O método exchangeCodeForTokens já cuida de salvar no MongoDB
            StravaAuth stravaAuth = stravaAuthService.exchangeCodeForTokens(authorizationCode);

            System.out.println("\n-----------------------------------------------------");
            System.out.println("Autorização Strava Concluída e Tokens Salvos/Atualizados!");
            System.out.println("ID do Atleta Strava: " + stravaAuth.getAthleteId());
            System.out.println("Access Token (parcial): " + stravaAuth.getAccessToken().substring(0, 10) + "...");
            System.out.println("Refresh Token (parcial): " + stravaAuth.getRefreshToken().substring(0, 10) + "...");
            System.out.println("-----------------------------------------------------\n");

            return "<html><body><h1>Autorização Strava Concluída com Sucesso!</h1>" +
                    "<p>Seus tokens foram salvos no banco de dados.</p>" +
                    "<p>ID do Atleta Strava: <b>" + stravaAuth.getAthleteId() + "</b></p>" +
                    "</body></html>";

        } catch (RuntimeException e) {
            System.err.println("Erro ao processar tokens do Strava: " + e.getMessage());
            // Em um cenário real, você faria um redirecionamento para uma página de erro mais amigável
            return "<html><body><h1>Erro ao conectar com Strava!</h1>" +
                    "<p>Detalhes: " + e.getMessage() + "</p>" +
                    "</body></html>";
        }
    }
}