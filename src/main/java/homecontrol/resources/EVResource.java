package homecontrol.resources;


import homecontrol.services.ev.EVException;
import homecontrol.services.ev.ElectricVehicle;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.logging.Logger;

@Path("/ev")
public class EVResource {
    public static final Logger LOGGER = Logger.getLogger(EVResource.class.getName());

    @Inject
    @Named("volvo")
    ElectricVehicle volvo;

    @Inject
    @Named("tesla")
    ElectricVehicle tesla;

    @GET
    @Path("chargeport/open")
    @Produces(MediaType.TEXT_PLAIN)
    public String openChargePort() throws EVException {
        tesla.openChargePortDoor().subscribe()
                .with(r -> {
                    LOGGER.info("Charge port opened: "+r);
                }, e -> {
                    LOGGER.info("Error opening charge port: "+e);
                });
        return "charge_port_door_open";
    }

    @GET
    @Path("debug/volvo")
    @Produces(MediaType.TEXT_PLAIN)
    public String debugvolvo() throws EVException {
        volvo.debug();
        return "ok";
    }

    @GET
    @Path("debug/tesla")
    @Produces(MediaType.TEXT_PLAIN)
    public String debugtesla() throws EVException {
        tesla.debug();
        return "ok";
    }


}