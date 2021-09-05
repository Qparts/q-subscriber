package q.rest.subscriber.operation;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import q.rest.subscriber.dao.DaoApi;
import q.rest.subscriber.filter.annotation.SubscriberJwt;
import q.rest.subscriber.filter.annotation.ValidApp;
import q.rest.subscriber.helper.AppConstants;
import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.helper.InternalAppRequester;
import q.rest.subscriber.helper.KeyConstant;
import q.rest.subscriber.model.*;
import q.rest.subscriber.model.entity.*;
import q.rest.subscriber.model.entity.role.company.CompanyRole;
import q.rest.subscriber.model.publicapi.*;

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
    private AsyncService async;
    @EJB
    private DaoApi daoApi;

    @POST
    @Path("login")
    @ValidApp
    public Response login(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        WebApp webApp = this.getWebAppFromAuthHeader(header);
        String password = Helper.cypher(map.get("password"));
        String email = map.get("email").trim().toLowerCase();
        String host = map.get("host").trim().toLowerCase();
        String ip = map.get("ipAddress");
        Subscriber subscriber = daoApi.findSubscriber(email, password);
        verifyLogin(subscriber, email, ip);
        PbLoginObject loginObject = getLoginObject(subscriber, webApp.getAppCode());
        String refreshJwt = issueRefreshToken(subscriber.getCompanyId(), subscriber.getId(), webApp.getAppCode());//long one
        loginObject.setRefreshJwt(refreshJwt);
        return Response.ok().entity(loginObject).build();
    }


    @POST
    @Path("signup")
    @ValidApp
    public Response signup(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, SignupModel sm){
        verifyAvailability(sm.getEmail(), sm.getMobile());//returns 409
        var appCode = getWebAppFromAuthHeader(header).getAppCode();
        var signupRequest = daoApi.createSignupRequest(sm, appCode);
        var code = daoApi.createAndGetVerificationCode(signupRequest, sm.getCountryId());
        sendMessagingNotification(sm, code);
        return Response.status(200).build();
    }

    @GET
    @Path("company/{id}")
    @SubscriberJwt
    public Response getCompany(@PathParam(value = "id") int id) {
        var company = daoApi.findPbCompany(id);
        if (company == null) throwError(404);
        return Response.status(200).entity(company).build();
    }

    @GET
    @SubscriberJwt
    @Path("company/search-name/{name}")
    public Response getCompanyByName(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, @PathParam(value = "name") String name){
        int companyId = Helper.getCompanyFromJWT(header);
        var companies = daoApi.searchCompaniesByName(name, companyId);
        return Response.status(200).entity(companies).build();
    }

    @GET
    @SubscriberJwt
    @Path("company/{id}/subscribers")
    public Response getCompanySubscribers(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, @PathParam(value = "id") int targetCompanyId){
        var subscribers  =  daoApi.getCompanyVisibleSubscriber(targetCompanyId);
        return Response.status(200).entity(subscribers).build();
    }

    @GET
    @Path("companies/{ids}")
    @SubscriberJwt
    public Response getCompanies(@PathParam(value = "ids") String ids) {
        var companies = daoApi.getCompaniesByIds(ids);
        return Response.status(200).entity(companies).build();
    }

    @GET
    @Path("companies/{ids}/visible")
    @SubscriberJwt
    public Response getCompaniesVisible(@PathParam(value = "ids") String ids) {
        var companies = daoApi.getVisibleCompaniesByIds(ids);
        return Response.status(200).entity(companies).build();
    }

    @ValidApp
    @POST
    @Path(value="verify-signup")
    public Response verifySignupToPending(Map<String, String> map){
        String code = map.get("code");
        String email = map.get("email").toLowerCase().trim();
        Date date = Helper.addMinutes(new Date(), -60);
        SignupRequest signupRequest = daoApi.getSignupRequest(date, email);
        verifyObjectFound(signupRequest);
        var verification = daoApi.getSubscriberVerification(code, date, signupRequest.getId());
        verifyObjectFound(verification);
        signupRequest.setStatus('P');//pending
        signupRequest.setEmailVerified(verification.getVerificationMode() == 'E');
        signupRequest.setMobileVerified(verification.getVerificationMode() == 'M');
        daoApi.deleteVerification(verification);
        daoApi.updateSignupRequest(signupRequest);
        async.informAdminsNewRegistration(signupRequest.getCompanyName());
        return Response.status(200).build();
    }

    @ValidApp
    @POST
    @Path("reset-password")
    public Response requestPasswordReset(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        var appCode = getWebAppFromAuthHeader(header).getAppCode();
        String email = map.get("email").trim().toLowerCase();
        var subscriber = daoApi.findSubscriberByEmail(email);
        verifyObjectFound(subscriber);
        String token = daoApi.createPasswordResetObject(subscriber.getId());
        System.out.println("app code for resetting password " + appCode);
        String[] values = new String[]{token, subscriber.getName(), appCode == 8 ? "qvm" : "qstock"};
        MessagingModel emailModel = new MessagingModel(null, subscriber.getEmail(), AppConstants.MESSAGING_PURPOSE_PASS_RESET, values);
        async.sendEmail(emailModel);
        return Response.status(200).build();
    }


    @ValidApp
    @PUT
    @Path("reset-password")
    public Response resetPassword(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        String token = map.get("code");
        String password = map.get("newPassword");
        PasswordReset pr = daoApi.findPasswordResetObject(token);
        verifyObjectFound(pr);
        daoApi.resetSubscriberPassword(pr, password);
        return Response.status(200).build();
    }


    @ValidApp
    @GET
    @Path("reset-password/{token}")
    public Response verifyPasswordReset(@PathParam(value = "token") String token) {
        PasswordReset pr = daoApi.findPasswordResetObject(token);
        verifyObjectFound(pr);
        return Response.status(200).build();
    }



    @POST
    @Path("refresh-token")
    public Response refresh(Map<String, String> map) {
        String shortToken = map.get("token");
        String refreshToken = map.get("refreshToken");
        int[] result = verifyTokensPair(shortToken, map.get("refreshToken"));
        int subscriberId = result[0];
        int appCode = result[1];
        var foundRefreshToken = daoApi.findRefreshToken(refreshToken, subscriberId, appCode);
        if (foundRefreshToken == null) throwError(401);

        var subscriber = daoApi.findSubscriber(subscriberId);
        PbLoginObject loginObject = getLoginObject(subscriber, appCode);
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
    @SubscriberJwt
    public Response logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String header) {
        int appCode = getAppCodeFromJWT(header);
        int subscriberId = Helper.getSubscriberFromJWT(header);
        daoApi.killTokens(subscriberId, appCode);
        return Response.ok().build();
    }

    @POST
    @Path("branch")
    @SubscriberJwt
    public Response createBranch(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Branch branch) {
        int companyId = Helper.getCompanyFromJWT(header);
        int subscriberId = Helper.getSubscriberFromJWT(header);
        var pbBranch = daoApi.createBranch(branch, companyId, subscriberId);
        return Response.status(200).entity(pbBranch).build();
    }

    @PUT
    @Path("invoice-template")
    @SubscriberJwt
    public Response updateInvoiceTemplate(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,String> map){
        int companyId = Helper.getCompanyFromJWT(header);
        var template = map.get("invoiceTemplate");
        daoApi.updateInvoiceTemplate(template, companyId);
        return Response.status(200).build();
    }


    @PUT
    @Path("default-currency")
    @SubscriberJwt
    public Response updateDefaultCurrency(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,String> map){
        int companyId = Helper.getCompanyFromJWT(header);
        var currency = map.get("currency");
        daoApi.updateDefaultCurrency(currency, companyId);
        return Response.status(200).build();
    }


    @PUT
    @Path("logo-upload")
    @SubscriberJwt
    public Response updateLogoUploaded(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,Boolean> map){
        int companyId = Helper.getCompanyFromJWT(header);
        Boolean uploaded = map.get("logoUploaded");
        daoApi.updateLogoUploaded(uploaded, companyId);
        return Response.status(200).build();
    }

    @Path("default-policy")
    @PUT
    @SubscriberJwt
    public Response makeDefaultPolicy(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,Integer> map){
        int companyId = Helper.getCompanyFromJWT(header);
        int policyId = map.get("policyId");
        daoApi.makeDefaultPolicy(policyId, companyId);
        return Response.status(200).build();
    }

    @Path("company-role")
    @POST
    @SubscriberJwt
    public Response createCompanyRole(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, CompanyRole role){
        int companyId = Helper.getCompanyFromJWT(header);
        daoApi.createCompanyRole(role, companyId);
        return Response.status(200).build();
    }

    @Path("default-customer")
    @PUT
    @SubscriberJwt
    public Response makeDefaultCustomer(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,Integer> map){
        int companyId = Helper.getCompanyFromJWT(header);
        int customerId = map.get("customerId");
        daoApi.makeDefaultCustomer(customerId, companyId);
        return Response.status(200).build();
    }


    @SubscriberJwt
    @Path("default-branch")
    @PUT
    public Response makeBranchDefault(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,Integer> map){
        int companyId = Helper.getCompanyFromJWT(header);
        int branchId = map.get("branchId");
        daoApi.makeDefaultBranch(companyId, branchId);
        return Response.status(200).build();
    }


    @SubscriberJwt
    @PUT
    @Path("setting-variables")
    public Response getPolicies(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,Object> map){
        int companyId = Helper.getCompanyFromJWT(header);
        String vatNumber = (String) map.get("vatNumber");
        if(vatNumber != null)
            daoApi.updateVatNumber(vatNumber, companyId);

        Double purchaseTax = (Double) map.get("defaultPurchaseTax");
        if(purchaseTax != null)
            daoApi.updateDefaultPurchaseTax(purchaseTax, companyId);

        Double salesTax = (Double) map.get("defaultSalesTax");
        if(salesTax != null)
           daoApi.updateDefaultSalesTax(salesTax, companyId);

        return Response.status(200).build();
    }

    @GET
    @SubscriberJwt
    @Path("branches/ids")
    public Response getBranchIds(@HeaderParam(HttpHeaders.AUTHORIZATION) String header){
        int companyId = Helper.getCompanyFromJWT(header);
        var ints = daoApi.getBranchIds(companyId);
        Map<String,Object> map = new HashMap<>();
        map.put("branchIds", ints);
        return Response.status(200).entity(map).build();
    }

    @SubscriberJwt
    @POST
    @Path("subscriber")
    public Response addNewSubscriber(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, AddSubscriberModel model) {
        verifyAvailability(model.getEmail(), model.getMobile());//returns 409
        int companyId = Helper.getCompanyFromJWT(header);
        int subscriberId = Helper.getSubscriberFromJWT(header);
        SignupRequest signupRequest = daoApi.createAdditionalSubscriberSignupRequest(model, companyId, subscriberId);
        String code = daoApi.createAndGetVerificationCode(signupRequest, model.getCountryId(), model.getCompanyId());

        if (model.getCountryId() == 1) {
            MessagingModel smsModel = new MessagingModel(model.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, code);
            async.sendSms(smsModel);
        } else {
            String[] s = new String[]{model.getName(), code};
            MessagingModel emailModel = new MessagingModel(null, model.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("mode", model.getCountryId() == 1 ? "mobile" : "email");
        return Response.status(200).entity(map).build();
    }

    @SubscriberJwt
    @PUT
    @Path("verify-subscriber")
    public Response verifyAdditionalSubscriber(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, Object> map) {
        String code = (String) map.get("code");
        int companyId = Helper.getCompanyFromJWT(header);
        var verification = daoApi.getSubscriberVerification(code, companyId);
        verifyObjectFound(verification);
        var pbSubscriber = daoApi.createAdditionalSubscriber(companyId, verification);
        return Response.status(200).entity(pbSubscriber).build();
    }

    @SubscriberJwt
    @GET
    @Path("dashboard-metrics-allowed")
    public Response isDashboardMetricsAllowed(@HeaderParam(HttpHeaders.AUTHORIZATION) String header){
        int subscriberId = Helper.getSubscriberFromJWT(header);
        if(daoApi.isDashboardMetricsAllowed(subscriberId)){
            return Response.status(201).build();
        }
        return Response.status(403).build();
    }

    @SubscriberJwt
    @GET
    @Path("is-search-unlimited")
    public Response isPremium(@HeaderParam(HttpHeaders.AUTHORIZATION) String header){
        int subscriberId = Helper.getSubscriberFromJWT(header);
        if(daoApi.isSearchPartsUnlimited(subscriberId)){
            return Response.status(201).build();
        }
        return Response.status(403).build();
    }

    @SubscriberJwt
    @GET
    @Path("verify-product-search-count")
    public Response verifySearchCount(@HeaderParam(HttpHeaders.AUTHORIZATION) String header) {
        int companyId = Helper.getCompanyFromJWT(header);
        int subscriberId = Helper.getSubscriberFromJWT(header);
        if(daoApi.isSearchPartsUnlimited(subscriberId)){
            return Response.status(201).build();
        }
        if (daoApi.getNumberOfPartsSearchUsed(companyId) >= 10) {
            return Response.status(403).build();
        }
        return Response.status(201).build();
    }

    @SubscriberJwt
    @GET
    @Path("product-search-count")
    public Response getProductSearchCount(@HeaderParam(HttpHeaders.AUTHORIZATION) String header){
        int companyId = Helper.getCompanyFromJWT(header);
        int number = daoApi.getNumberOfPartsSearchUsed(companyId);
        Map<String,Integer> map = new HashMap<>();
        map.put("count", number);
        return Response.status(200).entity(map).build();
    }

    @SubscriberJwt
    @GET
    @Path("most-searched-keywords")
    public Response getMostSearchedKeywords(){
        List<Map<String, Object>> list = daoApi.getMostSearchedKeywords();
        return Response.status(200).entity(list).build();
    }

    @SubscriberJwt
    @GET
    @Path("verify-product-replacement-search-count")
    public Response verifyReplacementSearchCount(@HeaderParam(HttpHeaders.AUTHORIZATION) String header) {
        int companyId = Helper.getCompanyFromJWT(header);
        int subscriberId = Helper.getSubscriberFromJWT(header);

        boolean isUnlimited = daoApi.isSearchReplacementsUnlimited(subscriberId);
        if(isUnlimited)
            return Response.status(201).build();

        int numberOfSearches = daoApi.getNumberOfReplacementSearchUsed(companyId);
        if (numberOfSearches >= 5)
            return Response.status(403).build();

        return Response.status(201).build();
    }

    private PbLoginObject getLoginObject(Subscriber subscriber, int appCode) {
        subscriber = updateSubscriptionStatus(subscriber);
        var companyView = daoApi.findCompanyView(subscriber.getCompanyId());
        var pbSubscriber = daoApi.findPbSubscriber(subscriber.getId());
        String jwt = issueToken(subscriber.getCompanyId(), subscriber.getId(), appCode);//short one
        List<Integer> activities = extractActivities(pbSubscriber);
        return new PbLoginObject(companyView, pbSubscriber, jwt, null, activities);
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
            Date expire = Helper.addMinutes(issued, 60*24*7);
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
            daoApi.saveRefreshToken(jwt, userId, issued, expire, appCode);
            return jwt;
        } catch (Exception ex) {
            throwError(500, "Token issuing error");
            return null;
        }
    }

    private Subscriber updateSubscriptionStatus(Subscriber subscriber) {
        Subscription activeSubscription = daoApi.getActiveSubscription(subscriber.getCompanyId());
        if (activeSubscription != null && activeSubscription.getEndDate().before(new Date())) {
            daoApi.doExpireSubscription(activeSubscription);
            //check if there is future make it active
            var futureSubscription = daoApi.getFutureSubscription(subscriber.getCompanyId());
            if (futureSubscription != null)
                daoApi.doActivateSubscription(futureSubscription);
            else {
                Map<String, Integer> planIds = getBasicPlanId();
                int roleId = planIds.get("roleId");
                subscriber = daoApi.downgradeSubscriber(subscriber, roleId);
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

    private void verifyLogin(Subscriber subscriber, String email, String ip) {
        if(subscriber == null || subscriber.getStatus() != 'A'){
            async.createLoginAttempt(email, 0, ip, false);
            throwError(404, "Invalid credentials");
        }
        var company = daoApi.findCompany(subscriber.getCompanyId());
        if(company.getStatus() != 'A'){
            async.createLoginAttempt(email, 0, ip, false);
            throwError(404, "Invalid credentials");
        } else {
            async.createLoginAttempt(email, subscriber.getId(), ip, true);
        }
    }

    private WebApp getWebAppFromAuthHeader(String authHeader) {
        try {
            String appSecret = authHeader.substring("Bearer".length()).trim();
            return daoApi.getWebAppFromSecret(appSecret);
        } catch (Exception ex) {
            throwError(401, "invalid secret");
            return null;
        }
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
        if (daoApi.subscriberExists(email, mobile))
            throwError(409, "Subscriber already registered");

        if(daoApi.signupRequestExists(email, mobile))
            throwError(409, "Signup request already sent! try again in an hour");
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
