package homecontrol.services.ev;

import java.math.BigDecimal;
import java.time.Instant;

public class EVState {
    private Instant timestamp;
    private int battery_level;
    private int charge_amps;
    private int charge_current_request;
    private int charge_current_request_max;
    private int charge_enable_request;
    private int charge_limit_soc;
    private int charge_limit_soc_max;
    private boolean charge_port_door_open;
    private String charge_port_latch;
    private BigDecimal charge_rate;
    private int charger_actual_current;
    private int charger_power;
    private String charging_state;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getBattery_level() {
        return battery_level;
    }

    public void setBattery_level(int battery_level) {
        this.battery_level = battery_level;
    }

    public int getCharge_amps() {
        return charge_amps;
    }

    public void setCharge_amps(int charge_amps) {
        this.charge_amps = charge_amps;
    }

    public int getCharge_current_request() {
        return charge_current_request;
    }

    public void setCharge_current_request(int charge_current_request) {
        this.charge_current_request = charge_current_request;
    }

    public int getCharge_current_request_max() {
        return charge_current_request_max;
    }

    public void setCharge_current_request_max(int charge_current_request_max) {
        this.charge_current_request_max = charge_current_request_max;
    }

    public int getCharge_enable_request() {
        return charge_enable_request;
    }

    public void setCharge_enable_request(int charge_enable_request) {
        this.charge_enable_request = charge_enable_request;
    }

    public int getCharge_limit_soc() {
        return charge_limit_soc;
    }

    public void setCharge_limit_soc(int charge_limit_soc) {
        this.charge_limit_soc = charge_limit_soc;
    }

    public int getCharge_limit_soc_max() {
        return charge_limit_soc_max;
    }

    public void setCharge_limit_soc_max(int charge_limit_soc_max) {
        this.charge_limit_soc_max = charge_limit_soc_max;
    }

    public boolean isCharge_port_door_open() {
        return charge_port_door_open;
    }

    public void setCharge_port_door_open(boolean charge_port_door_open) {
        this.charge_port_door_open = charge_port_door_open;
    }

    public String getCharge_port_latch() {
        return charge_port_latch;
    }

    public void setCharge_port_latch(String charge_port_latch) {
        this.charge_port_latch = charge_port_latch;
    }

    public BigDecimal getCharge_rate() {
        return charge_rate;
    }

    public void setCharge_rate(BigDecimal charge_rate) {
        this.charge_rate = charge_rate;
    }

    public int getCharger_actual_current() {
        return charger_actual_current;
    }

    public void setCharger_actual_current(int charger_actual_current) {
        this.charger_actual_current = charger_actual_current;
    }

    public int getCharger_power() {
        return charger_power;
    }

    public void setCharger_power(int charger_power) {
        this.charger_power = charger_power;
    }

    public String getCharging_state() {
        return charging_state;
    }

    public void setCharging_state(String charging_state) {
        this.charging_state = charging_state;
    }
}
