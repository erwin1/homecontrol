package evcharging.impl.slack;

import evcharging.services.NotificationService;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

/**
 * (quick and dirty) Slack implementation of the notication service
 */
@ApplicationScoped
public class SlackService implements NotificationService {
    @ConfigProperty(name = "EVCHARGING_SLACK_URL")
    String slackUrl;
    @ConfigProperty(name = "EVCHARGING_SLACK_CHANNEL")
    String channel;

    //quick and dirty slack notifcation service
    @Override
    public void sendNotification(String text) {
        if (slackUrl != null) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(slackUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("payload={\"channel\": \"#" + channel + "\", \"username\": \"evcharger\", \"text\": \"" + text + "\", \"icon_emoji\": \":electric_plug:\"}"))
                        .build();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Logger.getLogger(SlackService.class.getName()).severe("posting to slack failed " + text + " http status " + response.statusCode());
                }
            } catch (Exception e) {
                Logger.getLogger(SlackService.class.getName()).severe("posting to slack failed " + text + " " + e);
            }
        }
    }

}
