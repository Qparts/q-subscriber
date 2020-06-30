package q.rest.subscriber.filter;

import q.rest.subscriber.filter.annotation.InternalApp;
import q.rest.subscriber.helper.AppConstants;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@InternalApp
@Provider
@Priority(Priorities.AUTHENTICATION)
public class InternalAppAgent implements ContainerRequestFilter {


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try{
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String appSecret = authorizationHeader.substring("Bearer".length()).trim();
            validateSecret(appSecret);
        }catch (Exception ex){
            requestContext.abortWith(Response.status(401).entity("Unauthorized Access").build());
        }
    }

    // retrieves app object from app secret
    private void validateSecret(String secret) throws Exception {
        if (!secret.equals(AppConstants.INTERNAL_APP_SECRET)){
            throw new Exception();
        }

    }
}
