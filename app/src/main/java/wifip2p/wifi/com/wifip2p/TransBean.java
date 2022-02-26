package wifip2p.wifi.com.wifip2p;

import java.io.Serializable;

public class TransBean implements Serializable {
    public String type;

    public String text;

    public FileBean fileBean;

    public TransBean(String type, String text, FileBean fileBean) {
        this.type = type;
        this.text = text;
        this.fileBean = fileBean;
    }
}
