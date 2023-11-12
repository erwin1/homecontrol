# homecontrol

## What?

Use power metrics from
 - Solar Iverter
 - EV Charger
 - Smart power meter

to
- Control EV Charging for optimal power usage
- Monitor live and historical power usage
- Get alerts e.g. high usage

Currently this project only supports intervers and EV chargers from SMA and a Smart 
power meter with a P1 port. To control EV charging, only Tesla is currently supported.
However, this project is set up so that new device are easily pluggable. Get in touch if
you're interested in adding more device implementations.

## Why?

The most important reason to start this project was the introduction of 
the "capacity tariff" in Flanders (Belgium), which changed the way electricity 
consumption was charged. Part of the tariff is now based on how much
electricity is used at the same time. 

EV charging is obviously a major electrity consumer, but luckily it is also
relatively easy to control, which is where this project comes in.

## Get started

### Configure

Make sure to configure these settings in `.env`:
```
#SMA
EVCHARGING_CHARGER_IP=...
EVCHARGING_CHARGER_USERNAME=...
EVCHARGING_CHARGER_PASSWORD=...
EVCHARGING_INVERTER_IP=...
EVCHARGING_INVERTER_PASSWORD=...
#TESLA
EVCHARGING_TESLA_REFRESHTOKEN=...
EVCHARGING_TESLA_VEHICLE=...vehicle id...
#SLACK
EVCHARGING_SLACK_URL=...
EVCHARGING_SLACK_CHANNEL=...
#HOMEWIZARD
EVCHARGING_HWEP1_IP=....
METRICSLOGGER_DATA=...path/to/data...
```

*SMA*

Find the local IP addresses of your inverter and charger and the username and passwords of their admin web interfaces.

*Tesla*
To obtain a refresh token, please refer to [detailed information about Tesla API authentication](https://tesla-api.timdorr.com/api-basics/authentication) or use a tool like [Tesla Access Token Generator](https://chrome.google.com/webstore/detail/tesla-access-token-genera/kokkedfblmfbngojkeaepekpidghjgag)

*HomeWizard*

Check [this page](https://homewizard-energy-api.readthedocs.io/discovery.html) on how to find the local IP address.

*Slack*

This is optional. When configured, this slack webhook will be used to send useful notifications.

### Run

Build and run:

```shell
./mvnw quarkus:run
```

Or package and run with Java:

```shell
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Once running, you can visit:

- Live dashboard: http://localhost:8080
- Historical data: http://localhost:8080/details.html
- Config: http://localhost:8080/config
