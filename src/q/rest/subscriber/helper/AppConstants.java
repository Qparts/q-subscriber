package q.rest.subscriber.helper;

public class AppConstants {

    private final static String MESSAGING_SERVICE = SysProps.getValue("messagingService");
    private final static String PLAN_SERVICE = SysProps.getValue("planService");
    private final static String INVOICE_SERVICE = SysProps.getValue("invoiceService").replace("/v2/", "/v3/");

    public final static String INTERNAL_APP_SECRET = "INTERNAL_APP";
    public static final String ADMIN_EMAIL = "fareed@qetaa.com";
    public static final String SEND_SMS = MESSAGING_SERVICE + "sms";
    public static final String SEND_EMAIL = MESSAGING_SERVICE + "email";
    public static final String GET_BASIC_PLAN_ID = PLAN_SERVICE + "plan/basic/id";
    public static final String POST_GENERATE_HTML = MESSAGING_SERVICE + "generate-html";

    public static String getPlanGeneralRoleId(int planId){
        return PLAN_SERVICE + "general-role-id/plan/" + planId;
    }

    public static String getPlanNames(int planId){
        return PLAN_SERVICE + "names/" + planId;
    }



    public static String getSalesInvoice(int salesId){
        return INVOICE_SERVICE + "invoice/sales/" + salesId;
    }

    public static final String MESSAGING_PURPOSE_SIGNUP = "signup";
    public static final String MESSAGING_PURPOSE_API_TOKEN = "api-token";
    public static final String MESSAGING_PURPOSE_PASS_RESET = "password-reset";
    public static final String MESSAGING_PURPOSE_INVOICE = "subscription-invoice";
    public static final String MESSAGING_PURPOSE_NEW_PURCHASE_ORDER = "purchase-order-submit";
    public static final String MESSAGING_PURPOSE_ACCEPT_PURCHASE_ORDER = "purchase-order-accept";
    public static final String MESSAGING_PURPOSE_REFUSE_PURCHASE_ORDER = "purchase-order-refuse";
}
