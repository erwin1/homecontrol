package homecontrol.impl.tesla;

public class TeslaException extends Exception {
    private int code;

    public TeslaException(int code, String message) {
        super(code + " " + message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
