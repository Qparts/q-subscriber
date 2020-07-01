package q.rest.subscriber.model;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "error")
public class Error {
    private int statusCode;
    private String statusDescription;
    private String errorMessage;
    private Object errorLog;

    public Error(int statusCode, String statusDescription, String errorMessage, Object errorLog) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.errorMessage = errorMessage;
        this.errorLog = errorLog;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(Object errorLog) {
        this.errorLog = errorLog;
    }
}
