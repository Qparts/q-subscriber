package q.rest.subscriber.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.json.bind.annotation.JsonbDateFormat;
import java.util.Date;

public class SalesInvoiceModel {
    private int salesId;
    private int paymentOrderId;
    private char paymentMethod;
    private int actualDays;
    @JsonbDateFormat(value = JsonbDateFormat.TIME_IN_MILLIS)
    private Date invoiceDate;
    private double baseAmount;
    private double planDiscount;
    private double promoDiscount;
    private double vatPercentage;
    private String vatNumber;


    @JsonIgnore
    public double getSubtotal(){
        return baseAmount - planDiscount - promoDiscount;
    }
    @JsonIgnore
    public double getVatAmount(){
        return getSubtotal() * vatPercentage;
    }
    @JsonIgnore
    public double getNetTotal(){
        return getSubtotal() + getVatAmount();
    }

    public int getPaymentOrderId() {
        return paymentOrderId;
    }

    public void setPaymentOrderId(int paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
    }

    public char getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(char paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getActualDays() {
        return actualDays;
    }

    public void setActualDays(int actualDays) {
        this.actualDays = actualDays;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(double baseAmount) {
        this.baseAmount = baseAmount;
    }

    public double getPlanDiscount() {
        return planDiscount;
    }

    public void setPlanDiscount(double planDiscount) {
        this.planDiscount = planDiscount;
    }

    public double getPromoDiscount() {
        return promoDiscount;
    }

    public void setPromoDiscount(double promoDiscount) {
        this.promoDiscount = promoDiscount;
    }

    public double getVatPercentage() {
        return vatPercentage;
    }

    public void setVatPercentage(double vatPercentage) {
        this.vatPercentage = vatPercentage;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public int getSalesId() {
        return salesId;
    }

    public void setSalesId(int salesId) {
        this.salesId = salesId;
    }
}
