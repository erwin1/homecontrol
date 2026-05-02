package homecontrol.services.notications;



public interface NotificationService {

    void sendNotification(NotificationType level, String text);

}