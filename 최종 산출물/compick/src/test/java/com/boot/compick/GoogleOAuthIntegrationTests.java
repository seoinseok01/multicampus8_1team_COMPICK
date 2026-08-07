package com.boot.compick;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.boot.compick.member.entity.MemberRole;
import com.boot.compick.member.security.CompickOidcUser;

@SpringBootTest(properties = {
	"GOOGLE_CLIENT_ID=test-client-id",
	"GOOGLE_CLIENT_SECRET=test-client-secret"
})
@AutoConfigureMockMvc
class GoogleOAuthIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void loginPageShowsGoogleButtonWhenCredentialsExist() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(content().string(
				containsString("href=\"/oauth2/authorization/google\"")
			));
	}

	@Test
	void googleAuthorizationEndpointRedirectsToGoogle() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/google"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(
				"Location",
				startsWith("https://accounts.google.com/o/oauth2/v2/auth")
			));
	}

	@Test
	void googleAdminReceivesDatabaseAdminAuthority() {
		OidcUser googleUser = mock(OidcUser.class);
		doReturn(Set.of(new SimpleGrantedAuthority("OIDC_USER"))).when(googleUser).getAuthorities();
		CompickOidcUser user = new CompickOidcUser(googleUser, "admin", MemberRole.ADMIN, false);
		assertTrue(user.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
	}
}
