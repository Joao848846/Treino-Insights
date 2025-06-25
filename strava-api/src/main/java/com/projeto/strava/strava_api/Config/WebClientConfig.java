package com.projeto.strava.strava_api.Config;

import org.springframework.context.annotation.Bean; // Para a anotação @Bean
import org.springframework.context.annotation.Configuration; // Indica que esta classe é de configuração
import org.springframework.web.reactive.function.client.WebClient; // A classe WebClient

@Configuration // Marca esta classe como uma fonte de definição de beans para o Spring
public class WebClientConfig {

    // Define um bean para o WebClient.Builder
    // O Spring vai automaticamente fornecer uma instância de WebClient.Builder
    // Este Builder pode ser usado para construir diferentes instâncias de WebClient
    // ou uma instância padrão que será injetada em outros serviços.
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // Se você quisesse um WebClient com uma base URL padrão para a API do Strava,
    // poderia fazer assim (mas para o StravaAuthService já estamos passando a tokenUrl diretamente):
    /*
    @Bean
    public WebClient stravaWebClient(WebClient.Builder builder,
                                     @Value("${strava.api.base-url}") String stravaApiBaseUrl) {
        return builder.baseUrl(stravaApiBaseUrl)
                      .build();
    }
    */
}
