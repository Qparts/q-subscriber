package q.rest.subscriber.helper;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class InternalAppRequester {

    private static final String HEADER = "Bearer " + AppConstants.INTERNAL_APP_SECRET;


    public static Response getSecuredRequest(String link) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, HEADER);
        Response r = b.get();
        return r;
    }

    public static <T> Response postSecuredRequest(String link, T t) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, HEADER);
        Response r = b.post(Entity.entity(t, "application/json"));// not secured
        return r;
    }

    public static <T> Response putSecuredRequest(String link, T t) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, HEADER);
        Response r = b.put(Entity.entity(t, "application/json"));// not secured
        return r;
    }

    public static <T> Response deleteSecuredRequest(String link) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, HEADER);
        Response r = b.delete();// not secured
        return r;
    }

}
