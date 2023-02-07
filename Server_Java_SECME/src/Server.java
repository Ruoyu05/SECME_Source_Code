import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class Server {
    public static void main(String[] args) {
        try {
            MySqlConnect mysqlConnect = new MySqlConnect();

            //启动时生成一对临时服务器密钥
            KeyPair keyPair = RSAHelper.createKeyPair(2048);
            String privateKeyStr = RSAHelper.getPrivateKeyFromKeyPair(keyPair);
            String publicKeyStr = RSAHelper.getPublicKeyFromKeyPair(keyPair);

            mysqlConnect.updateServerInfo(privateKeyStr,publicKeyStr);
            mysqlConnect.close();

            //启动服务器
            WebScoketServerBuilder server = new WebScoketServerBuilder(10086);
            System.out.println("Server start on port: " + server.getPort());
            System.out.println(" ");
            server.start();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }
}
