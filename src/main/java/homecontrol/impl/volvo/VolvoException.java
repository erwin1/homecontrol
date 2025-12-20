package homecontrol.impl.volvo;

public class VolvoException extends Exception {
    private int code;

    public VolvoException(int code, String message) {
        super(code + " " + message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
