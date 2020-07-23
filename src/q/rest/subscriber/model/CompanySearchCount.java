package q.rest.subscriber.model;

import java.util.Date;

public class CompanySearchCount {
    private int companyId;
    private String name;
    private int count;
    private Date date;

    public CompanySearchCount(int companyId, String name, int count) {
        this.companyId = companyId;
        this.name = name;
        this.count = count;
    }

    public CompanySearchCount(Date date, int companyId, int count) {
        this.companyId = companyId;
        this.date = date;
        this.count = count;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }


}
