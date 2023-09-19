import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Connection.Method;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;;

public class Server {
	private static ServerSocket server = null;
	private static int port = 8321;
	private static int numThread = 2;
	private static final String FILE_PUBLICKEY = "publicKey.txt";
	private static final String FILE_PRIVATEKEY = "privateKey.txt";
	private static PublicKey publicKey;
	private static PrivateKey privateKey;
	public static Vector<Worker> clients = new Vector<>();
	
	public static void main(String[] args) throws IOException {
		ExecutorService executor = Executors.newFixedThreadPool(numThread);
		try {
			server = new ServerSocket(port); 
//			KeyPair kp = createKey();
			publicKey = readPublicKey();
			privateKey = readPrivateKey();
			shareIP();
			
			System.out.println("Server binding at port " + port);
			System.out.println("Waiting for client...");
			int i=1;
			while (true) {
				Socket socket = server.accept();
                Worker client = new Worker(socket, Integer.toString(i++), publicKey,
                		privateKey);
                clients.add(client);
                executor.execute(client);
			}
		} catch (IOException e) {
			System.out.println(e);
		} finally {
			if (server!=null) 
				server.close();
		}
		
	}
	
	private static void shareIP() {
		try {
			
			String ip = InetAddress.getLocalHost().getHostAddress().toString();
			
			String api = "https://retoolapi.dev/Fhiu4o/data/1";
			String requestBody = "{\"ip\":\"" + ip +"\"}";
			
			Jsoup.connect(api)
				.ignoreContentType(true)
				.ignoreHttpErrors(true)
				.header("Content-type", "application/json")
				.method(Method.PUT)
				.requestBody(requestBody)
				.execute();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private static KeyPair createKey() {
		KeyPair kp = null;
		try {
			SecureRandom sr = new SecureRandom();
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048, sr);

			kp = kpg.genKeyPair();
			
			PublicKey publicKey = kp.getPublic();
			// PrivateKey
			PrivateKey privateKey = kp.getPrivate();
			
			File publicKeyFile = new File(FILE_PUBLICKEY);
			File privateKeyFile = new File(FILE_PRIVATEKEY);
			
			// Lưu Public Key
			FileOutputStream fos = new FileOutputStream(publicKeyFile);
			fos.write(publicKey.getEncoded());
			fos.close();

			// Lưu Private Key
			fos = new FileOutputStream(privateKeyFile);
			fos.write(privateKey.getEncoded());
			fos.close();
			
//			System.out.println("Generate key successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return kp;
	}
	
	public static PublicKey readPublicKey() {
		PublicKey pubKey = null;
		try {
			// Đọc file chứa public key
			FileInputStream fis = new FileInputStream(FILE_PUBLICKEY);
			byte[] b = new byte[fis.available()];
			fis.read(b);
			fis.close();
			
			// Tạo public key
			X509EncodedKeySpec spec = new X509EncodedKeySpec(b);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			pubKey = factory.generatePublic(spec);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pubKey;	
	}
	
	public static PrivateKey readPrivateKey() {
		PrivateKey priKey = null;
		try {
			// Đọc file chứa private key
			FileInputStream fis = new FileInputStream(FILE_PRIVATEKEY);
			byte[] b = new byte[fis.available()];
			fis.read(b);
			fis.close();
			
			// Tạo private key
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			priKey = factory.generatePrivate(spec);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return priKey;	
	}
	
}
