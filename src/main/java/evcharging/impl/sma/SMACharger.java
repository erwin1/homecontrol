package evcharging.impl.sma;

import evcharging.services.EVCharger;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URLConnection;

@ApplicationScoped
/**
 * SMA implementation for the EVCharger interface.
 * It uses a reverse engineered version of the device local SMA web admin UI .
 */
public class SMACharger implements EVCharger {
    @ConfigProperty(name = "EVCHARING_CHARGER_IP")
    private String chargerIp;
    @ConfigProperty(name = "EVCHARING_CHARGER_USERNAME")
    private String chargerUserName;
    @ConfigProperty(name = "EVCHARING_CHARGER_PASSWORD")
    private String chargerPassword;

    private String token;

    @Override
    public State getState() {
        try {
            return getStatusInternal();
        } catch (SMAAuthException e) {
            token = null;
            try {
                return getStatusInternal();
            } catch (SMAAuthException ex) {
                throw new RuntimeException(ex.toString());
            }
        }
    }

    public State getStatusInternal() throws SMAAuthException {
        try {
            if (token == null) {
                token = authenticate();
            }
            HttpsURLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/widgets/emobility?componentId=Plant:1");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+token);

            if (conn.getResponseCode() == 401) {
                throw new SMAAuthException();
            }
            String body = new String(conn.getInputStream().readAllBytes());//TOOD catch 401 error
            conn.getInputStream().close();
//            System.out.println("BODY = "+b);
            JsonObject object = new JsonObject(body);

            if (object.getString("err") != null) {
                throw new SMAAuthException();
            }

            State status = State.valueOf(object.getString("chargeStatus"));
            return status;
        } catch (SMAAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String authenticate() {
        try {
            URLConnection conn = HttpHelper.openConnection("https://"+chargerIp+"/api/v1/token");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write("grant_type=password&username="+chargerUserName+"&password="+chargerPassword);
            writer.flush();

            String body = new String(conn.getInputStream().readAllBytes());
            conn.getInputStream().close();
//            System.out.println("BODY = "+b);

            JsonObject obj = new JsonObject(body);
            return obj.getString("access_token");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
