package evcharging;

import evcharging.impl.tesla.TeslaException;
import evcharging.services.EV;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/ev")
public class EVResource {
    @Inject
    EV ev;

    @GET
    @Path("chargeport/open")
    @Produces(MediaType.TEXT_PLAIN)
    public String openChargePort() throws TeslaException {
        boolean result = ev.openChargePortDoor();
        return "charge port door open = "+result;
    }


}