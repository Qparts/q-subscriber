package q.rest.subscriber.model;

public class MessagingModel {
    private String mobile;
    private String email;
    private String purpose;
    private String[] values;

    public MessagingModel(String mobile, String email, String purpose, String[] values) {
        this.mobile = mobile;
        this.purpose = purpose;
        this.values = values;
        this.email = email;
    }

    public MessagingModel(String mobile, String email, String purpose, String value) {
        this.mobile = mobile;
        this.email = email;
        this.purpose = purpose;
        this.values = new String[] {value};
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
