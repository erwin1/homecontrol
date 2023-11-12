package homecontrol.resources;


import homecontrol.services.ev.EVException;
import homecontrol.services.ev.ElectricVehicle;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.logging.Logger;

@Path("/ev")
public class EVResource {
    public static final Logger LOGGER = Logger.getLogger(EVResource.class.getName());

    @Inject
    ElectricVehicle ev;

    @GET
    @Path("chargeport/open")
    @Produces(MediaType.TEXT_PLAIN)
    public String openChargePort() throws EVException {
        ev.openChargePortDoor().subscribe()
                .with(r -> {
                    LOGGER.info("Charge port opened: "+r);
                }, e -> {
                    LOGGER.info("Error opening charge port: "+e);
                });
        return "charge_port_door_open";
    }


}