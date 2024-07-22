package cn.capinfo.ccaf.monitor.busi.manager.logic.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.EnumSet;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * sftp 文件夹处理工具类
 *
 * @author songpeng@capinfo.com.cn
 * @since 2024-06-12
 */
@Slf4j
public class SftpFolderUtil {

    private SshClient sshClient;
    private ClientSession session;
    private SftpClient sftpClient;

    public static SftpFolderUtil newSftpInstance(SftpParam param) {
        final SftpFolderUtil sftpFolder = new SftpFolderUtil();
        try {
            sftpFolder.connect(param);
        } catch (IOException e) {
            if (sftpFolder.sftpClient != null) {
                try {
                    sftpFolder.sftpClient.close();
                } catch (IOException excep) {}
            }
            if (sftpFolder.session != null) {
                try {
                    sftpFolder.session.close();
                } catch (IOException excep) {}
            }
            if (sftpFolder.sshClient != null && !sftpFolder.sshClient.isClosed()) {
                try {
                    sftpFolder.sshClient.close();
                } catch (IOException excep) {}
                sftpFolder.sshClient.stop();
            }

            throw new RuntimeException(e);
        }
        return sftpFolder;
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
     * 下载文件夹
     * @param remoteDirPath
     * @param localDirPath
     * @throws IOException
     */
    public void downloadDirectory(String remoteDirPath, String localDirPath) throws IOException {
        Path localPath = Paths.get(localDirPath);
        if (Files.notExists(localPath) || !Files.isDirectory(localPath)) {
            Files.createDirectories(localPath);
        } else if (!Files.isWritable(localPath)) {
            throw new IOException("本地文件夹不可写: " + localDirPath);
        }

        SftpClient.CloseableHandle handle = sftpClient.openDir(remoteDirPath);

        sftpClient.listDir(handle).forEach(remoteFile -> {
            String remoteFilePath = remoteDirPath + "/" + remoteFile.getFilename();
            Path localFilePath = localPath.resolve(remoteFile.getFilename());

            try {
                if (remoteFile.getAttributes().isDirectory()) {
                    if (!".".equals(remoteFile.getFilename()) && !"..".equals(remoteFile.getFilename())) {
                        downloadDirectory(remoteFilePath, localDirPath + "/" + remoteFile.getFilename());
                    }
                } else {
                    try (SftpClient.CloseableHandle remoteHandle = sftpClient.open(remoteFilePath, SftpClient.OpenMode.Read);
                         FileChannel fileChannel = FileChannel.open(localFilePath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {

                        long fileSize = sftpClient.stat(remoteHandle).getSize();
                        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 *100);  // 100 MB 缓冲区
                        long position = 0;
                        while (position < fileSize) {
                            buffer.clear();
                            int bytesRead = sftpClient.read(remoteHandle, position, buffer.array(), 0, buffer.capacity());
                            if (bytesRead == -1) break;
                            buffer.limit(bytesRead);
                            fileChannel.write(buffer);
                            position += bytesRead;
                        }
                    }catch (IOException e) {
                        log.error("下载文件失败: " + remoteFilePath, e);
                        throw e;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 上传文件夹
     * @param localDirPath
     * @param remoteDirPath
     * @throws IOException
     */
    public void uploadDirectory(String localDirPath, String remoteDirPath) throws IOException {
        if (!checkRemoteDirPathIsDirectory(remoteDirPath)) {
            createDirectories(remoteDirPath);
        }

        Path localPath = Paths.get(localDirPath);
        try (DirectoryStream<Path> localFiles = Files.newDirectoryStream(localPath)) {
            for (Path localFilePath : localFiles) {
                String remoteFilePath = remoteDirPath + "/" + localFilePath.getFileName().toString();

                if (Files.isDirectory(localFilePath)) {
                    uploadDirectory(localDirPath + "/" + localFilePath.getFileName().toString(), remoteFilePath);
                } else {
                    try (SftpClient.CloseableHandle handle = sftpClient.open(remoteFilePath, SftpClient.OpenMode.Write, SftpClient.OpenMode.Create, SftpClient.OpenMode.Truncate);
                         FileChannel fileChannel = FileChannel.open(localFilePath, EnumSet.of(StandardOpenOption.READ))) {

                        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 100);  // 100 MB 缓冲区
                        long position = 0;
                        int bytesRead;
                        while ((bytesRead = fileChannel.read(buffer)) != -1) {
                            buffer.flip();
                            sftpClient.write(handle, position, buffer.array(), 0, bytesRead);
                            position += bytesRead;
                            buffer.clear();
                        }
                    }catch (IOException e) {
                        log.error("上传文件失败: " + localFilePath.getFileName().toString(), e);
                        throw e;
                    }
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查远程服务器目录是否是一个已存在的目录
     * @param remoteDirPath
     * @return
     */
    public boolean checkRemoteDirPathIsDirectory(String remoteDirPath){
        try {
            SftpClient.Attributes attrs = sftpClient.stat(remoteDirPath);
            if (attrs.isDirectory()) {
                return true;
            } else {
                log.error("The path exists but is not a directory need to create");
                return false;
            }
        } catch (IOException e) {
            log.error("The path not exists need to create");
            return false;
        }
    }

    public void createDirectories(String remoteDirPath) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(remoteDirPath, "/");
        String currentDir = "";

        while (tokenizer.hasMoreTokens()) {
            String dir = tokenizer.nextToken();
            if (currentDir.isEmpty()) {
                currentDir = "/" + dir;
            } else {
                currentDir = currentDir + "/" + dir;
            }

            try {
                // Check if the directory exists
                sftpClient.stat(currentDir);
            } catch (IOException e) {
                try {
                    sftpClient.mkdir(currentDir);  // Create directory if it does not exist
                } catch (IOException innerE) {
                    System.err.println("Failed to create directory: " + currentDir);
                    innerE.printStackTrace();
                    throw innerE;
                }
            }
        }
    }

}