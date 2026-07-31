package com.boot.compick.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class GoogleOAuthConfig {

	@Bean
	@ConditionalOnExpression(
		"'${GOOGLE_CLIENT_ID:}' != '' and '${GOOGLE_CLIENT_SECRET:}' != ''"
	)
	ClientRegistrationRepository googleClientRegistrationRepository(
		@Value("${GOOGLE_CLIENT_ID}") String clientId,
		@Value("${GOOGLE_CLIENT_SECRET}") String clientSecret
	) {
		ClientRegistration google = CommonOAuth2Provider.GOOGLE
			.getBuilder("google")
			.clientId(clientId)
			.clientSecret(clientSecret)
			.scope("openid", "profile", "email")
			.build();
		return new InMemoryClientRegistrationRepository(google);
	}
}
