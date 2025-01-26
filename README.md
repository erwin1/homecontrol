# homecontrol

## What?

Use power metrics from
 - Solar inverter
 - EV charger
 - Smart power meter

to
- Control EV Charging for optimal power usage
- Monitor live and historical power usage
- Get alerts e.g. high usage

Currently, this project only supports inverters and EV chargers from SMA and a Smart 
power meter with a P1 port. To control EV charging, only Tesla is currently supported.
However, this project is set up so that new device are easily pluggable. Get in touch if
you're interested in adding more device implementations.

![Overview](/docs/homecontrol-overview.png)

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
EVCHARGING_TESLA_VEHICLE_VIN=...
EVCHARGING_TESLA_KEY_NAME=...
EVCHARGING_TESLA_TOKEN_NAME=...
EVCHARGING_TESLA_CACHE_FILE=...
EVCHARGING_TESLA_COMMAND_SDK=...
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

This tool communicates with a Tesla vehicle via BLE using the `tesla-control` SDK from [this repository](https://github.com/teslamotors/vehicle-command).
Using BLE requires you to run this tool on a bluetooth-capable device in range of the vehicle. 

Follow [these instructions](https://github.com/teslamotors/vehicle-command?tab=readme-ov-file#installing-locally) to install the `tesla-control` tool locally.

Then follow the [instructions to configure](https://github.com/teslamotors/vehicle-command/blob/main/cmd/tesla-control/README.md)  the Tesla SDK. 
This involves generating a key pair on the bluetooth device you will be running this tool from, and allowing access to it inside your vehicle (by tapping your keycard when requested).

Make sure to run some commands to make sure everything works as expected. e.g.

```shell
./tesla-control -ble -debug wake
./tesla-control -ble -debug honk
```

If your car honks, it works!

Then make sure to configure the path of the command SDK in `.env` as `EVCHARGING_TESLA_COMMAND_SDK`, together with the other 
env variables the Tesla SDK requires: `EVCHARGING_TESLA_VEHICLE_VIN`, `EVCHARGING_TESLA_KEY_NAME` and  `EVCHARGING_TESLA_TOKEN_NAME`.

*HomeWizard*

Check [this page](https://api-documentation.homewizard.com/docs/discovery) on how to find the local IP address.

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
