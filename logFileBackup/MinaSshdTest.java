package cn.capinfo.ccaf.monitor.busi.manager.logic.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.security.Security;

/**
 * sshd sftp 使用测试
 */
@Slf4j
public class MinaSshdTest {

    public static void main(String[] args) {

        //远程主机服务器 应用日志文件下载
        String remoteFilePath = "/usr/local/tomcat/logs/catalina.out";
        //下载后的本地文件路径
        String localFilePath = "C:\\Users\\19791\\AppData\\Local\\Temp\\catalina.out";

        SftpParam sftpParam = new SftpParam("192.168.29.130",22,"root","Sp12");
        SftpUtil sftpUtil = SftpUtil.newSftpInstance(sftpParam);
        try {
            sftpUtil.downloadFile(remoteFilePath , localFilePath);
        }catch (IOException e){
            log.error("sftpClient downloadFile fail: " +sftpParam );
        }

        //上传的远程文件路径
        String remoteFilePath2 = "/root/catalina.out";
        SftpParam sftpParam2 = new SftpParam("192.168.29.131",22,"root","Sp12");
        SftpUtil sftpUtil2 = SftpUtil.newSftpInstance(sftpParam2);
        try {
            sftpUtil2.uploadFile(localFilePath,remoteFilePath2);
        }catch (IOException e){
            log.error("sftpClient uploadFile fail: " +sftpParam2 );
        }

    }
}
