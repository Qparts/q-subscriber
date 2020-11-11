package q.rest.subscriber.model;

import q.rest.subscriber.model.entity.keywords.SearchKeyword;

import java.util.List;

public class SubscriberSummary {
    private int searchesToday;
    private int totalSearches;
    private int activeCompanies;
    private int totalCompanies;
    private List<SearchKeyword> topKeywords;
    private List<Integer> topCompanies;
    private List<MonthlySearches> monthlySearches;

    public int getSearchesToday() {
        return searchesToday;
    }

    public void setSearchesToday(int searchesToday) {
        this.searchesToday = searchesToday;
    }

    public int getActiveCompanies() {
        return activeCompanies;
    }

    public void setActiveCompanies(int activeCompanies) {
        this.activeCompanies = activeCompanies;
    }

    public int getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(int totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public List<SearchKeyword> getTopKeywords() {
        return topKeywords;
    }

    public void setTopKeywords(List<SearchKeyword> topKeywords) {
        this.topKeywords = topKeywords;
    }

    public List<Integer> getTopCompanies() {
        return topCompanies;
    }

    public void setTopCompanies(List<Integer> topCompanies) {
        this.topCompanies = topCompanies;
    }

    public List<MonthlySearches> getMonthlySearches() {
        return monthlySearches;
    }

    public void setMonthlySearches(List<MonthlySearches> monthlySearches) {
        this.monthlySearches = monthlySearches;
    }

    public int getTotalSearches() {
        return totalSearches;
    }

    public void setTotalSearches(int totalSearches) {
        this.totalSearches = totalSearches;
    }
}
