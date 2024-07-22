package cn.capinfo.ccaf.monitor.busi.manager.logic.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class MinaSshdFolderTest {
    public static void main(String[] args) {
        //远程主机服务器 应用日志文件下载
        String remoteFilePath = "/root/个人网上_log";
        //下载后的本地文件路径
        String localFilePath = "C:\\Users\\19791\\AppData\\Local\\Temp\\个人网上_log";

        SftpParam sftpParam = new SftpParam("192.168.29.130",22,"root","Sp12");
        SftpFolderUtil sftpFolderUtil = SftpFolderUtil.newSftpInstance(sftpParam);
        try {
            sftpFolderUtil.downloadDirectory(remoteFilePath , localFilePath);
        }catch (IOException e){
            log.error("sftpClient downloadDirectory fail: " +sftpParam );
        }finally {
            sftpFolderUtil.disconnect();
        }

        //上传的远程文件路径
        String remoteFilePath2 = "/root/个人网上_log";
        SftpParam sftpParam2 = new SftpParam("192.168.29.131",22,"root","Sp12");
        SftpFolderUtil sftpFolderUtil2 = SftpFolderUtil.newSftpInstance(sftpParam2);
        try {
            sftpFolderUtil2.uploadDirectory(localFilePath,remoteFilePath2);
        }catch (IOException e){
            log.error("sftpClient uploadDirectory fail: " +sftpParam2 );
            e.printStackTrace();
        }finally {
            sftpFolderUtil2.disconnect();
        }
    }
}
