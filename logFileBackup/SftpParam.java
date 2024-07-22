package cn.capinfo.ccaf.monitor.busi.manager.logic.util;

/**
 * sftp param
 * 包括 host、port、username及password
 */
public class SftpParam {

    private String host;
    private Integer port;
    private String username;
    private String password;

    public SftpParam() {
    }

    public SftpParam(String host, Integer port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
