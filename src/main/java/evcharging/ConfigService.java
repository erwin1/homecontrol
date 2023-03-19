package evcharging;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigService {

    //TODO persist changes in config/application.properties

    @ConfigProperty(name = "max15minpeak")
    int max15minPeak;

    @ConfigProperty(name = "chargelimit-grid")
    int chargeLimitFromGrid;

    @ConfigProperty(name = "mode")
    Mode currentMode;

    public Mode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(Mode currentMode) {
        this.currentMode = currentMode;
    }

    public int getMax15minPeak() {
        return max15minPeak;
    }

    public void setMax15minPeak(int max15minPeak) {
        this.max15minPeak = max15minPeak;
    }

    public int getChargeLimitFromGrid() {
        return chargeLimitFromGrid;
    }

    public void setChargeLimitFromGrid(int chargeLimitFromGrid) {
        this.chargeLimitFromGrid = chargeLimitFromGrid;
    }
}
