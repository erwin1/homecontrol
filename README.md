# EV Charging

This project automates EV charging using the following constraints:
- use as much self-produced solar energy as possible
- limit grid usage at peak hours
- charge using power grid in off hours until a configured battery level 
- but keep the "15 minute peak" as low as possible (Belgian 'capaciteitstarief')

Example: using the following settings
- Mode: `OPTIMAL`
- Charge limit from grid: `50%`
- Peak strategy: `DYNAMIC_UNLIMITED`

The EV will charge up until 50% using grid power, dynmically limiting the 15 minute peak value to this month's current
peak value (so charging won't account for extra peak value). And next to that, use solar power only to charge the remaining capacity. 

On a high level, it works like this:

```mermaid
flowchart LR
start{{Run every minute}} --> checkconnected{Is charger connected?}
checkconnected --yes--> calcpower{Check charging mode}
peakhours --yes--> usepv[Calculate charging  power based<br>on current grid injection]
checklevel --above setting--> usepv
calcpower --mode: PV_ONLY--> usepv
calcpower --mode: OPTIMAL--> peakhours{In peak hours?}
peakhours --no--> checklevel{Check current<br>battery level}
checklevel --below setting--> useopt[Calculate charging power based on<br>maximum 15m peak usage]
usepv --> charge{{Compare calculated power<br>with current EV charging power<br>and make change accordingly}}
useopt --> charge
```

## Concepts

The correct functioning of this project relies on these concepts:
- EVCharger: to check if the car is connected
- ElectricityMeter: to retrieve current power meter data
- EV (electric vehicle): to start and stop charging and to change charging power

All three are abstracted in interfaces:

```mermaid
classDiagram
    class EV {
        +getCurrentBatteryLevel()
        +requestPowerConsumptionChange(power, limit)
    }
    
    class ElectricityMeter {
      +MeterData getCurrentValues()
      +MeterReading getCurrentMonthPeak(timestamp)
    }

    class EVCharger {
        +State getState()
    }
```

By default, they are implemented by
- EVCharger: connecting to an SMA charger using it's web admin UI (local IP).
- ElectricityMeter: there are 2 choices:
  - connecting to an SMA Inverter (Sunny Boy) using it's web admin UI (local IP).
  - connecting to a [HomeWizard](https://www.homewizard.com/nl-be/shop/wi-fi-p1-meter/) Wi-Fi P1 meter ([local IP](https://homewizard-energy-api.readthedocs.io/endpoints.html#data-points-for-hwe-p1))
- EV: connecting to the Tesla REST API

These implementations need configuration (like credentials). More on that below.

If you're interested in this project but have a different charger, electricity meter or EV, you can add another custom implementation for one or more of these interfaces.

This project is designed to be always on, e.g. on a Raspberry Pi, and run every minute. Alternatively it could also work if started for as long as the EV is connected to the charger.
The charger connection state can also be retrieved from the `EV` but it seemed better not having to query the EV every minute (even if not connected).


## Running the application in dev mode

This project is written in Java and uses Quarkus. You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.


## Status page

This project serves an HTTP status page using at:
http://your.local.ip:8080/status

It outputs current settings and power values:

```text
EVCharging Status

Operating mode	     OPTIMAL
Max 15 min peak	     4000W
Charger state	     Waiting
From grid	         344W
To grid	             0W
From PV	             0W
Total consumption	 344W
```

And it allows to switch charging mode between 3 states:
- OFF
- PV_ONLY
- OPTIMAL


## Configuration

### General

There are a number of important inital configuration settings that are set in `config/application.properties`:

- max15minpeak: EV charging will be limited so the 15 min average peak usage will be below this value
- min15minpeak: the minimum power peak that can always be used
- peakstrategy:
  - `DYNAMIC_LIMITED`: Dynamic peak based on current month usage, but limited to the configured max peak (max15minpeak).
  - `DYNAMIC_UNLIMITED`: Dynamic peak based on current month usage, not limited by the configured max peak (max15minpeak).
  - `STATIC_LIMITED`: Limited to max15minpeak.
- chargelimit-grid: Stop charging using grid power when this battery level is reached. Only solar energy will be used to charge the remaining capacity.
- mode: 
  - `OFF`: do nothing
  - `PV_ONLY`: only use power that was otherwise injected to the grid
  - `OPTIMAL`: in peak hours same as `PV_ONLY`. otherwise, use as much power as possible, but keep the 15 min average under the configured `max15minpeak` value.

```properties
max15minpeak=4000
min15minpeak=2500
peakstrategy=DYNAMIC_LIMITED
mode=PV_ONLY
chargelimit-grid=75
```

These values can be changed at runtime at the status page (see above).

Furthermore, it makes sense to enable FINE logging in the `.env` file:
```properties
quarkus.log.level=INFO
quarkus.log.category."evcharging".level=FINE
```

### ElectricityMeter implementation selection

To use `SMAInverter` as the `ElectricityMeter` implementation, add this setting in `.env`:

```properties
evcharging.meter=sma
```

To use HomeWizard `HWEP1` as the `ElectricityMeter` implementation, add this setting in `.env`:

```properties
evcharging.meter=hwep1
```

### HomeWizard

When using `HWEP1` as the `ElectricityMeter` implementation, this setting must be set in `.env`:

```properties
EVCHARGING_HWEP1_IP=x.x.x.x
```

### SMA Inverter

When using `SMAInverter` as the `ElectricityMeter` implementation, these settings must be set in `.env`:

```properties
EVCHARGING_INVERTER_IP=x.x.x.x
EVCHARGING_INVERTER_PASSWORD=...
```

### SMA Charger

When using `SMACharger` as the `EVCharger` implementation, these settings must be set in `.env`:

```properties
EVCHARGING_CHARGER_IP=x.x.x.x
EVCHARGING_CHARGER_USERNAME=...
EVCHARGING_CHARGER_PASSWORD=...
```

### Tesla

When using `TeslaEV` as the `EV` implementation, the tesla refreh token must be set in `.env`:

```properties
EVCHARGING_TESLA_REFRESHTOKEN=...
```

To obtain a refresh token, please refer to [detailed information about Tesla API authentication](https://tesla-api.timdorr.com/api-basics/authentication) or use a tool like [Tesla Access Token Generator](https://chrome.google.com/webstore/detail/tesla-access-token-genera/kokkedfblmfbngojkeaepekpidghjgag)


### Slack notification service

This project can send administrative notifications via slack. You need to add these settings to `.env` if you want to use it:

```properties
EVCHARGING_SLACK_URL=https://hooks.slack.com/services/...
EVCHARGING_SLACK_CHANNEL=...
```