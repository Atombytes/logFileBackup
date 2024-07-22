package cn.capinfo.ccaf.monitor.busi.manager.logic.util;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public class RemoteDirectoryCopy {

    public static void main(String[] args) {
        String sourceHost = "192.168.29.130";
        int sourcePort = 22;
        String sourceUser = "root";
        String sourcePassword = "Sp12";
        String sourceDirectory = "/root/个人网上_log";

        String destinationHost = "192.168.29.131";
        int destinationPort = 22;
        String destinationUser = "root";
        String destinationPassword = "Sp12";
        String destinationDirectory = "/root";

        try {
            long startTime = System.currentTimeMillis();
            copyDirectoryBetweenServers(sourceHost, sourcePort, sourceUser, sourcePassword, sourceDirectory,
                    destinationHost, destinationPort, destinationUser, destinationPassword, destinationDirectory);
            long endTime = System.currentTimeMillis();
            System.out.println("Total transfer time: " + (endTime - startTime) / 1000 + " seconds");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyDirectoryBetweenServers(String sourceHost, int sourcePort, String sourceUser, String sourcePassword, String sourceDirectory,
                                                   String destinationHost, int destinationPort, String destinationUser, String destinationPassword, String destinationDirectory) throws IOException {
        SshClient client = SshClient.setUpDefaultClient();
        //客户端已知的连接远程服务器主机指纹（在客户端首次连接时，指纹会被保存到 ~/.ssh/known_hosts 文件中）
//        Path knownHostsPath = Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts");
//        client.setServerKeyVerifier(new KnownHostsServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE,knownHostsPath));
//        //公钥验证用于确认客户端的身份
//        Path privateKeyPath = Paths.get(System.getProperty("user.home"), ".ssh", "id_rsa");
//        KeyPairProvider keyPairProvider = new FileKeyPairProvider(privateKeyPath);
//        client.setKeyIdentityProvider(keyPairProvider);

        //服务器指纹验证用于确认客户端连接的服务器的真实性
//        ServerKeyVerifier customVerifier = (session, remoteAddress,serverKey) -> {
//            // Implement your custom key verification logic here
//            // For example, check if the server key matches an expected fingerprint or is signed by a trusted CA
//            // Return true if the key is accepted, false otherwise
//            return true; // Change this to your actual verification logic
//        };
//        client.setServerKeyVerifier(customVerifier);

        client.start();

        try (ClientSession sourceSession = client.connect(sourceUser, sourceHost, sourcePort).verify(10000, TimeUnit.MILLISECONDS).getSession()) {
            sourceSession.addPasswordIdentity(sourcePassword);
            sourceSession.auth().verify(10000, TimeUnit.MILLISECONDS);

            try (ClientSession destinationSession = client.connect(destinationUser, destinationHost, destinationPort).verify(10000, TimeUnit.MILLISECONDS).getSession()) {
                destinationSession.addPasswordIdentity(destinationPassword);
                destinationSession.auth().verify(10000, TimeUnit.MILLISECONDS);
            }

            // Construct the scp command to copy the directory from source to destination
            String scpCommand = String.format("scp -r %s %s@%s:%s", sourceDirectory, destinationUser, destinationHost, destinationDirectory);

            executeRemoteCommand(sourceSession, scpCommand);
        } finally {
            client.stop();
        }
    }

    private static void executeRemoteCommand(ClientSession session, String command) throws IOException {
        try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
             ClientChannel channel = session.createExecChannel(command)) {

            channel.setOut(responseStream);
            channel.setErr(responseStream);
            channel.open().verify(10000, TimeUnit.MILLISECONDS);
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), TimeUnit.MINUTES.toMillis(30));

            String response = new String(responseStream.toByteArray(), StandardCharsets.UTF_8).trim();
            System.out.println("Command response: " + response);
        }
    }
}


