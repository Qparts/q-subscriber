package q.rest.subscriber.dao;

import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.model.AddSubscriberModel;
import q.rest.subscriber.model.SignupModel;
import q.rest.subscriber.model.WebApp;
import q.rest.subscriber.model.entity.*;
import q.rest.subscriber.model.entity.role.company.CompanyRole;
import q.rest.subscriber.model.entity.role.general.GeneralRole;
import q.rest.subscriber.model.publicapi.*;
import q.rest.subscriber.model.view.CompanyView;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.*;

@Stateless
public class DaoApi {
    @EJB
    private DAO dao;
    private Helper helper = new Helper();

    public List<PbCompanyVisible> searchCompaniesByName(String query, int companyId){
        var name = "%" + query.toLowerCase().trim() + "%";
        String sql = "select b from PbCompanyVisible b where b.id != :value0" +
                " and lower(b.name) like :value1" +
                " and lower(b.nameAr) like :value1";
        return dao.getJPQLParams(PbCompanyVisible.class, sql, companyId, name);
    }

    public Subscriber findSubscriber(String email, String password) {
        return dao.findTwoConditions(Subscriber.class, "email", "password", email, password);
    }

    public boolean subscriberExists(String email, String mobile){
        var sql = "select b from Subscriber b where (b.mobile =:value0 or b.email =:value1)";
        var check = dao.getJPQLParams(Subscriber.class, sql, mobile, email);
        return !check.isEmpty();
    }

    public boolean signupRequestExists(String email, String mobile){
        var sql = "select b from SignupRequest b where (b.mobile =:value0 or b.email =:value1) and b.status = :value2 and b.created > :value3";
        var check = dao.getJPQLParams(SignupRequest.class, sql, mobile, email, 'R', Helper.addMinutes(new Date(), -60));
        return !check.isEmpty();
    }

    public Subscriber findSubscriberByEmail(String email) {
        String sql = "select b from Subscriber b where b.email =:value0";
        return dao.findJPQLParams(Subscriber.class, sql, email);
    }

    public PasswordReset findPasswordResetObject(String token){
        String sql = "select b from PasswordReset b where b.token = :value0 and b.status = :value1 and b.expire >= :value2";
        return dao.findJPQLParams(PasswordReset.class, sql, token, 'R', new Date());
    }

    public String createPasswordResetObject(int subscriberId) {
        String code = "";
        boolean available = false;
        do {
            code = Helper.getRandomString(20);
            String sql = "select b from PasswordReset b where b.token = :value0 and b.expire >= :value1 and status =:value2";
            List<PasswordReset> l = dao.getJPQLParams(PasswordReset.class, sql, code, new Date(), 'R');
            if (l.isEmpty())
                available = true;

        } while (!available);

        int expireMinutes = 60 * 24 * 14;
        PasswordReset ev = new PasswordReset(subscriberId, code, expireMinutes);
        dao.persist(ev);
        return code;
    }

    public void resetSubscriberPassword(PasswordReset passwordReset, String newPassword){
        Subscriber subscriber = findSubscriber(passwordReset.getSubscriberId());
        subscriber.setPassword(Helper.cypher(newPassword));
        subscriber.setEmailVerified(true);
        dao.update(subscriber);
        passwordReset.setStatus('V');
        dao.update(passwordReset);
    }

    public Subscriber findSubscriber(int id){
        return dao.find(Subscriber.class, id);
    }

    public PbBranch createBranch(Branch branch, int companyId, int subscriberId){
        branch.setCompanyId(companyId);
        branch.setCreated(new Date());
        branch.setNameAr(branch.getName());
        branch.setCreatedBySubscriber(subscriberId);
        branch.setStatus('A');
        System.out.println(branch);
        dao.persist(branch);
        System.out.println("finding branch id " + branch.getId());
        var pbBranch = dao.find(PbBranch.class, branch.getId());
        List<PbBranch> branches = dao.getCondition(PbBranch.class, "companyId", pbBranch.getCompanyId());
        System.out.println("branches size " + branches.size());
        if(branches.size() == 1)
            makeDefaultBranch(pbBranch.getCompanyId(), pbBranch.getId());
        return pbBranch;
    }

    public void updateInvoiceTemplate(String template, int companyId){
        var sql = "update sub_company_profile_settings set invoice_template = '"+template+"' where company_id = " + companyId;
        dao.updateNative(sql);
    }

