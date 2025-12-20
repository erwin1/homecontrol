package homecontrol.impl.volvo;


import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.IdToken;
import io.quarkus.oidc.RefreshToken;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.mutiny.Uni;

@Path("/")
public class VolvoAuthResource {

    private static final Logger LOG = Logger.getLogger(VolvoAuthResource.class);

    @Inject
    JsonWebToken accessToken;

    @Inject
    RefreshToken refreshToken;

    @Inject
    VolvoTokenService volvoTokenService;

    private static String RT;

    @GET
    @Path("/volvo-setup")
    @Authenticated
    @Produces(MediaType.TEXT_HTML)
    public String volvoAuthSetup() {
        volvoTokenService.addToken(accessToken.getRawToken(), accessToken.getExpirationTime(), refreshToken.getToken());

        return "<html><body><h1>Authentication Successful!</h1></body></html>";
    }



}