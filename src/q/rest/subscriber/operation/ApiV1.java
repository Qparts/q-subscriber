package q.rest.subscriber.operation;

import org.jboss.logging.Logger;
import q.rest.subscriber.dao.DAO;
import q.rest.subscriber.filter.annotation.*;
import q.rest.subscriber.helper.AppConstants;
import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.helper.KeyConstant;
import q.rest.subscriber.helper.InternalAppRequester;
import q.rest.subscriber.model.*;
import q.rest.subscriber.model.entity.*;
import q.rest.subscriber.model.entity.keywords.SearchKeyword;
import q.rest.subscriber.model.entity.keywords.SearchLimit;
import q.rest.subscriber.model.entity.keywords.SearchReplacementKeyword;
import q.rest.subscriber.model.entity.role.general.GeneralActivity;
import q.rest.subscriber.model.entity.role.general.GeneralRole;
import q.rest.subscriber.model.publicapi.PbLoginObject;
import q.rest.subscriber.model.publicapi.PbSubscriber;
import q.rest.subscriber.model.reduced.CompanyReduced;
import q.rest.subscriber.model.view.CompanyView;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.*;
import java.util.*;

@Path("/api/v1/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiV1 {

    private static final Logger logger = Logger.getLogger(ApiV1.class);

    @EJB
    private DAO dao;

    @EJB
    private AsyncService async;


    @UserJwt
    @PUT
    @Path("company")
    public Response updateCompany(Company company) {
        logger.info("update company");
        Company co = dao.find(Company.class, company.getId());
        company.setSubscribers(co.getSubscribers());//prevent password loss
        dao.update(company);
        logger.info("update company done");
        return Response.status(200).entity(company).build();
    }


    //ms-sub
    @UserSubscriberJwt
    @POST
    @Path("additional-subscriber-request")
    public Response addNewSubscriber(AddSubscriberModel model) {
        logger.info("additional subscriber request ");
        verifyAvailability(model.getEmail(), model.getMobile());//returns 409
        SignupRequest sr = new SignupRequest(model);
        dao.persist(sr);
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(sr, model.getCountryId() == 1 ? 'M' : 'E', code, model.getCompanyId());
        dao.persist(sv);
//        if (model.getCountryId() == 1) {
//            MessagingModel smsModel = new MessagingModel(model.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, sv.getVerificationCode());
//            async.sendSms(smsModel);
//        } else {
            String[] s = new String[]{model.getName(), sv.getVerificationCode()};
            MessagingModel emailModel = new MessagingModel(null, model.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
//        }
        Map<String, String> map = new HashMap<String, String>();
//        map.put("mode", model.getCountryId() == 1 ? "mobile" : "email");
        map.put("mode", "email");
        logger.info("additional subscriber request done");
        return Response.status(200).entity(map).build();
    }
    //ms-sub
    @UserSubscriberJwt
    @PUT
    @Path("verify-additional-subscriber")
    public Response verifyAdditionalSubscriber(Map<String, Object> map) {
        logger.info("verify additional subscriber");
        String code = (String) map.get("code");
        int companyId = (int) map.get("companyId");
        Date date = Helper.addMinutes(new Date(), -60);
        String sql = "select b from SubscriberVerification b " +
                " where b.verificationCode = :value0 " +
                " and b.created > :value1 " +
                " and b.stage = :value2 " +
                " and b.companyId = :value3";
        SubscriberVerification verification = dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 3, companyId);
        verifyObjectFound(verification);
        SignupRequest sr = dao.find(SignupRequest.class, verification.getSignupRequestId());
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(sr.getEmail());
        subscriber.setMobile(sr.getMobile());
        subscriber.setName(sr.getName());
        subscriber.setCreated(new Date());
        subscriber.setCreatedBy(sr.getCreatedBy());
        subscriber.setEmailVerified(verification.getVerificationMode() == 'E');
        subscriber.setMobileVerified(verification.getVerificationMode() == 'M');
        subscriber.setAdmin(false);
        subscriber.setPassword(sr.getPassword());
        subscriber.setCompanyId(companyId);
        subscriber.setStatus('A');
        //get general role id
        //get admin subscriber and copy its role
        Subscriber admin = dao.findTwoConditions(Subscriber.class, "companyId", "admin", companyId, true);
        for (var role : admin.getRoles()) {
            GeneralRole gr = dao.find(GeneralRole.class, role.getId());
            subscriber.getRoles().add(gr);
        }
        //subscriber.setRoles(admin.getRoles());
        dao.persist(subscriber);
        dao.delete(verification);
        sr.setStatus('C');
        dao.update(sr);
        logger.info("verify additional subscriber done");
        return Response.status(200).entity(subscriber).build();
    }

    //ms-sub
    @ValidApp
    @POST
    @Path(value="verify-signup-before-approval")
    public Response verifySignupToPending(Map<String, String> map){
        logger.info("verify signup before approval");
        String code = map.get("code");
        String email = map.get("email").toLowerCase().trim();
        Date date = Helper.addMinutes(new Date(), -60);
        String sql = "select b from SignupRequest b where b.created > :value0 and b.email = :value1";
        SignupRequest sr = dao.findJPQLParams(SignupRequest.class, sql, date, email);
        verifyObjectFound(sr);
        sql = "select b from SubscriberVerification b where b.verificationCode = :value0 and b.created > :value1 and b.stage = :value2 and b.signupRequestId = :value3";
        SubscriberVerification verification = dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 1, sr.getId());
        verifyObjectFound(verification);
        sr.setStatus('P');//pending
        sr.setEmailVerified(verification.getVerificationMode() == 'E');
        sr.setMobileVerified(verification.getVerificationMode() == 'M');
        dao.delete(verification);
        dao.update(sr);
        async.informAdminsNewRegistration(sr.getCompanyName());
        logger.info("verify signup before approval done");
        return Response.status(200).build();
    }

    //ms-sub
    @UserJwt
    @GET
    @Path("signup-requests/pending")
    public Response getPendingSingupRequests(){
        logger.info("signup request pending");
        List<SignupRequest> requests = dao.getCondition(SignupRequest.class, "status", 'P');
        logger.info("signup request pending done");
        return Response.status(200).entity(requests).build();
    }


    //ms-sub
    @PUT
    @UserJwt
    @Path("decline-signup")
    public Response declineSignup(Map<String,Integer> map){
        logger.info("decline signup");
        int id = map.get("signupRequestsId");
        SignupRequest sr = dao.find(SignupRequest.class, id);
        sr.setStatus('D');//declined
        dao.update(sr);
        logger.info("decline signup done");
        return Response.status(200).build();
    }

    //ms-sub
    @POST
    @UserJwt
    @Path("approve-signup")
    public Response approveSignup(Map<String,Integer> map){
        logger.info("approve signup");
        int id = map.get("signupRequestsId");
        Integer companyId = map.get("companyId");
        SignupRequest sr = dao.find(SignupRequest.class, id);
        if(companyId == null){
            //new company
            Map<String, Integer> planIds = getBasicPlanId();
            int planId = planIds.get("planId");
            int durationId = planIds.get("durationId");
            int roleId = planIds.get("roleId");
            GeneralRole role = dao.find(GeneralRole.class, roleId);
            char verificationMode = sr.isMobileVerified() ? 'M' : 'E';
            Company company = new Company(sr, verificationMode, planId, durationId, role);
            dao.persist(company);
            //create default customer
            createDefaultCashCustomer(company.getId(), company.getCountryId());
        } else {
            Subscriber admin = dao.findTwoConditions(Subscriber.class, "companyId", "admin", companyId, true);
            Set<GeneralRole> generalRoles = new HashSet<>();
            for (var role : admin.getRoles()) {
                GeneralRole gr = dao.find(GeneralRole.class, role.getId());
                generalRoles.add(gr);
            }
            Subscriber subscriber = new Subscriber(companyId, sr, generalRoles);
            dao.persist(subscriber);
            //TO DO: send notification
        }
        sr.setStatus('C');
        dao.update(sr);
        logger.info("approve signup done");
        return Response.status(200).build();
    }

    private void createDefaultCashCustomer(int companyId, int countryId){
        try {
            logger.info("create default cash customer");
            Map<String,Integer> map = new HashMap<String, Integer>();
            map.put("companyId", companyId);
            map.put("countryId", countryId);
            Response r = InternalAppRequester.postSecuredRequest(AppConstants.POST_CREATE_DEFAULT_CASH_CUSTOMER, map);
            if(r.getStatus() == 200){
                Map<String,Integer> remap = r.readEntity(Map.class);
                int customerId = remap.get("customerId");
                String sql = "insert into sub_company_profile_settings (company_id, default_customer_id) values (" + companyId + ", " + customerId + ")" +
                        "on conflict (company_id) do update set default_customer_id = " + customerId;
                dao.insertNative(sql);
            } else r.close();
            logger.info("create default cash customer done");
        }catch (Exception ex){
            logger.info("error in creating default cash customer");
        }
    }


    //not needed
    //verify signup and create company directly
    @ValidApp
    @POST
    @Path(value = "verify-signup")
    public Response verifySignup(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        logger.info("verify signup");
        WebApp webApp = getWebAppFromAuthHeader(header);
        String code = map.get("code");
        String ip = map.get("ipAddress");
        Date date = Helper.addMinutes(new Date(), -60);
        String sql = "select b from SubscriberVerification b where b.verificationCode = :value0 and b.created > :value1 and stage = :value2";
        SubscriberVerification verification = dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 1);
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
        Object loginObject = getLoginObject(subscriber, webApp.getAppCode());
        logger.info("verify signup done");
        return Response.status(200).entity(loginObject).build();
    }



    //ms-sub
    @UserJwt
    @GET
    @Path("comments-history/year/{year}/month/{month}")
    public Response getSmsHistory(@PathParam(value = "year") int year, @PathParam(value = "month") int month){
        logger.info("comments history report");
        Date from = Helper.getFromDate(month, year);
        Date to = Helper.getToDate(month, year);
        String sql = "select b from Comment b where b.created between :value0 and :value1 order by b.created desc";
        List<Comment> list = dao.getJPQLParams(Comment.class, sql, from , to);
        logger.info("comment history report done");
        return Response.ok().entity(list).build();
    }

    //ms-sub
    @SubscriberJwt
    @POST
    @Path("request-verify")
    public Response requestVerification(Map<String, Object> map) {
        logger.info("request verify");
        String method = (String) map.get("method");
        int subscriberId = (int) map.get("subscriberId");
        char mode = method.equals("email") ? 'E' : 'M';
        Subscriber sub = dao.find(Subscriber.class, subscriberId);
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(sub, mode, code);
        dao.persist(sv);
//        if (mode == 'E') {
            String[] s = new String[]{sub.getName(), sv.getVerificationCode()};
            MessagingModel emailModel = new MessagingModel(null, sub.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
//        }
//        if (mode == 'M') {
//            MessagingModel smsModel = new MessagingModel(sub.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, sv.getVerificationCode());
//            async.sendSms(smsModel);
//        }
        logger.info("request verify done");
        return Response.status(200).build();
    }

    //ms-sub
    @SubscriberJwt
    @PUT
    @Path("verify")
    public Response verifyMedium(Map<String, Object> map) {
        logger.info("verify");
        String code = (String) map.get("code");
        int subscriberId = (int) map.get("subscriberId");
        Date date = Helper.addMinutes(new Date(), -60);
        String sql = "select b from SubscriberVerification b " +
                "where b.verificationCode = :value0 " +
                "and b.created > :value1 " +
                "and stage = :value2 " +
                "and b.subscriberId = :value3";
        SubscriberVerification verification = dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 2, subscriberId);
        verifyObjectFound(verification);
        Subscriber sub = dao.find(Subscriber.class, subscriberId);
        if (verification.getVerificationMode() == 'E') {
            sub.setEmailVerified(true);
        }
        if (verification.getVerificationMode() == 'M') {
            sub.setMobileVerified(true);
        }
        dao.update(sub);
        dao.delete(verification);
        logger.info("verify done");
        return Response.status(200).entity(sub).build();
    }


    @InternalApp
    @POST
    @Path("send-purchase-order")
    public Response sendPurchaseOrder(Map<String, Integer> map) {
        logger.info("send purchase order");
        int receiverId = map.get("receiverCompanyId");
        int senderId = map.get("senderId");
        Company receiverCompany = dao.find(Company.class, receiverId);
        Subscriber senderSubscriber = dao.find(Subscriber.class, senderId);
        Company senderCompany = dao.find(Company.class, senderSubscriber.getCompanyId());
        Subscriber admin = receiverCompany.getAdminSubscriber();
//        if (receiverCompany.getCountryId() == 1) {
//            MessagingModel smsModel = new MessagingModel(receiverCompany.getAdminSubscriber().getMobile(), null, AppConstants.MESSAGING_PURPOSE_NEW_PURCHASE_ORDER, senderCompany.getName());
//            async.sendSms(smsModel);
//        } else {
            MessagingModel emailModel = new MessagingModel(null, receiverCompany.getAdminSubscriber().getEmail(), AppConstants.MESSAGING_PURPOSE_NEW_PURCHASE_ORDER, new String[]{admin.getName(), senderCompany.getName()});
            async.sendEmail(emailModel);
//        }
        logger.info("send purchase order done");
        return Response.status(200).build();
    }

    @InternalApp
    @POST
    @Path("update-purchase-order")
    public Response acceptPurchaseOrder(Map<String, Object> map) {
        logger.info("update purchase order");
        int receiverId = (int) map.get("receiverCompanyId");
        int senderId = (int) map.get("senderId");
        String status = (String) map.get("status");
        Company receiverCompany = dao.find(Company.class, receiverId);
        Subscriber subscriber = dao.find(Subscriber.class, senderId);
        Company senderCompany = dao.find(Company.class, subscriber.getCompanyId());
        String purpose = "";
        if (status.equals("Accepted"))
            purpose = AppConstants.MESSAGING_PURPOSE_ACCEPT_PURCHASE_ORDER;
        else if (status.equals("Refused"))
            purpose = AppConstants.MESSAGING_PURPOSE_REFUSE_PURCHASE_ORDER;
//        if (senderCompany.getCountryId() == 1) {
//            MessagingModel smsModel = new MessagingModel(subscriber.getMobile(), null, purpose, receiverCompany.getName());
//            async.sendSms(smsModel);
//        } else {
            MessagingModel emailModel = new MessagingModel(null, subscriber.getEmail(), purpose, new String[]{subscriber.getName(), receiverCompany.getName()});
            async.sendEmail(emailModel);
//        }
        logger.info("update purchase order done");
        return Response.status(200).build();
    }


    //ms-sub
    @ValidApp
    @POST
    @Path(value = "signup-request")
    public Response signup(@HeaderParam(HttpHeaders.AUTHORIZATION) String header,  SignupModel sm) {
        logger.info("signup request");
        WebApp webApp = getWebAppFromAuthHeader(header);
        verifyAvailability(sm.getEmail(), sm.getMobile());//returns 409
        SignupRequest signupRequest = new SignupRequest(sm, webApp.getAppCode());
        dao.persist(signupRequest);
        //generate code !
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(signupRequest, sm.getCountryId() == 1 ? 'M' : 'E', code);
        dao.persist(sv);
//        if (sm.getCountryId() == 1) {
//            MessagingModel smsModel = new MessagingModel(sm.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, sv.getVerificationCode());
//            async.sendSms(smsModel);
//        } else {
            String[] s = new String[]{sm.getName(), sv.getVerificationCode()};
            MessagingModel emailModel = new MessagingModel(null, sm.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
//        }
        logger.info("signup request done");
        return Response.status(200).build();
    }


    private String createVerificationCode() {
        logger.info("create verification code");
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
        logger.info("create verification code done");
        return code;
    }


    //ms-sub
    @UserSubscriberJwt
    @POST
    @Path("contact")
    public Response createContact(CompanyContact contact) {
        logger.info("create contact");
        contact.setCreated(new Date());
        dao.persist(contact);
        logger.info("create contact done");
        return Response.ok().entity(contact).build();
    }

    //ms-sub
    @DELETE
    @Path("contact/{id}")
    @UserJwt
    public Response deleteContact(@PathParam(value = "id") int id){
        logger.info("delete contact");
        CompanyContact contact = dao.find(CompanyContact.class, id);
        dao.delete(contact);
        logger.info("delete contact done");
        return Response.status(200).build();
    }

    //ms-sub
    @POST
    @Path("branch")
    @UserSubscriberJwt
    public Response createBranch(Branch branch) {
        logger.info("create branch");
        branch.setCreated(new Date());
        dao.persist(branch);
        logger.info("create branch done");
        return Response.status(200).entity(branch).build();
    }

    //ms-sub
    @GET
    @Path("company/{id}")
    @UserSubscriberJwt
    public Response getCompany(@PathParam(value = "id") int id) {
        logger.info("get company by id" + id);
        Company company = dao.find(Company.class, id);
        if (company == null) {
            throwError(404);
        }
        logger.info("get company by id done");
        return Response.status(200).entity(company).build();
    }

    @ValidApp
    @POST
    @Path("login")
    public Response login(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        logger.info("login");
        WebApp webApp = this.getWebAppFromAuthHeader(header);
        String password = Helper.cypher(map.get("password"));
        String email = map.get("email").trim().toLowerCase();
        String ip = map.get("ipAddress");
        Subscriber subscriber = dao.findTwoConditions(Subscriber.class, "email", "password", email, password);
        Company company = dao.find(Company.class, subscriber.getCompanyId());
        verifyLogin(company, subscriber, email, ip);
        Object loginObject = getLoginObject(subscriber, webApp.getAppCode());
        logger.info("login done");
        return Response.ok().entity(loginObject).build();
    }

    //ms-sub
    @GET
    @Path("last-login/subscriber/{id}")
    @UserJwt
    public Response getLastLogin(@PathParam(value = "id") int id) {
        logger.info("last login");
        String sql = "select b.created from LoginAttempt b where b.subscriberId = :value0 order by b.created desc";
        List<Date> dates = dao.getJPQLParamsMax(Date.class, sql, 1, id);
        verifyObjectsNotEmpty(dates);
        Map<String, Object> map = new HashMap<>();
        map.put("lastLogin", dates.get(0));
        logger.info("last login done");
        return Response.ok().entity(map).build();
    }

    //ms-sub
    @SubscriberJwt
    @POST
    @Path("search-keyword")
    public Response searchKeyword(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, Object> map) {
        logger.info("search keyword");
        SearchKeyword sk = new SearchKeyword();
        sk.setCompanyId((int) map.get("companyId"));
        sk.setSubscriberId((int) map.get("subscriberId"));
        sk.setQuery((String) map.get("query"));
        sk.setCreated(new Date());
        sk.setFound((boolean) map.get("found"));
        dao.persist(sk);
        logger.info("search keyword done");
        return Response.ok().build();
    }


    //md-sub
    @SubscriberJwt
    @POST
    @Path("replacement-search-keyword")
    public Response searchReplacementKeyword(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, Object> map) {
        logger.info("search replacement keyword");
        SearchReplacementKeyword sk = new SearchReplacementKeyword();
        sk.setCompanyId((int) map.get("companyId"));
        sk.setSubscriberId((int) map.get("subscriberId"));
        sk.setQuery((String) map.get("query"));
        sk.setCreated(new Date());
        sk.setFound((boolean) map.get("found"));
        dao.persist(sk);
        logger.info("search replacement keyword done");
        return Response.ok().build();
    }

    @UserJwt
    @GET
    @Path("company-summary-report/{id}")
    public Response getCompanySummary(@PathParam(value = "id") int id) {
        logger.info("company summary report " + id);
        String sql = "select b from SearchKeyword b where b.companyId =:value0 order by b.created desc";
        List<SearchKeyword> kwds = dao.getJPQLParamsMax(SearchKeyword.class, sql, 50, id);
        sql = "select b from SearchReplacementKeyword b where b.companyId =:value0 order by b.created desc";
        List<SearchReplacementKeyword> replacementKwds = dao.getJPQLParamsMax(SearchReplacementKeyword.class, sql, 50, id);
        sql = "select count(*) from SearchKeyword b where b.companyId =:value0";
        int totalSearches = dao.findJPQLParams(Number.class, sql, id).intValue();
        sql = "select count(*) from SearchReplacementKeyword b where b.companyId =:value0";
        int totalReplacementSearches = dao.findJPQLParams(Number.class, sql, id).intValue();
        sql = "select to_char(z.date, 'Mon') as mon," +
                " extract(year from z.date) as yy," +
                " z.count as count" +
                " from" +
                " (select date_trunc('month', created) as date, count(*) as count" +
                " from sub_search_keyword where company_id = " + id + " group by date_trunc('month', created) order by date desc limit 6)z order by z.date;";
        List<Object> ss = dao.getNative(sql);
        List<MonthlySearches> monthly = new ArrayList<>();
        for (Object o : ss) {
            if (o instanceof Object[]) {
                Object[] objArray = (Object[]) o;
                String month = objArray[0].toString();
                int year = ((Number) objArray[1]).intValue();
                int count = ((Number) objArray[2]).intValue();
                MonthlySearches ms = new MonthlySearches(month, year, count);
                monthly.add(ms);
            }
        }
        SubscriberSummary summary = new SubscriberSummary();
        summary.setMonthlySearches(monthly);
        summary.setTopKeywords(kwds);
        summary.setTopReplacementsKeywords(replacementKwds);
        summary.setTotalSearches(totalSearches);
        summary.setTotalReplacementSearches(totalReplacementSearches);
        logger.info("company summary report done");
        return Response.ok().entity(summary).build();
    }


    //ms-sub
    @UserJwt
    @GET
    @Path("summary-report")
    public Response getHomeSummary() {
        logger.info("summary report home");
        String sql = "select count(*) from SearchKeyword b where cast(b.created as date) = cast(now() as date)";
        int searchKeywordsToday = dao.findJPQLParams(Number.class, sql).intValue();
        sql = "select count(*) from SearchReplacementKeyword b where castcompany-summary-report(b.created as date) = cast(now() as date)";
        int searchReplacementsToday = dao.findJPQLParams(Number.class, sql).intValue();
        sql = "select count(*) from Company c";
        int totalCompanies = dao.findJPQLParams(Number.class, sql).intValue();
        sql = "select count(*) from Company c where c.id in (select c.companyId from SearchKeyword c where c.created > :value0)";
        int activeCompanies = dao.findJPQLParams(Number.class, sql, Helper.addDays(new Date(), -30)).intValue();
        sql = "select b from SearchKeyword b order by b.created desc";
        List<SearchKeyword> kwds = dao.getJPQLParamsMax(SearchKeyword.class, sql, 50);
        sql = "select b from SearchReplacementKeyword b order by b.created desc";
        List<SearchReplacementKeyword> replacementKwds = dao.getJPQLParamsMax(SearchReplacementKeyword.class, sql, 50);
        sql = "select b.id from Company b order by b.created desc";
        List<Integer> topCompaniesIds = dao.getJPQLParamsMax(Integer.class, sql, 10);
        sql = "select to_char(z.date, 'Mon') as mon," +
                " extract(year from z.date) as yy," +
                " z.count as count " +
                " from" +
                " (select date_trunc('month', created) as date, count(*) as count" +
                " from sub_search_keyword GROUP BY date_trunc('month', created) order by date desc limit 6)z order by z.date;";
        List<Object> ss = dao.getNative(sql);
        List<MonthlySearches> monthly = new ArrayList<>();
        for (Object o : ss) {
            if (o instanceof Object[]) {
                Object[] objArray = (Object[]) o;
                String month = objArray[0].toString();
                int year = ((Number) objArray[1]).intValue();
                int count = ((Number) objArray[2]).intValue();
                MonthlySearches ms = new MonthlySearches(month, year, count);
                monthly.add(ms);
            }
        }
        sql = "select to_char(z.date, 'Mon') as mon," +
                " extract(year from z.date) as yy," +
                " z.count as count" +
                " from" +
                " (select date_trunc('month', created) as date, count(*) as count" +
                " from sub_replacement_search_keyword GROUP BY date_trunc('month', created) order by date desc limit 6)z order by z.date;";

        List<MonthlySearches> replacementMonthly = new ArrayList<>();
        List<Object> replacementSS = dao.getNative(sql);
        for(Object o : replacementSS){
            if (o instanceof Object[]) {
                Object[] objArray = (Object[]) o;
                String month = objArray[0].toString();
                int year = ((Number) objArray[1]).intValue();
                int count = ((Number) objArray[2]).intValue();
                MonthlySearches ms = new MonthlySearches(month, year, count);
                replacementMonthly.add(ms);
            }
        }
        SubscriberSummary summary = new SubscriberSummary();
        summary.setSearchesToday(searchKeywordsToday);
        summary.setReplacementSearchesToday(searchReplacementsToday);
        summary.setTotalCompanies(totalCompanies);
        summary.setActiveCompanies(activeCompanies);
        summary.setTopKeywords(kwds);
        summary.setTopReplacementsKeywords(replacementKwds);
        summary.setTopCompanies(topCompaniesIds);
        summary.setMonthlySearches(monthly);
        summary.setReplacementMonthlySearches(replacementMonthly);
        logger.info("summary report home done");
        return Response.ok().entity(summary).build();
    }

    @SubscriberJwt
    @GET
    @Path("verify-search-count/company/{id}")
    public Response verifySearchCount(@PathParam(value = "id") int companyId) {
        logger.info("verify search count company " + companyId);
        String sql = "select count(*) from SearchKeyword where found = :value0 and companyId = :value1" +
                " and cast (created as date) = cast (now() as date)";
        Number number = dao.findJPQLParams(Number.class, sql, true, companyId);
        if (number.intValue() >= 10) {
            logger.info("verify search count 403 done");
            return Response.status(403).build();
        }
        logger.info("verify search count done");
        return Response.status(201).build();
    }

    //ms-sub
    @SubscriberJwt
    @GET
    @Path("replacement-search-count/company/{id}")
    public Response verifyReplacementSearchCount(@PathParam(value = "id") int companyId) {
        logger.info("replacement search count " + companyId);
        String sql = "select count(*) from SearchReplacementKeyword where found = :value0 and companyId = :value1";
        Number number = dao.findJPQLParams(Number.class, sql, true, companyId);
        Map<String,Integer> map = new HashMap<>();
        map.put("count", number.intValue());
        logger.info("replacement ssearch count done");
        return Response.status(200).entity(map).build();
    }

    //ms-sub
    @SubscriberJwt
    @POST
    @Path("hit-search-limit")
    public Response hitLimit(Map<String, Integer> map) {
        logger.info("hit search limit");
        int companyId = map.get("companyId");
        String sql = "select b from SearchLimit b where b.companyId = :value0 and cast (b.created as date) = cast (now() as date)";
        SearchLimit sl = dao.findJPQLParams(SearchLimit.class, sql, companyId);
        if (sl != null) {
            logger.info("hit search limit done 409");
            throwError(409);
        }
        sl = new SearchLimit();
        sl.setCompanyId(companyId);
        sl.setCreated(new Date());
        dao.persist(sl);
        logger.info("hit search limit done");
        return Response.ok().build();
    }


    @UserJwt
    @POST
    @Path("search-report/accumulated")
    public Response getSearchReportAccumulated(Map<String, Object> map) {
        logger.info("search report accumulated");
        Date from = new Date((long) map.get("from"));
        Date to = new Date((long) map.get("to"));
        Helper h = new Helper();
        String sql = "select b.companyId, count(*) from SearchKeyword b where cast(b.created as date) between cast(:value0 as date) and cast(:value1 as date) group by b.companyId";
        List<Object> ss = dao.getJPQLParams(Object.class, sql, from, to);
        List<CompanySearchCount> csc = new ArrayList<>();
        for (Object o : ss) {
            if (o instanceof Object[]) {
                Object[] objArray = (Object[]) o;
                int companyId = ((Number) objArray[0]).intValue();
                int count = ((Number) objArray[1]).intValue();
                CompanySearchCount sc = new CompanySearchCount(companyId, null, count);
                csc.add(sc);
            }
        }
        logger.info("search report accumulated done");
        return Response.ok().entity(csc).build();
    }

    //ms-sub
    @UserJwt
    @GET
    @Path("pull-chunk-size/company/{id}")
    public Response getChunkSize(@PathParam(value = "id") int companyId){
        logger.info("pull chunk size " + companyId);
        String sql = "select pull_chunk_size from sub_company where id = " + companyId;
        Number number = dao.findNative(Number.class, sql);
        Map<String,Integer> map = new HashMap<>();
        map.put("chunk", number.intValue());
        logger.info("pull chunk size done");
        return Response.status(200).entity(map).build();
    }


    @UserJwt
    @POST
    @Path("search-report/hit-limit")
    public Response getSearchReportLimit(Map<String, Object> map) {
        logger.info("search report hit limit");
        Date from = new Date((long) map.get("from"));
        Date to = new Date((long) map.get("to"));
        Helper h = new Helper();
        List<Date> dates = h.getAllDatesBetween(from, to, false);
        List<CompanySearchCount> csc = new ArrayList<>();
        for (Date date : dates) {
            String sql = "select b from SearchLimit b where cast(b.created as date) = cast(:value0 as date)";
            List<SearchLimit> limits = dao.getJPQLParams(SearchLimit.class, sql, date);
            for (var limit : limits) {
                CompanySearchCount counts = new CompanySearchCount(limit.getCreated(), limit.getCompanyId(), 0);
                csc.add(counts);
            }
        }
        logger.info("search report hit limit done");
        return Response.ok().entity(csc).build();
    }

    @UserJwt
    @POST
    @Path("search-report")
    public Response getSearchReport(Map<String, Object> map) {
        logger.info("search report");
        Date from = new Date((long) map.get("from"));
        Date to = new Date((long) map.get("to"));
        Helper h = new Helper();
        List<Date> dates = h.getAllDatesBetween(from, to, false);
        List<CompanySearchCount> csc = new ArrayList<>();
        for (Date date : dates) {
            String sql = "select b.companyId, count(*) from SearchKeyword b where cast(b.created as date) = cast(:value0 as date) group by b.companyId";
            List<Object> ss = dao.getJPQLParams(Object.class, sql, date);
            for (Object o : ss) {
                if (o instanceof Object[]) {
                    Object[] objArray = (Object[]) o;
                    int companyId = ((Number) objArray[0]).intValue();
                    int count = ((Number) objArray[1]).intValue();
                    CompanySearchCount sc = new CompanySearchCount(date, companyId, count);
                    csc.add(sc);
                }
            }
        }
        logger.info("search report done");
        return Response.ok().entity(csc).build();
    }


    //ms-sub
    @UserJwt
    @GET
    @Path("search-activity/from/{from}/to/{to}")
    public Response getVendorSearchKeywordsDate(@PathParam(value = "from") long fromLong, @PathParam(value = "to") long toLong, @Context UriInfo info) {
        logger.info("search activity from to");
        Helper h = new Helper();
        String excludeFriday = info.getQueryParameters().getFirst("exclude-friday");
        List<Date> dates = h.getAllDatesBetween(new Date(fromLong), new Date(toLong), excludeFriday != null);
        List<Map> kgs = new ArrayList<>();
        for (Date date : dates) {
            String sql = "select count(*) from SearchKeyword b where cast(b.created as date) = cast(:value0 as date)";
            Number n = dao.findJPQLParams(Number.class, sql, date);
            Map<String, Object> map = new HashMap<>();
            map.put("count", n.intValue());
            map.put("date", date.getTime());
            kgs.add(map);
        }
        logger.info("search activity from to done");
        return Response.status(200).entity(kgs).build();
    }

    //ms-sub
    @UserJwt
    @GET
    @Path("search-replacement-activity/from/{from}/to/{to}")
    public Response getVendorSearchReplacementKeywordsDate(@PathParam(value = "from") long fromLong, @PathParam(value = "to") long toLong, @Context UriInfo info) {
        logger.info("search replacement activity from to");
        Helper h = new Helper();
        String excludeFriday = info.getQueryParameters().getFirst("exclude-friday");
        List<Date> dates = h.getAllDatesBetween(new Date(fromLong), new Date(toLong), excludeFriday != null);
        List<Map> kgs = new ArrayList<>();
        for (Date date : dates) {
            String sql = "select count(*) from SearchReplacementKeyword b where cast(b.created as date) = cast(:value0 as date)";
            Number n = dao.findJPQLParams(Number.class, sql, date);
            Map<String, Object> map = new HashMap<>();
            map.put("count", n.intValue());
            map.put("date", date.getTime());
            kgs.add(map);
        }
        logger.info("search replacement activity from to done");
        return Response.status(200).entity(kgs).build();
    }

    //ms-sub
    @UserJwt
    @GET
    @Path("today-search/company")
    public Response getLatestVendorSearchesGroup() {
        logger.info("today search / company");
        Helper h = new Helper();
        String dateString = h.getDateFormat(new Date(), "yyyy-MM-dd");
        String sql = "select z.*, c.name from" +
                " (select k.company_id, count(*) from sub_search_keyword k" +
                " where k.created > '" + dateString + "'" +
                " group by company_id order by count desc) z" +
                " left join sub_company c on z.company_id = c.id";
        List<Object> ss = dao.getNative(sql);
        List<CompanySearchCount> csc = new ArrayList<>();
        for (Object o : ss) {
            if (o instanceof Object[]) {
                Object[] objArray = (Object[]) o;
                int companyId = ((Number) objArray[0]).intValue();
                int count = ((Number) objArray[1]).intValue();
                String name = ((String) objArray[2]);
                CompanySearchCount sc = new CompanySearchCount(companyId, name, count);
                csc.add(sc);
            }
        }
        logger.info("today search / company done");
        return Response.ok().entity(csc).build();
    }

    //ms-sub
    @GET
    @Path("company-ids/all")
    @UserJwt
    public Response getAllCompanies() {
        logger.info("company ids / all");
        String sql = "select b.id from Company b order by b.id desc";
        List<Integer> ids = dao.getJPQLParams(Integer.class, sql);
        Map<String, Object> map = new HashMap<>();
        map.put("companyIds", ids);
        logger.info("company ids / all done");
        return Response.ok().entity(map).build();
    }

    //ms-sub
    @GET
    @Path("companies/ids/{ids}")
    @UserJwt
    public Response getCompanies(@PathParam(value = "ids") String ids) {
        logger.info("company ids / ids");
        String[] idsArray = ids.split(",");
        StringBuilder sql = new StringBuilder("select * from sub_company where id in (0");
        for (String s : idsArray) {
            sql.append(",").append(s);
        }
        sql.append(") order by id");
        var companies =  dao.getNative(Company.class, sql.toString());
        logger.info("company ids / ids done");
        return Response.status(200).entity(companies).build();
    }


    //ms-sub
    @POST
    @Path("company-ids/labels")
    @UserJwt
    public Response searchCompaniesWithLabels(List<Label> labels) {
        logger.info("company ids / labels");

        if (labels == null || labels.isEmpty()) {
            return Response.status(400).build();
        }
        String sql = "select b.id from sub_company b where b.id != 0";
        for (var l : labels) {
            sql += " and b.id in ( select c.company_id from sub_company_label c where c.label_id = " + l.getId() + " ) ";
        }
        List<Integer> list = (List<Integer>) dao.getNative(sql);
        Map<String, Object> map = new HashMap<>();
        map.put("companyIds", list);
        logger.info("company ids / labels done");
        return Response.ok().entity(map).build();
    }

    //ms-sub
    @POST
    @Path("pin-comment")
    @UserJwt
    public Response pinComment(CommentPinned pin) {
        logger.info("pin comment");
        String sql = "select b from CommentPinned b where b.comment.id = :value0 and b.pinnedBy = :value1";
        List<CommentPinned> check = dao.getJPQLParams(CommentPinned.class, sql, pin.getComment().getId(), pin.getPinnedBy());
        if (!check.isEmpty()) throwError(409);
        pin.setCreated(new Date());
        dao.persist(pin);
        logger.info("pin comment done");
        return Response.ok().entity(pin).build();
    }

    //ms-sub
    @DELETE
    @Path("pin-comment/{pinId}")
    @UserJwt
    public Response unpin(@PathParam(value = "pinId") int pinId) {
        logger.info("unpin");
        CommentPinned pinned = dao.find(CommentPinned.class, pinId);
        verifyObjectFound(pinned);
        dao.delete(pinned);
        logger.info("unpin done");
        return Response.ok().build();
    }

    //ms-sub
    @GET
    @Path("pin-comments/user/{userId}")
    @UserJwt
    public Response getPinnedComments(@PathParam(value = "userId") int userId) {
        logger.info("pin comment user " + userId);
        String sql = "select b from CommentPinned b where b.pinnedBy = :value0 order by b.created desc";
        List<CommentPinned> pinned = dao.getJPQLParams(CommentPinned.class, sql, userId);
        logger.info("pin comment user done");
        return Response.ok().entity(pinned).build();
    }

    //ms-sub
    @POST
    @Path("comment")
    @UserJwt
    public Response addComment(Comment comment) {
        logger.info("comment");
        if (comment.getCompanyId() == 0) throwError(409);
        dao.persist(comment);
        logger.info("comment done");
        return Response.ok().entity(comment).build();
    }

    //ms-sub
    @DELETE
    @Path("comment/{id}")
    @UserJwt
    public Response deleteComment(@PathParam(value = "id") int id) {
        logger.info("delete comment");
        Comment comment = dao.find(Comment.class, id);
        comment.setStatus('X');
        dao.update(comment);
        logger.info("delete comment done");
        return Response.ok().build();
    }

    //ms-sub
    @GET
    @Path("company-ids/not-logged/days/{days}")
    @UserJwt
    public Response searchNotLogged(@PathParam(value = "days") int days) {
        logger.info("company ids / not logged");
        String sql = "select b.id from Company b where b.id not in (" +
                " select c.companyId from Subscriber c where c.id in (" +
                " select d.subscriberId from LoginAttempt d where d.success = :value0" +
                " and d.created > :value1))";
        Date date = Helper.addDays(new Date(), days * -1);
        List<Integer> companyIds = dao.getJPQLParams(Integer.class, sql, true, date);
        Map<String, Object> map = new HashMap<>();
        map.put("companyIds", companyIds);
        logger.info("company ids not logged done");
        return Response.ok().entity(map).build();
    }

    //ms-sub
    @UserJwt
    @GET
    @Path("company-ids/integrated")
    public Response getIntegratedCompanies() {
        logger.info("company ids integrated");
        String sql = "select b.id from Company b where b.integrated = :value0 order by b.id";
        List<Integer> ints = dao.getJPQLParams(Integer.class, sql, true);
        Map<String, Object> map = new HashMap<>();
        map.put("companyIds", ints);
        logger.info("company ids integrated done");
        return Response.ok().entity(map).build();
    }

    //ms-sub
    @GET
    @Path("company-ids/search/{query}")
    @UserJwt
    public Response search(@PathParam(value = "query") String query) {
        logger.info("company ids search ");
        int id = Helper.parseId(query);
        query = "%" + query.toLowerCase().trim() + "%";
        String sql = " select b.id from Company b " +
                " where lower(b.name) like :value0 " +
                " or lower(b.nameAr) like :value0 " +
                " or b.id in (" +
                " select c.companyId from Subscriber c where c.email like :value0 " +
                " or c.mobile like :value0 or c.name like :value0) " +
                "or b.id = :value1";
        List<Integer> ids = dao.getJPQLParams(Integer.class, sql, query, id);
        Map<String, Object> map = new HashMap<>();
        map.put("companyIds", ids);
        logger.info("company ids search done");
        return Response.ok().entity(map).build();
    }


    //ms-sub
    @UserJwt
    @GET
    @Path("company-joined/from/{from}/to/{to}")
    public Response getVendorsJoinedDate(@PathParam(value = "from") long fromLong, @PathParam(value = "to") long toLong) {
        logger.info("company joined from to");
        Helper h = new Helper();
        Date toDate = new Date(toLong);
        Date fromDate = new Date(fromLong);
        List<Date> dates = h.getAllDatesBetween(fromDate, toDate, false);
        List<CompaniesDateGroup> vdgs = new ArrayList<>();
        for (Date date : dates) {
            String sql = "select count(b) from Company b where cast(b.created as date) = cast(:value0 as date)";
            Long daily = dao.findJPQLParams(Long.class, sql, date);
            String sql2 = "select count(b) from Company b where cast(b.created as date) between cast(:value0 as date) and cast(:value1 as date)";
            Long total = dao.findJPQLParams(Long.class, sql2, fromDate, date);
            var vdg = new CompaniesDateGroup();
            vdg.setDate(date);
            vdg.setDaily(daily.intValue());
            vdg.setTotal(total.intValue());
            vdgs.add(vdg);
        }
        logger.info("company joined from to done");
        return Response.status(200).entity(vdgs).build();

    }


    private Subscriber updateSubscriptionStatus(Subscriber subscriber) {
        logger.info("update subscription status");
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
        logger.info("update subscription done");
        return subscriber;
    }


    private Object getLoginObject(Subscriber subscriber, int appCode) {
        logger.info("get login object");
        subscriber = updateSubscriptionStatus(subscriber);
        if (appCode == 6) {
            CompanyView cview = dao.find(CompanyView.class, subscriber.getCompanyId());
            PbSubscriber pbSubscriber = dao.find(PbSubscriber.class, subscriber.getId());
            String jwt = issueToken(subscriber.getCompanyId(), subscriber.getId(), appCode);
            return new PbLoginObject(cview, pbSubscriber, jwt);
        }
        Company company = dao.find(Company.class, subscriber.getCompanyId());
        String jwt = issueToken(subscriber.getCompanyId(), subscriber.getId(), appCode);
        logger.info("get login object done");
        return new LoginObject(company, subscriber, jwt);
    }

    private String issueToken(int companyId, int userId, int appCode) {
        try {
            logger.info("issue token");
            Map<String, Object> map = new HashMap<>();
            map.put("typ", 'S');
            map.put("appCode", appCode);
            map.put("comp", companyId);
            logger.info("issue token done");
            return KeyConstant.issueToken(userId, map);
        } catch (Exception ex) {
            logger.info("issue token 500");
            throwError(500, "Token issuing error");
            return null;
        }
    }


    private void verifyLogin(Company company, Subscriber subscriber, String email, String ip) {
        logger.info("verify login");
        if(company.getStatus() != 'A' || subscriber.getStatus() != 'A'){
            logger.info("invalid credentials");
            throwError(404, "Invalid credentials");
        }
        if (subscriber == null) {
            logger.info("invalid credentials");
            async.createLoginAttempt(email, 0, ip, false);
            throwError(404, "Invalid credentials");
        } else {
            async.createLoginAttempt(email, subscriber.getId(), ip, true);
        }
        logger.info("verify login done");
    }

    private void verifyObjectFound(Object object) {
        if (object == null) {
            throwError(404);
        }
    }


    private void verifyObjectFound(Object ...objects) {
        for(var ob : objects){
            verifyObjectFound(ob);
        }
    }

    private void verifyObjectsNotEmpty(List list) {
        if (list.isEmpty()) throwError(404);
    }

    //ms-sub
    @ValidApp
    @POST
    @Path("reset-password-verify")
    public Response verifyPasswordReset(Map<String, String> map) {
        logger.info("reset password verify");
        String token = map.get("token");
        String sql = "select b from PasswordReset b where b.token = :value0 and b.status = :value1 and b.expire >= :value2";
        PasswordReset pr = dao.findJPQLParams(PasswordReset.class, sql, token, 'R', new Date());
        verifyObjectFound(pr);
        logger.info("reset password verify done");
        return Response.status(200).build();
    }


    //ms-sub
    @ValidApp
    @PUT
    @Path("reset-password")
    public Response resetPassword(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        logger.info("reset password update");
        WebApp webApp = getWebAppFromAuthHeader(header);
        String token = map.get("token");
        String password = map.get("newPassword");
        String sql = "select b from PasswordReset b where b.token = :value0 and b.status = :value1 and b.expire >= :value2";
        PasswordReset pr = dao.findJPQLParams(PasswordReset.class, sql, token, 'R', new Date());
        verifyObjectFound(pr);
        Subscriber subscriber = dao.find(Subscriber.class, pr.getSubscriberId());
        subscriber.setPassword(Helper.cypher(password));
        subscriber.setEmailVerified(true);
        dao.update(subscriber);
        pr.setStatus('V');
        dao.update(pr);
        LoginObject loginObject = (LoginObject) getLoginObject(subscriber, webApp.getAppCode());
        logger.info("reset password update done");
        return Response.status(200).entity(loginObject).build();
    }

    //ms-sub
    @UserJwt
    @POST
    @Path("generate-password-link")
    public Response generatePasswordRest(Map<String,Object> map){
        logger.info("generate password link");
        int subscriberId = ((Number) map.get("subscriberId")).intValue();
        String application = (String) map.get("application");
        if(!(application.equals("qvm") || application.equals("qstock")))
            return Response.status(404).build();
        Subscriber subscriber = dao.find(Subscriber.class, subscriberId);
        String token = createPasswordResetObject(subscriber);
        String[] values = new String[]{token, subscriber.getName(), application};
        MessagingModel emailModel = new MessagingModel(null, subscriber.getEmail(), AppConstants.MESSAGING_PURPOSE_PASS_RESET, values);
        async.sendEmail(emailModel);
        Map<String,String> newMap = new HashMap<>();
        newMap.put("token", token);
        logger.info("generate password link done");
        return Response.status(200).entity(newMap).build();
    }

    //ms-sub
    @ValidApp
    @POST
    @Path("password-reset-request")
    public Response requestPasswordReset(Map<String, String> map) {
        logger.info("password reset request");
        String method = map.get("method");
        String sql = "select b from Subscriber b where ";
        String value = "";
        if (method.equals("email")) {
            sql += "b.email = :value0";
            value = map.get("email").trim().toLowerCase();
        } else if (method.equals("sms")) {
            sql += "b.mobile = :value0";
            value = map.get("mobile");
        }
        Subscriber subscriber = dao.findJPQLParams(Subscriber.class, sql, value);
        verifyObjectFound(subscriber);
        String token = createPasswordResetObject(subscriber);
        String[] values = new String[]{token, subscriber.getName(), "qvm"};
        MessagingModel emailModel = new MessagingModel(null, subscriber.getEmail(), AppConstants.MESSAGING_PURPOSE_PASS_RESET, values);
        async.sendEmail(emailModel);
        logger.info("password reset request done");
        return Response.status(200).build();
    }


    private String createPasswordResetObject(Subscriber subscriber) {
        logger.info("create password reset object");
        String code = "";
        boolean available = false;
        do {
            code = Helper.getRandomString(20);
            String sql = "select b from PasswordReset b where b.token = :value0 and b.expire >= :value1 and status =:value2";
            List<PasswordReset> l = dao.getJPQLParams(PasswordReset.class, sql, code, new Date(), 'R');
            if (l.isEmpty()) {
                available = true;
            }
        } while (!available);

        PasswordReset ev = new PasswordReset();
        ev.setToken(code);
        ev.setCreated(new Date());
        ev.setSubscriberId(subscriber.getId());
        ev.setExpire(Helper.addMinutes(ev.getCreated(), 60 * 24 * 14));
        ev.setStatus('R');
        dao.persist(ev);
        logger.info("create passsword reset object done");
        return code;
    }


    private Map<String, Integer> getBasicPlanId() {
        logger.info("get basic plan id");
        Response r = InternalAppRequester.getSecuredRequest(AppConstants.GET_BASIC_PLAN_ID);
        if (r.getStatus() == 200) {
            logger.info("get basic plan id done");
            return r.readEntity(Map.class);
        }
        logger.info("get basic plan id error");
        throwError(500);
        return null;
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

    //ms-sub
    @POST
    @Path("general-activities")
    @UserJwt
    public Response createGeneralActivities(List<GeneralActivity> activities) {
        logger.info("create general activities");
        activities.forEach(ga -> dao.persist(ga));
        logger.info("create general activities done");
        return Response.status(200).build();
    }


    //ms-sub
    @GET
    @Path("general-activities")
    @UserJwt
    public Response getGeneralActivities() {
        logger.info("get general activities");
        List<GeneralActivity> activities = dao.get(GeneralActivity.class);
        logger.info("get general activities done");
        return Response.status(200).entity(activities).build();
    }

    //ms-sub
    @GET
    @Path("general-roles")
    @UserJwt
    public Response getGeneralRoles() {
        logger.info("get general role");
        List<GeneralRole> roles = dao.get(GeneralRole.class);
        logger.info("get general roles done");
        return Response.status(200).entity(roles).build();
    }

    //ms-sub
    @POST
    @Path("general-role")
    @UserJwt
    public Response createGeneralRole(GeneralRole generalRole) {
        logger.info("create general role");
        dao.persist(generalRole);
        logger.info("create general role done");
        return Response.status(200).entity(generalRole).build();
    }

    //ms-sub
    @PUT
    @Path("general-role")
    @UserJwt
    public Response updateGeneralRole(GeneralRole generalRole) {
        logger.info("update general role");
        dao.update(generalRole);
        logger.info("update general role done");
        return Response.status(200).entity(generalRole).build();
    }

    private int getPlanRoleId(int planId, String header) {
        logger.info("get plan role id");
        Response r = this.getSecuredRequest(AppConstants.getPlanGeneralRoleId(planId), header);
        if (r.getStatus() == 200) {
            Map<String, Integer> map = r.readEntity(Map.class);
            logger.info("get plan role id done");
            return map.get("generalRoleId");
        }
        logger.info("get plan role id error");
        return 0;
    }

    //ms-sub
    @POST
    @Path("label")
    @UserJwt
    public Response createLabel(Label label) {
        logger.info("create label");
        label.setCreated(new Date());
        Label check = dao.findCondition(Label.class, "label", label.getLabel());
        if (check != null) {
            throwError(409);
        }
        dao.persist(label);
        logger.info("create label done");
        return Response.status(200).entity(label).build();
    }

    //ms-subscriber
    @GET
    @Path("labels")
    @UserJwt
    public Response getLabels() {
        logger.info("get labels");
        List<Label> labels = dao.get(Label.class);
        logger.info("get labels done");
        return Response.status(200).entity(labels).build();
    }


    @UserSubscriberJwt
    @GET
    @Path("qvm-invoice/{invoiceId}/company/{companyId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getInvoice(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, @PathParam(value = "invoiceId") int salesId, @PathParam(value = "companyId") int companyId) {
        logger.info("get qvm invoice");
        Subscription subscription = dao.findTwoConditions(Subscription.class, "salesId", "companyId", salesId, companyId);
        Company company = dao.find(Company.class, companyId);
        verifyObjectFound(subscription);
        verifyObjectFound(company);
        MessagingModel mm = async.createInvoiceEmailModel(subscription, company, header);
        Response r = InternalAppRequester.postSecuredRequest(AppConstants.POST_GENERATE_HTML, mm);
        if (r.getStatus() == 200) {
            String body = r.readEntity(String.class);
            logger.info("get qvm invoice done");
            return Response.ok().entity(body).build();
        }
        logger.info("get qvm invoice error");
        throwError(404);
        return null;
    }

    @InternalApp
    @POST
    @Path("companies/reduced")
    public Response getCompanyReduced(Map<String, Object> map) {
        logger.info("get company reduced");
        List<Integer> ids = (ArrayList) map.get("companyIds");
        String sql = "select * from sub_company b where b.id in (0";
        for (var id : ids) {
            sql += "," + id;
        }
        sql += ")";
        List<CompanyReduced> companyReducedList = dao.getNative(CompanyReduced.class, sql);
        logger.info("get company reduced done");
        return Response.ok().entity(companyReducedList).build();
    }

    //this only works for me
    @UserJwt
    @POST
    @Path("api-long-token")
    public Response generateApiLongToken(Map<String, Integer> map) {
        try {
            logger.info("api link token generate");
            int companyId = map.get("companyId");
            int subscriberId = map.get("subscriberId");
            int duration = map.get("duration");
            int appCode = map.get("appCode");
            Date issued = new Date();
            Date expire = Helper.addDays(issued, duration);
            Map<String, Object> cred = new HashMap<>();
            cred.put("typ", 'S');
            cred.put("appCode", appCode);
            cred.put("comp", companyId);
            String token = KeyConstant.issueToken(subscriberId, cred, issued, expire);
            MessagingModel emailModel = new MessagingModel(null, AppConstants.ADMIN_EMAIL, AppConstants.MESSAGING_PURPOSE_API_TOKEN, new String[]{token});
            async.sendEmail(emailModel);
            logger.info("api link token generate done");
            return Response.status(200).build();
        } catch (Exception ex) {
            throwError(500, "Token issuing error");
            return null;
        }
    }

    //ms-sub
    @UserJwt
    @PUT
    @Path("subscriber")
    public Response editSubscriber(Map<String,Object> map){
        logger.info("edit subscrriber");
        int id = (int) map.get("id");
        String email = (String) map.get("email");
        String mobile = (String) map.get("mobile");
        String name = (String) map.get("name");
        Subscriber subscriber = dao.find(Subscriber.class, id);
        if(name.trim().length() > 0) {
            subscriber.setName(name);
        }
        if(!mobile.trim().equals(subscriber.getMobile().trim())) {
            subscriber.setMobile(mobile.trim());
            subscriber.setMobileVerified(false);
        }
        if(!email.toLowerCase().trim().equals(subscriber.getEmail().trim().toLowerCase())) {
            subscriber.setEmail(email.toLowerCase().trim());
            subscriber.setEmailVerified(false);
        }
        logger.info("edit subscriber done");
        dao.update(subscriber);
        return Response.status(200).build();
    }

    @UserJwt
    @POST
    @Path("merge")
    public Response mergeSubscriptions(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String,Integer> map){
        logger.info("merge");
        int mainId = map.get("mainId");
        int secId = map.get("secondaryId");
        int createdBy = map.get("createdBy");
        Company main = dao.find(Company.class, mainId);
        Company sec = dao.find(Company.class, secId);
        for(Subscriber s : sec.getSubscribers()){
            s.setCompanyId(main.getId());
            s.setAdmin(false);
            s.setRoles(main.getAdminSubscriber().getRoles());
            dao.update(s);
        }
        for(Subscription s : sec.getSubscriptions()){
          dao.delete(s);
        }
        for(Branch b : sec.getBranches()){
            for(CompanyContact cc : b.getContacts()){
                dao.delete(cc);
            }
            dao.delete(b);
        }
        for(Comment comment : sec.getComments()){
            comment.setCompanyId(main.getId());
            comment.setComment("[from merged account ]" + comment.getComment());
            dao.update(comment);
        }
        Comment comment = new Comment();
        comment.setComment("[Merged Company ID " + sec.getId() + "]");
        comment.setCompanyId(main.getId());
        comment.setStatus('A');
        comment.setCreated(new Date());
        comment.setCreatedBy(createdBy);
        dao.persist(comment);
        String sql = "update sub_search_keyword set company_id = " + main.getId() + " where company_id = "  + sec.getId();
        dao.updateNative(sql);
        String sql2 = "update sub_replacement_search_keyword set company_id = " + main.getId() + " where company_id = "  + sec.getId();
        dao.updateNative(sql2);
        String sql3 = "delete from sub_company where id = " + sec.getId();
        dao.updateNative(sql3);
        logger.info("merge done");
        return Response.status(200).build();
    }


    @UserSubscriberJwt
    @POST
    @Path("subscribe")
    public Response createSubscription(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, SubscribeModel model) {
        logger.info("subscribe");
        Company company = dao.find(Company.class, model.getCompanyId());
        Subscription futureSubscription = company.getFutureSubscription();
        Subscription premium = new Subscription();
        premium.setPlanId(model.getPlanId());
        premium.setDurationId(model.getDurationId());
        premium.setCreatedBySubscriber(model.getCreatedBySubscriber());
        premium.setCreated(new Date());
        premium.setSalesId(model.getSalesId());
        if (futureSubscription == null) {
            Subscription activeSubscription = company.getActiveSubscription();
            if (activeSubscription.getStatus() == 'B') {
                //upgrade to premium
                premium.setStatus('A');
                premium.setStartDate(new Date());
                premium.setEndDate(Helper.addDays(premium.getStartDate(), model.getActualDays()));
            } else if (activeSubscription.getStatus() == 'A') {
                //add a future subscription
                premium.setStatus('F');
                premium.setStartDate(activeSubscription.getEndDate());
                premium.setEndDate(Helper.addDays(premium.getStartDate(), model.getActualDays()));
            }
        } else {
            //deal with it later
            premium.setStatus('F');
            premium.setStartDate(futureSubscription.getEndDate());
            premium.setEndDate(Helper.addDays(premium.getStartDate(), model.getActualDays()));
        }
        company.getSubscriptions().add(premium);
        int roleId = getPlanRoleId(model.getPlanId(), header);
        GeneralRole gr = dao.find(GeneralRole.class, roleId);
        company.updateSubscribersRoles(gr);
        dao.update(company);
        Company updated = dao.find(Company.class, company.getId());
        async.sendInvoiceEmail(premium, updated, header);
        logger.info("subscribe done");
        return Response.ok().entity(updated).build();
    }




    public void throwError(int code) {
        throwError(code, null);
    }

    public void throwError(int code, String msg) {
        throw new WebApplicationException(
                Response.status(code).entity(msg).build()
        );
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


    public <T> Response postSecuredRequest(String link, T t, String authHeader) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, authHeader);
        Response r = b.post(Entity.entity(t, "application/json"));// not secured
        return r;
    }

}