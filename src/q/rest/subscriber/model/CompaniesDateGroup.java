package q.rest.subscriber.model;

import javax.json.bind.annotation.JsonbDateFormat;
import java.util.Date;

public class CompaniesDateGroup {
    @JsonbDateFormat(value = JsonbDateFormat.TIME_IN_MILLIS)
    private Date date;
    private int daily;
    private int total;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getDaily() {
        return daily;
    }

    public void setDaily(int daily) {
        this.daily = daily;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
