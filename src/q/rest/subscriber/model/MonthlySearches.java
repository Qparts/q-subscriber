package q.rest.subscriber.model;

public class MonthlySearches {
    private String month;
    private int year;
    private int count;

    public MonthlySearches(String month, int year, int count) {
        this.month = month;
        this.year = year;
        this.count = count;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
