package com.projeto.strava.strava_api.Repository;

import com.projeto.strava.strava_api.model.strava.StravaAuth;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface StravaAuthRepository extends MongoRepository<StravaAuth, String> {

    // Método para encontrar um StravaAuth pelo athleteId
    Optional<StravaAuth> findByAthleteId(Long athleteId);

    // Método para verificar se existe um StravaAuth com o athleteId fornecido
    boolean existsByAthleteId(Long athleteId);
}
