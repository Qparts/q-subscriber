package q.rest.subscriber.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import q.rest.subscriber.helper.KeyConstant;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.PublicKey;

public class JwtFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            String token = authorizationHeader.substring("Bearer".length()).trim();
            PublicKey key = KeyConstant.PUBLIC_KEY;
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            validateType(claims.get("typ"));
            validateAppCode(claims.get("appCode"));
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    public void validateType(Object type) throws Exception {

    }

    public void validateAppCode(Object appCode) throws Exception {
        Integer ac = (Integer) appCode;
        //verify ac has access
    }
}
