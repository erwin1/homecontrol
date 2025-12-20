package homecontrol.impl.volvo;

import io.quarkus.oidc.client.OidcClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@ApplicationScoped
public class VolvoTokenService {
    private static final Logger LOG = Logger.getLogger(VolvoTokenService.class);

    @Inject
    OidcClient client;

    private String accessToken;
    private String refreshToken;
    private long exp;

    @PostConstruct
    public void init() {
        try {
            Properties properties = new Properties();
            properties.load(new FileReader("config/volvo-token.properties"));
            refreshToken = properties.getProperty("refreshtoken");
            LOG.info("read Volvo refreshToken = " + refreshToken);
        }catch (Exception e) {
            LOG.info("could not load volvo refresh token. use /volvo-setup");
        }
    }

    public void addToken(String accessToken, long exp, String refreshToken) {
        this.accessToken = accessToken;
        this.exp = exp;
        this.refreshToken = refreshToken;
        try {
            Properties properties = new Properties();
            properties.setProperty("refreshtoken", refreshToken);
            properties.store(new FileWriter("config/volvo-token.properties"), "");
        } catch (IOException e) {
            LOG.error("could not store new refreshtoken. "+e);
        }
    }

    public String getAccessToken() {
        if ((exp-60)*1000 < System.currentTimeMillis()) {
            refreshToken();
        }
        return accessToken;
    }

    public void refreshToken() {
        if (refreshToken == null) {
            LOG.error("cannot refresh token for Volvo. use /setup-volvo first.");
            return;
        }
        try {
            LOG.info("trying to refresh "+refreshToken);
            var tokens = client.refreshTokens(refreshToken).await().atMost(Duration.ofSeconds(10));

            LOG.info("========================================");
            LOG.info("TOKENS REFRESHED SUCCESSFULLY");
            LOG.info("========================================");
            LOG.info("New Access Token: " + tokens.getAccessToken());
            LOG.info("========================================");

            if (tokens.getRefreshToken() != null) {
                LOG.info("New Refresh Token: " + tokens.getRefreshToken());
                LOG.info("========================================");
            } else {
                LOG.info("No new refresh token received");
                LOG.info("========================================");
            }

            addToken(tokens.getAccessToken(), tokens.getAccessTokenExpiresAt(), tokens.getRefreshToken());

        } catch (Exception failure) {
            LOG.error("Failed to refresh tokens", failure);
        }
    }
}
