package eu.ill.webx.controllers;

import eu.ill.webx.model.AuthenticationToken;
import eu.ill.webx.services.AuthService;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.List;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    public AuthController() {
    }

    @POST
    public Response auth(@Context HttpHeaders headers) {
        List<String> authHeaders = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (authHeaders.size() != 1) {
            return Response.status(401).build();
        }

        // Convert from base64
        String credentialsBase64 = authHeaders.get(0).substring(6);
        byte[] credentialsBytes = Base64.getUrlDecoder().decode(credentialsBase64);
        String credentials = new String(credentialsBytes);

        // Store credentials and get the token
        String token = AuthService.instance().addAuthorisation(credentials);
        return Response.status(200).entity(new AuthenticationToken(token)).build();
    }
}
