package q.rest.subscriber.operation;

import q.rest.subscriber.dao.DAO;
import q.rest.subscriber.filter.annotation.*;
import q.rest.subscriber.helper.AppConstants;
import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.helper.KeyConstant;
import q.rest.subscriber.helper.InternalAppRequester;
import q.rest.subscriber.model.*;
import q.rest.subscriber.model.entity.*;
import q.rest.subscriber.model.entity.role.general.GeneralActivity;
import q.rest.subscriber.model.entity.role.general.GeneralRole;
import q.rest.subscriber.model.publicapi.PbCompany;
import q.rest.subscriber.model.publicapi.PbLoginObject;
import q.rest.subscriber.model.publicapi.PbSubscriber;
import q.rest.subscriber.model.reduced.CompanyReduced;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.*;
import java.util.*;

@Path("/api/v1/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiV1 {

    @EJB
    private DAO dao;

    @EJB
    private AsyncService async;

    @UserJwt
    @PUT
    @Path("company")
    public Response updateCompany(Company company) {
        Company co = dao.find(Company.class, company.getId());
        company.setSubscribers(co.getSubscribers());//prevent password loss
        dao.update(company);
        return Response.status(200).entity(company).build();
    }




    @UserSubscriberJwt
    @POST
    @Path("additional-subscriber-request")
    public Response addNewSubscriber(AddSubscriberModel model) {
        verifyAvailability(model.getEmail(), model.getMobile());//returns 409
        SignupRequest sr = new SignupRequest(model);
        dao.persist(sr);
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(sr, model.getCountryId() == 1 ? 'M' : 'E', code, model.getCompanyId());
        dao.persist(sv);
        if (model.getCountryId() == 1) {
            MessagingModel smsModel = new MessagingModel(model.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, sv.getVerificationCode());
            async.sendSms(smsModel);
        } else {
            String[] s = new String[]{model.getName(), sv.getVerificationCode()};
            MessagingModel emailModel = new MessagingModel(null, model.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
        }
        Map<String, String> map = new HashMap<String, String>();
        map.put("mode", model.getCountryId() == 1 ? "mobile" : "email");
        return Response.status(200).entity(map).build();
    }

    @UserSubscriberJwt
    @PUT
    @Path("verify-additional-subscriber")
    public Response verifyAdditionalSubscriber(Map<String, Object> map) {
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
        return Response.status(200).entity(subscriber).build();
    }

    @ValidApp
    @POST
    @Path(value = "verify-signup")
    public Response verifySignup(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
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
        verifyLogin(subscriber, subscriber.getEmail(), ip);
        Object loginObject = getLoginObject(subscriber, webApp.getAppCode());
        return Response.status(200).entity(loginObject).build();
    }


    @SubscriberJwt
    @POST
    @Path("request-verify")
    public Response requestVerification(Map<String, Object> map) {
        String method = (String) map.get("method");
        int subscriberId = (int) map.get("subscriberId");
        char mode = method.equals("email") ? 'E' : 'M';
        Subscriber sub = dao.find(Subscriber.class, subscriberId);
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(sub, mode, code);
        dao.persist(sv);
        if (mode == 'E') {
            String[] s = new String[]{sub.getName(), sv.getVerificationCode()};
            MessagingModel emailModel = new MessagingModel(null, sub.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
        }
        if (mode == 'M') {
            MessagingModel smsModel = new MessagingModel(sub.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, sv.getVerificationCode());
            async.sendSms(smsModel);
        }
        return Response.status(200).build();
    }

    @SubscriberJwt
    @PUT
    @Path("verify")
    public Response verifyMedium(Map<String, Object> map) {
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
        return Response.status(200).entity(sub).build();
    }



    @InternalApp
    @POST
    @Path("send-purchase-order")
    public Response sendPurchaseOrder(Map<String, Integer> map){
        int receiverId = map.get("receiverId");
        int senderId = map.get("senderId");
        Company receiverCompany = dao.find(Company.class, receiverId);
        Company senderCompany = dao.find(Company.class, senderId);
        Subscriber admin = receiverCompany.getAdminSubscriber();
        if(receiverCompany.getCountryId() == 1){
            MessagingModel smsModel = new MessagingModel(admin.getMobile(), null, AppConstants.MESSAGING_PURPOSE_NEW_PURCHASE_ORDER, senderCompany.getName());
            async.sendSms(smsModel);
        }else {
            String[] s = new String[]{admin.getName(), senderCompany.getName()};
            MessagingModel emailModel = new MessagingModel(null, admin.getEmail(), AppConstants.MESSAGING_PURPOSE_NEW_PURCHASE_ORDER, s);
            async.sendEmail(emailModel);
        }
        return Response.status(200).build();
    }

    @InternalApp
    @POST
    @Path("update-purchase-order")
    public Response acceptPurchaseOrder(Map<String, Object> map){
        int receiverId = (int) map.get("receiverId");
        int senderId = (int) map.get("senderId");
        String status = (String) map.get("status");
        Company receiverCompany = dao.find(Company.class, receiverId);
        Company senderCompany = dao.find(Company.class, senderId);
        Subscriber admin = senderCompany.getAdminSubscriber();
        String purpose ="";
        if(status.equals("Accepted")){
            purpose  = AppConstants.MESSAGING_PURPOSE_ACCEPT_PURCHASE_ORDER;
        }
        else if (status.equals("Refused")){
            purpose  = AppConstants.MESSAGING_PURPOSE_REFUSE_PURCHASE_ORDER;
        }
        if(receiverCompany.getCountryId() == 1){
            MessagingModel smsModel = new MessagingModel(admin.getMobile(), null, purpose, receiverCompany.getName());
            async.sendSms(smsModel);
        }else {
            String[] s = new String[]{admin.getName(), receiverCompany.getName()};
            MessagingModel emailModel = new MessagingModel(null, admin.getEmail(), purpose, s);
            async.sendEmail(emailModel);
        }
        return Response.status(200).build();
    }


    @ValidApp
    @POST
    @Path(value = "signup-request")
    public Response signup(SignupModel sm) {
        verifyAvailability(sm.getEmail(), sm.getMobile());//returns 409
        SignupRequest signupRequest = new SignupRequest(sm);
        dao.persist(signupRequest);
        //generate code !
        String code = createVerificationCode();
        SubscriberVerification sv = new SubscriberVerification(signupRequest, sm.getCountryId() == 1 ? 'M' : 'E', code);
        dao.persist(sv);
        if (sm.getCountryId() == 1) {
            MessagingModel smsModel = new MessagingModel(sm.getMobile(), null, AppConstants.MESSAGING_PURPOSE_SIGNUP, sv.getVerificationCode());
            async.sendSms(smsModel);
        } else {
            String[] s = new String[]{sm.getName(), sv.getVerificationCode()};
            MessagingModel emailModel = new MessagingModel(null, sm.getEmail(), AppConstants.MESSAGING_PURPOSE_SIGNUP, s);
            async.sendEmail(emailModel);
        }
        return Response.status(200).build();
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


    @POST
    @Path("contact")
    @UserSubscriberJwt
    public Response createContact(CompanyContact contact) {
        contact.setCreated(new Date());
        dao.persist(contact);
        return Response.ok().entity(contact).build();
    }

    @POST
    @Path("branch")
    @UserSubscriberJwt
    public Response createBranch(Branch branch) {
        branch.setCreated(new Date());
        dao.persist(branch);
        return Response.status(200).entity(branch).build();
    }

    @GET
    @Path("company/{id}")
    @UserSubscriberJwt
    public Response getCompany(@PathParam(value = "id") int id) {
        Company company = dao.find(Company.class, id);
        if (company == null) {
            throwError(404);
        }
        return Response.status(200).entity(company).build();
    }

    @POST
    @Path("login")
    @ValidApp
    public Response login(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
        WebApp webApp = this.getWebAppFromAuthHeader(header);
        String password = Helper.cypher(map.get("password"));
        String email = map.get("email").trim().toLowerCase();
        String ip = map.get("ipAddress");
        Subscriber subscriber = dao.findTwoConditions(Subscriber.class, "email", "password", email, password);
        verifyLogin(subscriber, email, ip);
        Object loginObject = getLoginObject(subscriber, webApp.getAppCode());
        return Response.ok().entity(loginObject).build();
    }

    @GET
    @Path("last-login/subscriber/{id}")
    @UserJwt
    public Response getLastLogin(@PathParam(value = "id") int id) {
        String sql = "select b.created from LoginAttempt b where b.subscriberId = :value0 order by b.created desc";
        List<Date> dates = dao.getJPQLParamsMax(Date.class, sql, 1, id);
        verifyObjectsNotEmpty(dates);
        Map<String, Object> map = new HashMap<>();
        map.put("lastLogin", dates.get(0));
        return Response.ok().entity(map).build();
    }

    @SubscriberJwt
    @POST
    @Path("search-keyword")
    public Response searchKeyword(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, Object> map) {
        SearchKeyword sk = new SearchKeyword();
        sk.setCompanyId((int) map.get("companyId"));
        sk.setSubscriberId((int) map.get("subscriberId"));
        sk.setQuery((String) map.get("query"));
        sk.setCreated(new Date());
        sk.setFound((boolean) map.get("found"));
        dao.persist(sk);
        return Response.ok().build();
    }

    @UserJwt
    @GET
    @Path("company-summary-report/{id}")
    public Response getCompanySummary(@PathParam(value = "id") int id) {
        String sql = "select b from SearchKeyword b where b.companyId =:value0 order by b.created desc";
        List<SearchKeyword> kwds = dao.getJPQLParamsMax(SearchKeyword.class, sql, 50, id);
        sql = "select count(*) from SearchKeyword b where b.companyId =:value0";
        int totalSearches = dao.findJPQLParams(Number.class, sql, id).intValue();
        sql = "select to_char(z.date, 'Mon') as mon," +
                " extract(year from z.date) as yy," +
                " count (z.*) as count" +
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
        summary.setTotalSearches(totalSearches);
        return Response.ok().entity(summary).build();
    }

    @UserJwt
    @GET
    @Path("summary-report")
    public Response getHomeSummary() {
        String sql = "select count(*) from SearchKeyword b where cast(b.created as date) = cast(now() as date)";
        int searchKeywordsToday = dao.findJPQLParams(Number.class, sql).intValue();
        sql = "select count(*) from Company c";
        int totalCompanies = dao.findJPQLParams(Number.class, sql).intValue();
        sql = "select count(*) from Company c where c.id in (select c.companyId from SearchKeyword c where c.created > :value0)";
        int activeCompanies = dao.findJPQLParams(Number.class, sql, Helper.addDays(new Date(), -5)).intValue();
        sql = "select b from SearchKeyword b order by b.created desc";
        List<SearchKeyword> kwds = dao.getJPQLParamsMax(SearchKeyword.class, sql, 50);
        sql = "select b.id from Company b order by b.created desc";
        List<Integer> topCompaniesIds = dao.getJPQLParamsMax(Integer.class, sql, 10);
        sql = "select to_char(z.date, 'Mon') as mon," +
                " extract(year from z.date) as yy," +
                " count (z.*) as count" +
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

        SubscriberSummary summary = new SubscriberSummary();
        summary.setSearchesToday(searchKeywordsToday);
        summary.setTotalCompanies(totalCompanies);
        summary.setActiveCompanies(activeCompanies);
        summary.setTopKeywords(kwds);
        summary.setTopCompanies(topCompaniesIds);
        summary.setMonthlySearches(monthly);
        return Response.ok().entity(summary).build();
    }

    @SubscriberJwt
    @GET
    @Path("verify-search-count/company/{id}")
    public Response verifySearchCount(@PathParam(value = "id") int companyId) {
        String sql = "select count(*) from SearchKeyword where found = :value0 and companyId = :value1" +
                " and cast (created as date) = cast (now() as date)";
        Number number = dao.findJPQLParams(Number.class, sql, true, companyId);
        if (number.intValue() >= 10) {
            return Response.status(403).build();
        }
        return Response.status(201).build();
    }

    @SubscriberJwt
    @POST
    @Path("hit-search-limit")
    public Response hitLimit(Map<String, Integer> map) {
        int companyId = map.get("companyId");
        String sql = "select b from SearchLimit b where b.companyId = :value0 and cast (b.created as date) = cast (now() as date)";
        SearchLimit sl = dao.findJPQLParams(SearchLimit.class, sql, companyId);
        if (sl != null) {
            throwError(409);
        }
        sl = new SearchLimit();
        sl.setCompanyId(companyId);
        sl.setCreated(new Date());
        dao.persist(sl);
        return Response.ok().build();
    }


    @UserJwt
    @POST
    @Path("search-report/accumulated")
    public Response getSearchReportAccumulated(Map<String, Object> map) {
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
        return Response.ok().entity(csc).build();
    }


    @UserJwt
    @POST
    @Path("search-report/hit-limit")
    public Response getSearchReportLimit(Map<String, Object> map){
        Date from = new Date((long) map.get("from"));
        Date to = new Date((long) map.get("to"));
        Helper h = new Helper();
        List<Date> dates = h.getAllDatesBetween(from, to, false);
        List<CompanySearchCount> csc = new ArrayList<>();
        for (Date date : dates) {
            String sql = "select b from SearchLimit b where cast(b.created as date) = cast(:value0 as date)";
            List<SearchLimit> limits = dao.getJPQLParams(SearchLimit.class, sql, date);
            for(var limit : limits){
                CompanySearchCount counts = new CompanySearchCount(limit.getCreated(), limit.getCompanyId(), 0);
                csc.add(counts);
            }
        }
        return Response.ok().entity(csc).build();
    }

    @UserJwt
    @POST
    @Path("search-report")
    public Response getSearchReport(Map<String, Object> map) {
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
        return Response.ok().entity(csc).build();
    }

    @UserJwt
    @GET
    @Path("search-activity/from/{from}/to/{to}")
    public Response getVendorSearchKeywordsDate(@PathParam(value = "from") long fromLong, @PathParam(value = "to") long toLong, @Context UriInfo info) {
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
        return Response.status(200).entity(kgs).build();
    }

    @UserJwt
    @GET
    @Path("today-search/company")
    public Response getLatestVendorSearchesGroup() {
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
        return Response.ok().entity(csc).build();
    }

    @GET
    @Path("companies/all")
    @UserJwt
    public Response getAllCompanies() {
        String sql = "select b.id from Company b order by b.id desc";
        List<Integer> ids = dao.getJPQLParams(Integer.class, sql);
        Map<String, Object> map = new HashMap<>();
        map.put("companies", ids);
        return Response.ok().entity(map).build();
    }

    @POST
    @Path("search/company/labels")
    @UserJwt
    public Response searchCompaniesWithLabels(List<Label> labels) {
        if(labels == null || labels.isEmpty()){
            return Response.status(400).build();
        }
        String sql = "select b.id from sub_company b where b.id != 0";
        for (var l : labels) {
            sql+= " and b.id in ( select c.company_id from sub_company_label c where c.label_id = " + l.getId() + " ) ";
        }
        List<Integer> list = (List<Integer>) dao.getNative(sql);
        Map<String, Object> map = new HashMap<>();
        map.put("companies", list);
        return Response.ok().entity(map).build();
    }

    @GET
    @Path("search/company/not-logged/days/{days}")
    @UserJwt
    public Response searchNotLogged(@PathParam(value = "days") int days) {
        String sql = "select b.id from Company b where b.id not in (" +
                " select c.companyId from Subscriber c where c.id in (" +
                " select d.subscriberId from LoginAttempt d where d.success = :value0" +
                " and d.created > :value1))";
        Date date = Helper.addDays(new Date(), days * -1);
        List<Integer> companyIds = dao.getJPQLParams(Integer.class, sql, true, date);
        Map<String, Object> map = new HashMap<>();
        map.put("companies", companyIds);
        return Response.ok().entity(map).build();
    }

    @UserJwt
    @GET
    @Path("companies/integrated")
    public Response getIntegratedCompanies() {
        String sql = "select b.id from Company b where b.integrated = :value0 order by b.id";
        List<Integer> ints = dao.getJPQLParams(Integer.class, sql, true);
        Map<String, Object> map = new HashMap<>();
        map.put("companies", ints);
        return Response.ok().entity(map).build();
    }

    @GET
    @Path("search/company/{query}")
    @UserJwt
    public Response search(@PathParam(value = "query") String query) {
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
        map.put("companies", ids);
        return Response.ok().entity(map).build();
    }


    @UserJwt
    @GET
    @Path("company-joined/from/{from}/to/{to}")
    public Response getVendorsJoinedDate(@PathParam(value = "from") long fromLong, @PathParam(value = "to") long toLong) {
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
        return Response.status(200).entity(vdgs).build();

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


    private Object getLoginObject(Subscriber subscriber, int appCode) {
        subscriber = updateSubscriptionStatus(subscriber);
        if(appCode == 6){
            PbCompany pbCompany = dao.find(PbCompany.class, subscriber.getCompanyId());
            PbSubscriber pbSubscriber = dao.find(PbSubscriber.class, subscriber.getId());
            String jwt = issueToken(subscriber.getCompanyId(), subscriber.getId(), appCode);
            return new PbLoginObject(pbCompany, pbSubscriber, jwt);
        }
        Company company = dao.find(Company.class, subscriber.getCompanyId());
        String jwt = issueToken(subscriber.getCompanyId(), subscriber.getId(), appCode);
        return new LoginObject(company, subscriber, jwt);
    }

    private String issueToken(int companyId, int userId, int appCode) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("typ", 'S');
            map.put("appCode", appCode);
            map.put("comp", companyId);
            return KeyConstant.issueToken(userId, map);
        } catch (Exception ex) {
            throwError(500, "Token issuing error");
            return null;
        }
    }


    private void verifyLogin(Subscriber subscriber, String email, String ip) {
        if (subscriber == null) {
            async.createLoginAttempt(email, 0, ip);
            throwError(404, "Invalid credentials");
        } else {
            async.createLoginAttempt(email, subscriber.getId(), ip);
        }
    }

    private void verifyObjectFound(Object object) {
        if (object == null) {
            throwError(404);
        }
    }

    private void verifyObjectsNotEmpty(List list) {
        if (list.isEmpty()) throwError(404);
    }

    @ValidApp
    @POST
    @Path("reset-password-verify")
    public Response verifyPasswordReset(Map<String, String> map) {
        String token = map.get("token");
        String sql = "select b from PasswordReset b where b.token = :value0 and b.status = :value1 and b.expire >= :value2";
        PasswordReset pr = dao.findJPQLParams(PasswordReset.class, sql, token, 'R', new Date());
        verifyObjectFound(pr);
        return Response.status(200).build();
    }


    @ValidApp
    @PUT
    @Path("reset-password")
    public Response resetPassword(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, Map<String, String> map) {
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
        return Response.status(200).entity(loginObject).build();
    }


    @ValidApp
    @POST
    @Path("password-reset-request")
    public Response requestPasswordReset(Map<String, String> map) {
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
        String[] values = new String[]{token, subscriber.getName()};
        MessagingModel emailModel = new MessagingModel(null, subscriber.getEmail(), AppConstants.MESSAGING_PURPOSE_PASS_RESET, values);
        async.sendEmail(emailModel);
        return Response.status(200).build();
    }


    private String createPasswordResetObject(Subscriber subscriber) {
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
        return code;
    }


    private Map<String, Integer> getBasicPlanId() {
        Response r = InternalAppRequester.getSecuredRequest(AppConstants.GET_BASIC_PLAN_ID);
        if (r.getStatus() == 200) {
            return r.readEntity(Map.class);
        }
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


    @POST
    @Path("general-activities")
    @UserJwt
    public Response createGeneralActivities(List<GeneralActivity> activities) {
        activities.forEach(ga -> dao.persist(ga));
        return Response.status(200).build();
    }

    @GET
    @Path("general-activities")
    @UserJwt
    public Response getGeneralActivities() {
        List<GeneralActivity> activities = dao.get(GeneralActivity.class);
        return Response.status(200).entity(activities).build();
    }

    @GET
    @Path("general-roles")
    @UserJwt
    public Response getGeneralRoles() {
        List<GeneralRole> roles = dao.get(GeneralRole.class);
        return Response.status(200).entity(roles).build();
    }

    @POST
    @Path("general-role")
    @UserJwt
    public Response createGeneralRole(GeneralRole generalRole) {
        dao.persist(generalRole);
        return Response.status(200).entity(generalRole).build();
    }

    @PUT
    @Path("general-role")
    @UserJwt
    public Response updateGeneralRole(GeneralRole generalRole) {
        dao.update(generalRole);
        return Response.status(200).entity(generalRole).build();
    }

    private int getPlanRoleId(int planId, String header) {
        Response r = this.getSecuredRequest(AppConstants.getPlanGeneralRoleId(planId), header);
        if (r.getStatus() == 200) {
            Map<String, Integer> map = r.readEntity(Map.class);
            return map.get("generalRoleId");
        }
        return 0;
    }

    @POST
    @Path("label")
    @UserJwt
    public Response createLabel(Label label) {
        label.setCreated(new Date());
        Label check = dao.findCondition(Label.class, "label" , label.getLabel());
        if(check != null){
            throwError(409);
        }
        dao.persist(label);
        return Response.status(200).entity(label).build();
    }

    @GET
    @Path("labels")
    @UserJwt
    public Response getLabels(){
        List<Label> labels = dao.get(Label.class);
        return Response.status(200).entity(labels).build();
    }


    @UserSubscriberJwt
    @GET
    @Path("qvm-invoice/{invoiceId}/company/{companyId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getInvoice(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, @PathParam(value = "invoiceId") int salesId, @PathParam(value = "companyId") int companyId) {
        Subscription subscription = dao.findTwoConditions(Subscription.class, "salesId", "companyId", salesId, companyId);
        Company company = dao.find(Company.class, companyId);
        verifyObjectFound(subscription);
        verifyObjectFound(company);
        MessagingModel mm = async.createInvoiceEmailModel(subscription, company, header);
        Response r = InternalAppRequester.postSecuredRequest(AppConstants.POST_GENERATE_HTML, mm);
        if (r.getStatus() == 200) {
            String body = r.readEntity(String.class);
            return Response.ok().entity(body).build();
        }
        throwError(404);
        return null;
    }

    @InternalApp
    @POST
    @Path("companies/reduced")
    public Response getCompanyReduced(Map<String, Object> map) {
        List<Integer> ids = (ArrayList) map.get("companyIds");
        String sql = "select * from sub_company b where b.id in (0";
        for (var id : ids) {
            sql += "," + id;
        }
        sql += ")";
        List<CompanyReduced> companyReducedList = dao.getNative(CompanyReduced.class, sql);
        return Response.ok().entity(companyReducedList).build();
    }


    @UserSubscriberJwt
    @POST
    @Path("subscribe")
    public Response createSubscription(@HeaderParam(HttpHeaders.AUTHORIZATION) String header, SubscribeModel model) {
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
        Subscriber admin = company.getAdminSubscriber();
        int roleId = getPlanRoleId(model.getPlanId(), header);
        GeneralRole gr = dao.find(GeneralRole.class, roleId);
        admin.getRoles().clear();
        admin.getRoles().add(gr);
        dao.update(company);
        Company updated = dao.find(Company.class, company.getId());
        async.sendInvoiceEmail(premium, updated, header);
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
}