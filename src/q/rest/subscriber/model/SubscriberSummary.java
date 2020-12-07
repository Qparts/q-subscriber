package q.rest.subscriber.model;

import q.rest.subscriber.model.entity.keywords.SearchKeyword;
import q.rest.subscriber.model.entity.keywords.SearchReplacementKeyword;

import java.util.List;

public class SubscriberSummary {
    private int searchesToday;
    private int replacementSearchesToday;
    private int totalSearches;
    private int totalReplacementSearches;
    private int activeCompanies;
    private int totalCompanies;
    private List<SearchKeyword> topKeywords;
    private List<SearchReplacementKeyword> topReplacementsKeywords;
    private List<Integer> topCompanies;
    private List<MonthlySearches> monthlySearches;
    private List<MonthlySearches> replacementMonthlySearches;

    public int getTotalReplacementSearches() {
        return totalReplacementSearches;
    }

    public void setTotalReplacementSearches(int totalReplacementSearches) {
        this.totalReplacementSearches = totalReplacementSearches;
    }

    public List<MonthlySearches> getReplacementMonthlySearches() {
        return replacementMonthlySearches;
    }

    public void setReplacementMonthlySearches(List<MonthlySearches> replacementMonthlySearches) {
        this.replacementMonthlySearches = replacementMonthlySearches;
    }

    public int getReplacementSearchesToday() {
        return replacementSearchesToday;
    }

    public void setReplacementSearchesToday(int replacementSearchesToday) {
        this.replacementSearchesToday = replacementSearchesToday;
    }

    public List<SearchReplacementKeyword> getTopReplacementsKeywords() {
        return topReplacementsKeywords;
    }

    public void setTopReplacementsKeywords(List<SearchReplacementKeyword> topReplacementsKeywords) {
        this.topReplacementsKeywords = topReplacementsKeywords;
    }

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
