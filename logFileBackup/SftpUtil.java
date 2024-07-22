package cn.capinfo.ccaf.monitor.busi.manager.logic.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * sftp工具类
 *
 * @author songpeng@capinfo.com.cn
 * @since 2024-06-11
 */
@Slf4j
public class SftpUtil {

    private SshClient sshClient;
    private ClientSession session;
    private SftpClient sftpClient;

    public static SftpUtil newSftpInstance(SftpParam param) {
        final SftpUtil sftp = new SftpUtil();
        try {
            sftp.connect(param);
        } catch (IOException e) {
            if (sftp.sftpClient != null) {
                try {
                    sftp.sftpClient.close();
                } catch (IOException excep) {}
            }
            if (sftp.session != null) {
                try {
                    sftp.session.close();
                } catch (IOException excep) {}
            }
            if (sftp.sshClient != null && !sftp.sshClient.isClosed()) {
                try {
                    sftp.sshClient.close();
                } catch (IOException excep) {}
                sftp.sshClient.stop();
            }

            throw new RuntimeException(e);
        }
        return sftp;
    }

    public void connect(SftpParam sftpParam) throws IOException {
        if (sftpClient != null) {
            sftpClient.close();
        }
        if (session != null && session.isAuthenticated()) {
            session.close();
        }
        if (sshClient == null || !sshClient.isOpen()) {
            sshClient = SshClient.setUpDefaultClient();
            sshClient.start();
        }
        try{
            session = sshClient.connect(sftpParam.getUsername(), sftpParam.getHost(), sftpParam.getPort())
                    .verify(10000, TimeUnit.MILLISECONDS)
                    .getSession();
            session.addPasswordIdentity(sftpParam.getPassword());
            session.auth().verify(10000, TimeUnit.MILLISECONDS);
            sftpClient = SftpClientFactory.instance().createSftpClient(session);
        }catch(IOException e) {
            log.error("sftpClient connect fail: " +sftpParam );
            throw e;
        }
    }

    public void disconnect() {
        if (sftpClient != null) {
            try {
                sftpClient.close();
            } catch (IOException e) {}
        }
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {}
        }
        if (sshClient != null && !sshClient.isClosed()) {
            try {
                sshClient.close();
            } catch (IOException e) {}

            sshClient.stop();
        }
    }


    /**
     * 下载文件
     * @param remoteFilePath 远程文件路径
     * @param localFilePath 下载后的本地文件路径
     * @throws IOException
     */
    public void downloadFile(String remoteFilePath , String localFilePath) throws IOException {
        Path localPath = Paths.get(localFilePath);
        if (Files.notExists(localPath.getParent())) {
            Files.createDirectories(localPath.getParent());
        }
        if (Files.exists(localPath) && !Files.isWritable(localPath)) {
            throw new IOException("本地文件路径不可写: " + localFilePath);
        }

        log.info("开始下载文件: " + remoteFilePath);
        try (SftpClient.CloseableHandle handle = sftpClient.open(remoteFilePath, SftpClient.OpenMode.Read);
             FileChannel fileChannel = FileChannel.open(localPath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {

            long fileSize = sftpClient.stat(handle).getSize();
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);  // 1 MB 缓冲区
            long position = 0;
            while (position < fileSize) {
                buffer.clear();
                int bytesRead = sftpClient.read(handle, position, buffer.array(), 0, buffer.capacity());
                if (bytesRead == -1) break;
                buffer.limit(bytesRead);
                fileChannel.write(buffer);
                position += bytesRead;
            }
            log.info("文件下载完成: " + localFilePath);
        }catch (IOException e) {
            log.error("下载文件失败: " + remoteFilePath, e);
            throw e;
        } finally {
            sftpClient.close();
            session.close();
            sshClient.stop();
        }

    }

    /**
     * 上传文件
     * @param localFilePath 本地文件路径
     * @param remoteFilePath 上传的远程文件路径
     * @throws IOException 异常处理
     */
    public void uploadFile(String localFilePath , String remoteFilePath) throws IOException {
        Path localPath = Paths.get(localFilePath);
        if (Files.notExists(localPath) || !Files.isReadable(localPath)) {
            throw new IOException("本地文件不存在或不可读: " + localFilePath);
        }
        log.info("开始上传文件: " + localFilePath);
        try (SftpClient.CloseableHandle handle = sftpClient.open(remoteFilePath, SftpClient.OpenMode.Write, SftpClient.OpenMode.Create, SftpClient.OpenMode.Truncate);
             FileChannel fileChannel = FileChannel.open(localPath, EnumSet.of(StandardOpenOption.READ))) {

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);  // 1 MB 缓冲区
            long position = 0;
            int bytesRead;
            while ((bytesRead = fileChannel.read(buffer)) != -1) {
                buffer.flip();
                sftpClient.write(handle, position, buffer.array(), 0, bytesRead);
                position += bytesRead;
                buffer.clear();
            }
            log.info("文件上传完成: " + remoteFilePath);
        }catch (IOException e) {
            log.error("上传文件失败: " + localFilePath, e);
            throw e;
        } finally {
            sftpClient.close();
            session.close();
            sshClient.stop();
        }

    }
}