    public void updateVatNumber(String vatNumber, int companyId){
        String quoted = "'" + vatNumber + "'";
        String sql = "insert into sub_company_profile_settings (company_id, vat_number) values (" + companyId + ", " + quoted + ")" +
                "on conflict (company_id) do update set vat_number = " + quoted;
        dao.insertNative(sql);
    }

    public void updateDefaultPurchaseTax(double purchaseTax, int companyId){
        String sql = "insert into sub_company_profile_settings (company_id, default_purchase_tax) values (" + companyId + ", " + purchaseTax + ")" +
                "on conflict (company_id) do update set default_purchase_tax = " + purchaseTax;
        dao.insertNative(sql);
    }


    public void updateDefaultSalesTax(double salesTax, int companyId){
        String sql = "insert into sub_company_profile_settings (company_id, default_sales_tax) values (" + companyId + ", " + salesTax + ")" +
                "on conflict (company_id) do update set default_sales_tax = " + salesTax;
        dao.insertNative(sql);
    }

    public void updateDefaultCurrency(String currency, int companyId){
        var sql = "update sub_company_profile_settings set default_currency = '"+currency+"' where company_id = " + companyId;
        dao.updateNative(sql);
    }

    public void updateLogoUploaded(Boolean uploaded, int companyId){
        var sql = "update sub_company_profile_settings set logo_uploaded = "+uploaded+" where company_id = " + companyId;
        dao.updateNative(sql);
    }


    public void makeDefaultPolicy(int policyId, int companyId){
        var sql = "insert into sub_company_profile_settings (company_id, default_policy_id) values (" + companyId + ", " + policyId +")" +
                " on conflict (company_id) do update set default_policy_id = " + policyId;
        dao.insertNative(sql);
    }

    public void createCompanyRole(CompanyRole role, int companyId){
        role.setCompanyId(companyId);
        dao.persist(role);
    }


    public void makeDefaultCustomer(int customerId, int companyId){
        String sql = "insert into sub_company_profile_settings (company_id, default_customer_id) values (" + companyId + ", " + customerId +")" +
                "on conflict (company_id) do update set default_customer_id = " + customerId;
        dao.insertNative(sql);
    }


    public void makeDefaultBranch(int companyId, int branchId){
        String sql = "insert into sub_company_profile_settings (company_id, default_branch_id) values (" + companyId + ", " + branchId +")" +
                "on conflict (company_id) do update set default_branch_id = " + branchId;
        dao.insertNative(sql);
    }

    public Company findCompany(int id){
        return dao.find(Company.class, id);
    }

    public PbCompany findPbCompany(int id) {
        return dao.find(PbCompany.class, id);
    }

    public CompanyView findCompanyView(int companyId){
        return dao.find(CompanyView.class, companyId);
    }

    public PbSubscriber findPbSubscriber(int subscriberId){
        return dao.find(PbSubscriber.class, subscriberId);
    }

    public SignupRequest createAdditionalSubscriberSignupRequest(AddSubscriberModel model, int companyId, int subscriberId){
        model.setCompanyId(companyId);
        model.setCreatedBySubscriber(subscriberId);
        SignupRequest sr = new SignupRequest(model);
        dao.persist(sr);
        return sr;
    }

    public PbSubscriber createAdditionalSubscriber(int companyId, SubscriberVerification verification){
        SignupRequest signupRequest = getSignupRequest(verification.getSignupRequestId());

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(signupRequest.getEmail());
        subscriber.setMobile(signupRequest.getMobile());
        subscriber.setName(signupRequest.getName());
        subscriber.setCreated(new Date());
        subscriber.setCreatedBy(signupRequest.getCreatedBy());
        subscriber.setEmailVerified(verification.getVerificationMode() == 'E');
        subscriber.setMobileVerified(verification.getVerificationMode() == 'M');
        subscriber.setAdmin(false);
        subscriber.setPassword(signupRequest.getPassword());
        subscriber.setCompanyId(companyId);
        subscriber.setStatus('A');

        Subscriber admin = dao.findTwoConditions(Subscriber.class, "companyId", "admin", companyId, true);
        for (var role : admin.getRoles()) {
            GeneralRole gr = dao.find(GeneralRole.class, role.getId());
            subscriber.getRoles().add(gr);
        }

        dao.persist(subscriber);
        deleteVerification(verification);
        signupRequest.setStatus('C');
        dao.update(signupRequest);
        var pbSubscriber = findPbSubscriber(subscriber.getId());
        pbSubscriber.setDefaultBranch(signupRequest.getDefaultBranch());
        dao.update(pbSubscriber);
        return pbSubscriber;
    }

