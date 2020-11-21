package q.rest.subscriber.operation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import q.rest.subscriber.dao.DAO;
import q.rest.subscriber.filter.annotation.ValidApp;
import q.rest.subscriber.helper.AppConstants;
import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.helper.InternalAppRequester;
import q.rest.subscriber.helper.KeyConstant;
import q.rest.subscriber.model.MessagingModel;
import q.rest.subscriber.model.SignupModel;
import q.rest.subscriber.model.WebApp;
import q.rest.subscriber.model.entity.*;
import q.rest.subscriber.model.entity.role.general.GeneralRole;
import q.rest.subscriber.model.publicapi.PbCompany;
import q.rest.subscriber.model.publicapi.PbLoginObject;
import q.rest.subscriber.model.publicapi.PbSubscriber;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/api/v2/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiV2 {
    @EJB
    private DAO dao;
    @EJB
    private AsyncService async;

    @POST
    @Path("login")
    @ValidApp
    public Response login(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        WebApp webApp = this.getWebAppFromAuthHeader(header);
        String password = Helper.cypher(map.get("password"));
        String email = map.get("email").trim().toLowerCase();
        String ip = map.get("ipAddress");
        Subscriber subscriber = dao.findTwoConditions(Subscriber.class, "email", "password", email, password);
        Company company = dao.find(Company.class, subscriber.getCompanyId());
        verifyLogin(company, subscriber, email, ip);
        PbLoginObject loginObject = getLoginObject(subscriber, webApp.getAppCode());
        String refreshJwt = issueRefreshToken(subscriber.getCompanyId(), subscriber.getId(), webApp.getAppCode());//long one
        loginObject.setRefreshJwt(refreshJwt);
        return Response.ok().entity(loginObject).build();
    }

    @POST
    @Path("signup")
    @ValidApp
    public Response signup(SignupModel sm){
        verifyAvailability(sm.getEmail(), sm.getMobile());//returns 409
        SignupRequest signupRequest = new SignupRequest(sm);
        dao.persist(signupRequest);
        //generate code !
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(signupRequest, sm.getCountryId() == 1 ? 'M' : 'E', code);
        dao.persist(sv);
        sendMessagingNotification(sm, sv.getVerificationCode());
        return Response.status(200).build();
    }


    @ValidApp
    @POST
    @Path(value = "verify-signup")
    public Response verifySignup(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        WebApp webApp = getWebAppFromAuthHeader(header);
        String code = map.get("code");
        String email = map.get("email");
        String ip = "from app";
        Date date = Helper.addMinutes(new Date(), -60);
        String sql = "select b from SubscriberVerification b where b.verificationCode = :value0 and b.created > :value1 and stage = :value2 and b.signupRequestId in (" +
                "select c.id from SignupRequest c where c.email = :value3)";
        SubscriberVerification verification = dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 1, email.trim().toLowerCase());
        verifyObjectFound(verification);
        SignupRequest sr = dao.find(SignupRequest.class, verification.getSignupRequestId());
        verifyObjectFound(sr);
        //create company
        Map<String, Integer> planIds = getBasicPlanId();
        int planId = planIds.get("planId");
        int durationId = planIds.get("durationId");
        int roleId = planIds.get("roleId");
        GeneralRole role = dao.find(GeneralRole.class, roleId);
        Company company = new Company(sr, verification.getVerificationMode(), planId, durationId, role);
        dao.persist(company);
        sr.setStatus('C');
        dao.update(sr);
        dao.delete(verification);
        Subscriber subscriber = company.getSubscribers().iterator().next();
        subscriber = dao.find(Subscriber.class, subscriber.getId());
        verifyLogin(company, subscriber, subscriber.getEmail(), ip);
        PbLoginObject loginObject = getLoginObject(subscriber, webApp.getAppCode());
        String refreshJwt = issueRefreshToken(subscriber.getCompanyId(), subscriber.getId(), webApp.getAppCode());//long one
        loginObject.setRefreshJwt(refreshJwt);
        return Response.status(200).entity(loginObject).build();

    }

    @POST
    @Path("refresh-token")
    public Response refresh(Map<String, String> map) {
        String shortToken = map.get("token");
        String refreshToken = map.get("refreshToken");
        int[] result = verifyTokensPair(shortToken, refreshToken);
        int subId = result[0];
        int appCode = result[1];
        String sql = "select b from RefreshToken b where b.subscriberId = :value0 " +
                " and b.appCode = :value1" +
                " and b.token = :value2" +
                " and b.status = :value3" +
                " and b.expiresAt > :value4";
        RefreshToken rt = dao.findJPQLParams(RefreshToken.class, sql, subId, appCode, refreshToken, 'A', new Date());
        if (rt == null) throwError(401);
        Subscriber sub = dao.find(Subscriber.class, subId);
        PbLoginObject loginObject = getLoginObject(sub, appCode);
        return Response.status(200).entity(loginObject).build();
    }

    private int[] verifyTokensPair(String shortToken, String refreshToken) {
        Claims shortClaims = getJWTClaimsEvenIfExpired(shortToken);
        Claims refreshClaims = getJWTClaimsEvenIfExpired(refreshToken);
        if (!shortClaims.get("sub").toString().equals(refreshClaims.get("sub"))) throwError(401);
        if (shortClaims.get("appCode") != refreshClaims.get("appCode")) throwError(401);
        if (!refreshClaims.get("typ").toString().equals("R")) throwError(401);
        if (!shortClaims.get("typ").toString().equals("S")) throwError(401);
        int subId = Integer.parseInt(shortClaims.get("sub").toString());
        int appCode = Integer.parseInt(shortClaims.get("appCode").toString());
        return new int[]{subId, appCode};
    }

    @POST
    @Path("logout")
    //   @SubscriberJwt
    public Response logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String header) {
        int appCode = getAppCodeFromJWT(header);
        int subscriberID = getSubscriberFromJWT(header);
        killTokens(subscriberID, appCode);
        return Response.ok().build();
    }


    private PbLoginObject getLoginObject(Subscriber subscriber, int appCode) {
        subscriber = updateSubscriptionStatus(subscriber);
        PbCompany pbCompany = dao.find(PbCompany.class, subscriber.getCompanyId());
        PbSubscriber pbSubscriber = dao.find(PbSubscriber.class, subscriber.getId());
        String jwt = issueToken(subscriber.getCompanyId(), subscriber.getId(), appCode);//short one
        List<Integer> activities = extractActivities(pbSubscriber);
        return new PbLoginObject(pbCompany, pbSubscriber, jwt, null, activities);
    }

    private List<Integer> extractActivities(PbSubscriber subscriber) {
        List<Integer> activities = new ArrayList<>();
        for (var role : subscriber.getRoles()) {
            for (var act : role.getActivities()) {
                activities.add(act.getId());
            }
        }
        return activities;
    }


    private String issueToken(int companyId, int userId, int appCode) {
        try {
            Date issued = new Date();
            Date expire = Helper.addMinutes(issued, 10);
            Map<String, Object> map = new HashMap<>();
            map.put("typ", 'S');
            map.put("appCode", appCode);
            map.put("comp", companyId);
            return KeyConstant.issueToken(userId, map, issued, expire);
        } catch (Exception ex) {
            throwError(500, "Token issuing error");
            return null;
        }
    }


    private String issueRefreshToken(int companyId, int userId, int appCode) {
        try {
            Map<String, Object> map = new HashMap<>();
            Date issued = new Date();
            Date expire = Helper.addDays(issued, 365);
            map.put("typ", 'R');
            map.put("appCode", appCode);
            map.put("comp", companyId);
            String jwt = KeyConstant.issueToken(userId, map, issued, expire);
            saveRefreshToken(jwt, userId, issued, expire, appCode);
            return jwt;
        } catch (Exception ex) {
            throwError(500, "Token issuing error");
            return null;
        }
    }

    private void killTokens(int userId, int appCode) {
        String sql = "select b from RefreshToken b where b.subscriberId = :value0 and b.status = :value1 and b.appCode = :value2";
        List<RefreshToken> oldTokens = dao.getJPQLParams(RefreshToken.class, sql, userId, 'A', appCode);
        for (var ort : oldTokens) {
            ort.setStatus('K');//kill previous tokens
            dao.update(ort);
        }
    }

    private void saveRefreshToken(String jwt, int userId, Date issued, Date expire, int appCode) {
        killTokens(userId, appCode);
        RefreshToken rt = new RefreshToken();
        rt.setSubscriberId(userId);
        rt.setIssuedAt(issued);
        rt.setExpiresAt(expire);
        rt.setStatus('A');
        rt.setAppCode(appCode);
        rt.setToken(jwt);
        dao.persist(rt);
    }

    private Subscriber updateSubscriptionStatus(Subscriber subscriber) {
        Subscription activeSubscription = dao.findTwoConditions(Subscription.class, "companyId", "status", subscriber.getCompanyId(), 'A');
        if (activeSubscription != null) {
            if (activeSubscription.getEndDate().before(new Date())) {
                activeSubscription.setStatus('E');
                dao.update(activeSubscription);
                //check if there is future make it active
                String jpql = "select b from Subscription b where b.companyId = :value0 and b.status = :value1 and b.startDate < :value2";
                Subscription futureSubscription = dao.findJPQLParams(Subscription.class, jpql, subscriber.getCompanyId(), 'F', new Date());
                if (futureSubscription != null) {
                    futureSubscription.setStatus('A');
                    dao.update(futureSubscription);
                } else {
                    //downgrade
                    Company company = dao.find(Company.class, subscriber.getCompanyId());
                    company.getActiveSubscription().getPlanId();
                    Map<String, Integer> planIds = getBasicPlanId();
                    int planId = planIds.get("planId");
                    int durationId = planIds.get("durationId");
                    int roleId = planIds.get("roleId");
                    GeneralRole role = dao.find(GeneralRole.class, roleId);
                    for (Subscriber s : company.getSubscribers()) {
                        s.getRoles().clear();
                        s.getRoles().add(role);
                        dao.update(s);
                        if (subscriber.getId() == s.getId()) {
                            subscriber = s;
                        }
                    }
                }
            }
        }
        return subscriber;
    }


    private Map<String, Integer> getBasicPlanId() {
        Response r = InternalAppRequester.getSecuredRequest(AppConstants.GET_BASIC_PLAN_ID);
        if (r.getStatus() == 200) {
            return r.readEntity(Map.class);
        }
        throwError(500);
        return null;
    }

    private void verifyLogin(Company company, Subscriber subscriber, String email, String ip) {
        if(company.getStatus() != 'A' || subscriber.getStatus() != 'A'){
            throwError(404, "Invalid credentials");
        }
        if (subscriber == null) {
            async.createLoginAttempt(email, 0, ip, false);
            throwError(404, "Invalid credentials");
        } else {
            async.createLoginAttempt(email, subscriber.getId(), ip, true);
        }
    }


    private WebApp getWebAppFromAuthHeader(String authHeader) {
        try {
            String appSecret = authHeader.substring("Bearer".length()).trim();
            return getWebAppFromSecret(appSecret);
        } catch (Exception ex) {
            throwError(401, "invalid secret");
            return null;
        }
    }


    // retrieves app object from app secret
    private WebApp getWebAppFromSecret(String secret) throws Exception {
        // verify web app secret
        WebApp webApp = dao.findTwoConditions(WebApp.class, "appSecret", "active", secret, true);
        if (webApp == null) {
            throw new Exception();
        }
        return webApp;
    }


    public <T> Response getSecuredRequest(String link, String authHeader) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, authHeader);
        Response r = b.get();
        return r;
    }


    public void throwError(int code) {
        throwError(code, null);
    }

    public void throwError(int code, String msg) {
        throw new WebApplicationException(
                Response.status(code).entity(msg).build()
        );
    }

    public int getSubscriberFromJWT(String header) {
        String token = header.substring("Bearer".length()).trim();
        Claims claims = Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
        return Integer.parseInt(claims.get("sub").toString());
    }

    public int getSubscriberFromJWTEvenIfExpired(String header) {
        try {
            String token = header.substring("Bearer".length()).trim();
            Claims claims = Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
            return Integer.parseInt(claims.get("sub").toString());
        } catch (ExpiredJwtException e) {
            return Integer.parseInt(e.getClaims().get("sub").toString());
        }
    }

    public Claims getJWTClaimsEvenIfExpired(String header) {
        try {
            String token = header;
            if (header.startsWith("Bearer")) {
                token = header.substring("Bearer".length()).trim();
            }
            return Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public int getAppCodeFromJWTEvenIfExpired(String header) {
        try {
            String token = header;
            if (header.startsWith("Bearer")) {
                token = header.substring("Bearer".length()).trim();
            }
            Claims claims = Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
            return Integer.parseInt(claims.get("appCode").toString());
        } catch (ExpiredJwtException e) {
            return Integer.parseInt(e.getClaims().get("sub").toString());
        }
    }


    public int getAppCodeFromJWT(String header) {
        String token = header;
        if (header.startsWith("Bearer")) {
            token = header.substring("Bearer".length()).trim();
        }
        Claims claims = Jwts.parserBuilder().setSigningKey(KeyConstant.PUBLIC_KEY).build().parseClaimsJws(token).getBody();
        return Integer.parseInt(claims.get("appCode").toString());
    }



    private void verifyAvailability(String email, String mobile) {
        String sql = "select b from Subscriber b where (b.mobile =:value0 or b.email =:value1)";
        List<Subscriber> check = dao.getJPQLParams(Subscriber.class, sql, mobile, email);
        if (!check.isEmpty()) {
            throwError(409, "Subscriber already registered");
        }

        sql = "select b from SignupRequest b where (b.mobile =:value0 or b.email =:value1) and b.status = :value2 and b.created > :value3";
        List<SignupRequest> check2 = dao.getJPQLParams(SignupRequest.class, sql, mobile, email, 'R', Helper.addMinutes(new Date(), -60));
        if (!check2.isEmpty()) {
            throwError(409, "Signup request already sent! try again in an hour");
        }
    }


    private String createVerificationCode() {
        String code = "";
        boolean available = false;
        do {
            code = String.valueOf(Helper.getRandomInteger(1000, 9999));
            Date date = Helper.addMinutes(new Date(), -60);
            String sql = "select b from SubscriberVerification b where b.verificationCode = :value0 and b.created >= :value1";
            List<SubscriberVerification> l = dao.getJPQLParams(SubscriberVerification.class, sql, code, date);
            if (l.isEmpty()) {
                available = true;
            }
        } while (!available);
        return code;
    }


    private void sendMessagingNotification(SignupModel sm, String code){
        if (sm.getCountryId() == 1) {
            MessagingModel smsModel = new MessagingModel(sm.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, code);
            async.sendSms(smsModel);
        } else {
            String[] s = new String[]{sm.getName(), code};
            MessagingModel emailModel = new MessagingModel(null, sm.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
        }
    }


    private void verifyObjectFound(Object object) {
        if (object == null) {
            throwError(404);
        }
    }


}
