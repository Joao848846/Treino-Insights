package com.projeto.strava.strava_api.controller; // Adapte este pacote para o seu projeto

import org.springframework.beans.factory.annotation.Value; // Para ler as configurações do application.properties
import org.springframework.http.HttpHeaders; // Para manipular cabeçalhos HTTP
import org.springframework.http.HttpStatus; // Para definir o status da resposta HTTP
import org.springframework.http.ResponseEntity; // Para construir respostas HTTP completas
import org.springframework.stereotype.Controller; // Indica que esta classe é um Controller do Spring
import org.springframework.web.bind.annotation.GetMapping; // Para mapear requisições GET
import org.springframework.web.bind.annotation.RequestMapping; // Para definir o caminho base dos endpoints
import org.springframework.web.bind.annotation.RequestParam; // Para pegar parâmetros da URL

import java.net.URI; // Para trabalhar com URLs
import java.net.URLEncoder; // Para codificar URLs (segurança)
import java.nio.charset.StandardCharsets; // Para definir o encoding da URL

@Controller // Usamos @Controller aqui porque faremos um REDIRECIONAMENTO do navegador
@RequestMapping("/oauth") // Define que todos os endpoints neste controller começarão com /oauth
public class StravaOAuthController {

    // --- Injeção das configurações do application.properties ---
    // @Value lê o valor da propriedade definida no application.properties
    @Value("${strava.client-id}")
    private String clientId;

    @Value("${strava.redirect-uri}")
    private String redirectUri;

    @Value("${strava.api.auth-url}")
    private String authUrl;


    // Este método vai REDIRECIONAR o navegador do usuário para a página de autorização do Strava.
    @GetMapping("/strava/authorize")
    public ResponseEntity<Void> authorizeStrava() {

        String scope = "activity:read_all,profile:read_all";


        String state = "seu_estado_aleatorio_e_seguro_aqui_pra_testar"; // ANOTE ESTE VALOR!

        // Constrói a URL COMPLETA para a qual o navegador do usuário será redirecionado
        String fullAuthUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                authUrl,
                clientId,
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8), // Codifica a URL de redirecionamento para segurança
                URLEncoder.encode(scope, StandardCharsets.UTF_8), // Codifica os scopes
                URLEncoder.encode(state, StandardCharsets.UTF_8) // Codifica o state
        );

        // Cria um cabeçalho HTTP para o redirecionamento
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(fullAuthUrl)); // Define para onde o navegador deve ir

        // Retorna uma resposta de redirecionamento (status 302 Found)
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // --- ENDPOINT 2: Receber o Callback do Strava (APÓS a Autorização do Usuário) ---
    // Esta URL DEVE SER EXATAMENTE a que você configurou em strava.redirect-uri no application.properties
    // Ex: http://localhost:8080/oauth/callback/strava
    @GetMapping("/callback/strava")
    public String handleStravaCallback(
            @RequestParam("code") String authorizationCode, // O código de autorização que o Strava te manda
            @RequestParam("scope") String scope,           // Os scopes que foram concedidos
            @RequestParam("state") String state,           // O valor do 'state' que você enviou antes (para verificar segurança)
            @RequestParam(value = "error", required = false) String error // Se houver um erro na autorização
    ) {

        String expectedState = "seu_estado_aleatorio_e_seguro_aqui_pra_testar"; // USE O MESMO VALOR DO MÉTODO authorizeStrava()
        if (!expectedState.equals(state)) {
            System.err.println("Erro de segurança: 'state' inválido! Recebido: " + state + ", Esperado: " + expectedState);
            return "Erro de segurança: 'state' inválido. Não prossiga.";
        }

        // --- Lida com Erros de Autorização do Strava ---
        if (error != null) {
            System.err.println("Erro na autorização do Strava: " + error);
            return "Autorização Strava falhou. Erro: " + error;
        }

        // --- SUCESSO! Código de Autorização Recebido ---
        System.out.println("\n-----------------------------------------------------");
        System.out.println("SUCESSO! Código de Autorização Recebido:");
        System.out.println(authorizationCode); // ESTE É O SEU 'CODE'!
        System.out.println("Escopo concedido: " + scope);
        System.out.println("-----------------------------------------------------\n");

        // Por enquanto, para você ver o 'code' no navegador e no console:
        return "<html><body><h1>Autorização Strava Concluída!</h1>" +
                "<p>Seu Código de Autorização (temporário): <b>" + authorizationCode + "</b></p>" +
                "<p><i>Anote este código! Ele será usado na próxima etapa com o cURL (troca por tokens).</i></p>" +
                "</body></html>";
    }
}