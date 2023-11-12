package homecontrol.services.ev;

public class EVException extends Exception {
    public EVException() {
    }

    public EVException(String message) {
        super(message);
    }

    public EVException(String message, Throwable cause) {
        super(message, cause);
    }

    public EVException(Throwable cause) {
        super(cause);
    }

    public EVException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
