package com.aerionsoft.application.repository.oauth;

import com.aerionsoft.application.entity.oauth.OauthPkceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthPkceStateRepository  extends JpaRepository<OauthPkceState, Long> {
    Optional<OauthPkceState> findByStateAndUsedFalse(String state);
}
