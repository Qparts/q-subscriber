package q.rest.subscriber.operation;

import q.rest.subscriber.dao.DAO;
import q.rest.subscriber.helper.AppConstants;
import q.rest.subscriber.helper.Helper;
import q.rest.subscriber.helper.InternalAppRequester;
import q.rest.subscriber.model.MessagingModel;
import q.rest.subscriber.model.SalesInvoiceModel;
import q.rest.subscriber.model.entity.Company;
import q.rest.subscriber.model.entity.LoginAttempt;
import q.rest.subscriber.model.entity.Subscription;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Map;

@Stateless
public class AsyncService {

    @EJB
    private DAO dao;

    @Asynchronous
    public void sendInvoiceEmail(Subscription subscription, Company company, String header) {
        MessagingModel mm = createInvoiceEmailModel(subscription, company, header);
        if (mm != null) {
            sendEmail(mm);//sending email
        }
    }

    public MessagingModel createInvoiceEmailModel(Subscription subscription, Company company, String header) {
        Response r = this.getSecuredRequest(AppConstants.getSalesInvoice(subscription.getSalesId()), header);
        SalesInvoiceModel salesModel = r.readEntity(SalesInvoiceModel.class);
        Response r2 = this.getSecuredRequest(AppConstants.getPlanNames(subscription.getPlanId()), header);
        Map<String, String> planObject = r2.readEntity(Map.class);
        if (r.getStatus() == 200 && r2.getStatus() == 200) {
            String paymentMethodAr = "";
            String paymentMethodEn = "";
            if (salesModel.getPaymentMethod() == 'C') {
                paymentMethodAr = "بطاقة ائتمانية";
                paymentMethodEn = "Credit/Debit Card";
            } else if (salesModel.getPaymentMethod() == 'W') {
                paymentMethodAr = "تحويل بنكي";
                paymentMethodEn = "Bank Transfer";
            }
            Helper h = new Helper();
            String[] s = new String[21];
            s[0] = String.valueOf(subscription.getSalesId());
            s[1] = String.valueOf(salesModel.getPaymentOrderId());
            s[2] = String.valueOf(company.getId());
            s[3] = company.getName();
            s[4] = paymentMethodEn;
            s[5] = company.getNameAr();
            s[6] = paymentMethodAr;
            s[7] = String.valueOf(salesModel.getActualDays());
            s[8] = planObject.get("name");
            s[9] = planObject.get("nameAr");
            s[10] = h.getDateFormat(salesModel.getInvoiceDate(), "dd-MMM-yyyy");
            s[11] = h.getDateFormat(subscription.getStartDate(), "dd-MMM-yyyy");
            s[12] = h.getDateFormat(subscription.getEndDate(), "dd-MMM-yyyy");
            s[13] = h.getCurrencyFormat(salesModel.getBaseAmount());
            s[14] = h.getCurrencyFormat(salesModel.getPlanDiscount() + salesModel.getPromoDiscount());
            s[15] = h.getCurrencyFormat(salesModel.getSubtotal());
            s[16] = "1";
            s[17] = salesModel.getVatPercentage() * 100 + "%";
            s[18] = salesModel.getVatNumber();
            s[19] = h.getCurrencyFormat(salesModel.getVatAmount());
            s[20] = h.getCurrencyFormat(salesModel.getNetTotal());
            return new MessagingModel(null, company.getAdminSubscriber().getEmail(), AppConstants.MESSAGING_PURPOSE_INVOICE, s);
        }
        return null;
    }
    public void createLoginAttempt(String username, int subscriberId, String ip, boolean success){
        var loginAttempt = new LoginAttempt();
        loginAttempt.setUsername(username);
        loginAttempt.setCreated(new Date());
        loginAttempt.setSuccess(success);
        loginAttempt.setSubscriberId(subscriberId);
        loginAttempt.setIp(ip);
        dao.persist(loginAttempt);
    }

    @Asynchronous
    public void informAdminsNewRegistration(String companyname){
        String mobile = "";
        String body = "New signup in qvm needs approval: " + companyname;
        MessagingModel smsModel = new MessagingModel(mobile, null, AppConstants.MESSAGING_PURPOSE_SIGNUP_ADMIN_NOTIFY, body);

    }

    @Asynchronous
    public void sendSms(MessagingModel smsModel) {
        Response r = InternalAppRequester.postSecuredRequest(AppConstants.SEND_SMS, smsModel);
        System.out.println("SMS Response " + r.getStatus());
    }

    @Asynchronous
    public void sendEmail(MessagingModel model) {
        Response r = InternalAppRequester.postSecuredRequest(AppConstants.SEND_EMAIL, model);
        System.out.println("EMAIL Response " + r.getStatus() + " - " + r.readEntity(String.class));
    }

    public <T> Response getSecuredRequest(String link, String authHeader) {
        Invocation.Builder b = ClientBuilder.newClient().target(link).request();
        b.header(HttpHeaders.AUTHORIZATION, authHeader);
        Response r = b.get();
        return r;
    }
}