    public SignupRequest getSignupRequest(Date date, String email){
        String sql = "select b from SignupRequest b where b.created > :value0 and b.email = :value1";
        return dao.findJPQLParams(SignupRequest.class, sql, date, email);
    }


    public SignupRequest getSignupRequest(int id){
        return dao.find(SignupRequest.class, id);
    }

    public SubscriberVerification getSubscriberVerification(String code, Date date, int signupRequestId){
        var sql = "select b from SubscriberVerification b " +
                " where b.verificationCode = :value0 " +
                " and b.created > :value1 " +
                " and b.stage = :value2 " +
                " and b.signupRequestId = :value3";
        return dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 1, signupRequestId);
    }

    public SubscriberVerification getSubscriberVerification(String code, int companyId){
        Date date = Helper.addMinutes(new Date(), -60);
        String sql = "select b from SubscriberVerification b " +
                " where b.verificationCode = :value0 " +
                " and b.created > :value1 " +
                " and b.stage = :value2 " +
                " and b.companyId = :value3";
        return dao.findJPQLParams(SubscriberVerification.class, sql, code, date, 3, companyId);
    }

    public void deleteVerification(SubscriberVerification verification){
        dao.delete(verification);
    }

    public void updateSignupRequest(SignupRequest request){
        dao.update(request);
    }

    public List<PbSubscriberVisible> getCompanyVisibleSubscriber(int companyId){
        String sql = "SELECT b from PbSubscriberVisible b where b.company.id = :value0";
        return dao.getJPQLParams(PbSubscriberVisible.class, sql, companyId);
    }

    public List<PbCompany> getCompaniesByIds(String ids){
        String[] idsArray = ids.split(",");
        StringBuilder sql = new StringBuilder("select * from sub_company where id in (0");
        for (String s : idsArray) {
            sql.append(",").append(s);
        }
        sql.append(") order by id");
        return dao.getNative(PbCompany.class, sql.toString());
    }

    public List<PbCompanyVisible> getVisibleCompaniesByIds(String ids){
        String[] idsArray = ids.split(",");
        StringBuilder sql = new StringBuilder("select * from sub_company where status = 'A' and id in (0");
        for (String s : idsArray) {
            sql.append(",").append(s);
        }
        sql.append(") order by id");
        return dao.getNative(PbCompanyVisible.class, sql.toString());
    }

    public RefreshToken findRefreshToken(String refreshToken, int subscriberId, int appCode){
        String sql = "select b from RefreshToken b where b.subscriberId = :value0 " +
                " and b.appCode = :value1" +
                " and b.token = :value2" +
                " and b.status = :value3" +
                " and b.expiresAt > :value4";
        return dao.findJPQLParams(RefreshToken.class, sql, subscriberId, appCode, refreshToken, 'A', new Date());
    }

    public SignupRequest createSignupRequest(SignupModel signupModel, int appCode){
        SignupRequest signupRequest = new SignupRequest(signupModel, appCode);
        dao.persist(signupRequest);
        return signupRequest;
    }


    public String createAndGetVerificationCode(SignupRequest signupRequest, int countryId) {
        String code = createVerificationCode();
        var sv = new SubscriberVerification(signupRequest, countryId == 1 ? 'M' : 'E', code);
        dao.persist(sv);
        return code;
    }

    public boolean isSearchReplacementsUnlimited(int subscriberId){
        String sql = "select count(*) from sub_general_role_activity ra where ra.activity_id = 15" +
                "and ra.role_id in (" +
                "    select sr.role_id from sub_subscriber_general_role sr where subscriber_id = " + subscriberId + ")";
        Number n = dao.findNative(Number.class, sql);
        return n.intValue() > 0;
    }

    public int getNumberOfReplacementSearchUsed(int companyId){
        var sql = "select count(*) from SearchReplacementKeyword where found = :value0 and companyId = :value1";
        Number number = dao.findJPQLParams(Number.class, sql, true, companyId);
        return number.intValue();
    }

    public int getNumberOfPartsSearchUsed(int companyId){
        var sql = "select count(*) from SearchKeyword where found = :value0 and companyId = :value1" +
                " and cast (created as date) = cast (now() as date)";
        Number number = dao.findJPQLParams(Number.class, sql, true, companyId);
        return number.intValue();
    }


    public boolean isSearchPartsUnlimited(int subscriberId){
        var sql = "select count(*) from sub_general_role_activity ra where ra.activity_id = 13" +
                "and ra.role_id in (" +
                "    select sr.role_id from sub_subscriber_general_role sr where subscriber_id = " + subscriberId + ")";
        Number n = dao.findNative(Number.class, sql);
        return n.intValue() > 0;
    }

    public boolean isDashboardMetricsAllowed(int subscriberId){
        var sql = "select count(*) from sub_general_role_activity ra where ra.activity_id = 16" +
                "and ra.role_id in (" +
                "    select sr.role_id from sub_subscriber_general_role sr where subscriber_id = " + subscriberId + ")";
        Number n = dao.findNative(Number.class, sql);
        return n.intValue() > 0;
    }


    public Subscription getActiveSubscription(int companyId){
        return dao.findTwoConditions(Subscription.class, "companyId", "status", companyId, 'A');
    }

    public void doExpireSubscription(Subscription subscription){
        subscription.setStatus('E');
        dao.update(subscription);
    }

    public void doActivateSubscription(Subscription subscription){
        subscription.setStatus('A');
        dao.update(subscription);
    }

    public Subscriber downgradeSubscriber(Subscriber subscriber, int roleId){
        //downgrade
        Company company = dao.find(Company.class, subscriber.getCompanyId());
        GeneralRole role = dao.find(GeneralRole.class, roleId);
        for (Subscriber s : company.getSubscribers()) {
            s.getRoles().clear();
            s.getRoles().add(role);
            dao.update(s);
            if (subscriber.getId() == s.getId()) {
                subscriber = s;
            }
        }
        return subscriber;
    }

    public Subscription getFutureSubscription(int companyId){
        String jpql = "select b from Subscription b where b.companyId = :value0 and b.status = :value1 and b.startDate < :value2";
        return dao.findJPQLParams(Subscription.class, jpql, companyId, 'F', new Date());
    }

    public String createAndGetVerificationCode(SignupRequest signupRequest, int countryId, int companyId) {
        String code = createVerificationCode();
        var sv = new SubscriberVerification(signupRequest, countryId == 1 ? 'M' : 'E', code, companyId);
        dao.persist(sv);
        return code;
    }

    private String createVerificationCode(){
        String code = "";
        boolean available = false;
        do {
            code = String.valueOf(Helper.getRandomInteger(1000, 9999));
            Date date = Helper.addMinutes(new Date(), -60);
            String sql = "select b from SubscriberVerification b where b.verificationCode = :value0 and b.created >= :value1";
            var l = dao.getJPQLParams(SubscriberVerification.class, sql, code, date);
            if (l.isEmpty())
                available = true;

        } while (!available);
        return code;
    }

    // retrieves app object from app secret
    public WebApp getWebAppFromSecret(String secret) throws Exception {
        WebApp webApp = dao.findTwoConditions(WebApp.class, "appSecret", "active", secret, true);
        if (webApp == null)
            throw new Exception();
        return webApp;
    }

    public List<Map<String, Object>>  getMostSearchedKeywords(){
        String sql = "select query, count(*) from sub_search_keyword where found = true group by query order by count desc";
        List<Object> ss = dao.getNativeMax(sql, 5);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object o : ss) {
            if (o instanceof Object[]) {
                Object[] objArray = (Object[]) o;
                String query = objArray[0].toString();
                int count = ((Number) objArray[1]).intValue();
                Map<String, Object> map = new HashMap<>();
                map.put("keywords", query);
                map.put("count", count);
                list.add(map);
            }
        }
        return list;
    }

    public List<Integer> getBranchIds(int companyId){
        var sql = "select b.id from Branch b where companyId = :value0";
        return dao.getJPQLParams(Integer.class, sql, companyId);
    }


    public void killTokens(int userId, int appCode) {
        String sql = "select b from RefreshToken b where b.subscriberId = :value0 and b.status = :value1 and b.appCode = :value2";
        List<RefreshToken> oldTokens = dao.getJPQLParams(RefreshToken.class, sql, userId, 'A', appCode);
        for (var ort : oldTokens) {
            ort.setStatus('K');//kill previous tokens
            dao.update(ort);
        }
    }


    public void saveRefreshToken(String jwt, int userId, Date issued, Date expire, int appCode) {
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



}
