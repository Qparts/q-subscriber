package q.rest.subscriber.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "sub_apps")
public class WebApp implements Serializable{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "app_code", updatable=false)
    private Integer appCode;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "app_secret")
    private String appSecret;

    @Column(name="active")
    private boolean active;

    public Integer getAppCode() {
        return appCode;
    }

    public void setAppCode(Integer appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appCode == null) ? 0 : appCode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebApp other = (WebApp) obj;
        if (appCode == null) {
            if (other.appCode != null)
                return false;
        } else if (!appCode.equals(other.appCode))
            return false;
        return true;
    }


}