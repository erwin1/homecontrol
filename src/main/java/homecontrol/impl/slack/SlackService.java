package homecontrol.impl.slack;

import homecontrol.services.notications.NotificationService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

@ApplicationScoped
public class SlackService implements NotificationService {
    @ConfigProperty(name = "EVCHARGING_SLACK_URL")
    String slackUrl;
    @ConfigProperty(name = "EVCHARGING_SLACK_CHANNEL")
    String channel;

    @Inject
    SlackService slackService;

    @Override
    public void sendNotification(String text) {
        //need to subscribe (and ignore any results) because Async runs lazy
        slackService.doSendNotification(text).subscribe().with(e -> {
            //ok
        }, e -> {
            Logger.getLogger(SlackService.class.getName()).info("Error sending message to slack "+e);
        });
    }

    @Retry(maxRetries = 3, delay = 10_000)
    @Asynchronous
    Uni<Void> doSendNotification(String text) {
        if (slackUrl != null) {
            try {
                JsonObject payload = new JsonObject();
                payload.put("channel", "#"+channel);
                payload.put("username", "homecontrol");
                payload.put("text", text);
                payload.put("icon_emoji", ":electric_plug:");
                System.out.println("body = " + payload.toString());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(slackUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("payload="+payload))
                        .build();
                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("non-200 response from slack "+response.statusCode());
                }
            } catch (Exception e) {
                Logger.getLogger(SlackService.class.getName()).severe("posting to slack failed " + text + " " + e);
                throw new RuntimeException(e);
            }
        }
        return Uni.createFrom().voidItem();
    }


}
