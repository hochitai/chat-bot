import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Worker implements Runnable {
	
	private PublicKey publicKey;
	private PrivateKey privateKey;
	private SecretKeySpec skeySpec;
	private String secretKey;

    private BufferedReader in;
    private BufferedWriter out;
    private String name;

	private Socket socket;
	
	public Worker(Socket s, String n, PublicKey pubK, PrivateKey priK) {
		this.socket = s;
		this.name = n;
		this.publicKey = pubK;
		this.privateKey = priK;
		try {
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Client " + socket.toString() + " accepted");
		try {
			String key = Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
//			System.out.println("Public key: " + key);
			out.write(key);
			out.newLine();
			out.flush();
			String line = in.readLine();
//			System.out.println("Encryption Key đã mã hóa: " + line);
			getEncryptionKey(line);
			
			sendGreeting();
			
			String request,response;
			while (true) {
				request = decryptData(in.readLine());
				System.out.println("Server received: " + request + " from " + socket.toString() + " name: " + name);
				
				response = broadcast(request);
				System.out.println("Server response: " + response+ " to " + socket.toString() + " name: " + name);
				send(response);
				
			}		
			
		} catch (Exception e) {
			System.out.println("Client number " + name + " disconnected");
			Server.clients.remove(this);
		}
	}
	
	
	// Giải mã key đối xứng của client
	public void getEncryptionKey(String msg) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
			byte[] decryptOut = cipher.doFinal(Base64.getDecoder().decode(msg));
			this.skeySpec = new SecretKeySpec(decryptOut, "AES");	
			this.secretKey = new String(decryptOut);
//			System.out.println("Key mã hóa: " + this.secretKey );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public String encryptData(String msg) {
		String strEncrypt = "";
		try {
        	Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, this.skeySpec);
			byte[] byteEncrypted = cipher.doFinal(msg.getBytes());
			strEncrypt = Base64.getEncoder().encodeToString(byteEncrypted);
//			System.out.println("Dữ liệu sau khi mã hóa: " + strEncrypt );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return strEncrypt;
	}
	
	public String decryptData(String msg) {
		String strDecrypted = "";
		try {
        	Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, this.skeySpec);
			byte[] byteDecrypted = cipher.doFinal(Base64.getDecoder().decode(msg));
			strDecrypted = new String(byteDecrypted);
//			System.out.println("Dữ liệu sau khi giải mã: " + strDecrypted );
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return strDecrypted;
	}
	
	public String covertMessageToJSON(ArrayList<String> list ) {
		JSONObject json = new JSONObject();
		json.put("type", "chat");

		JSONArray array = new JSONArray();
		
		list.forEach(context -> array.put(context));
		
		json.put("data", array);

		String message = json.toString();
		
		return message;
	}
	
	public void send(String msg) {
		try {
			out.write(encryptData(msg));
			out.newLine();
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendGreeting() {
		ArrayList<String> list = new ArrayList<>();
		list.add("Chào bạn, mình có thể giúp gì cho bạn?");
		list.add("Bạn có thể nhắn /help để xem các sự trợ giúp của mình.");

		String message = covertMessageToJSON(list);
		
		send(message);
		
	}
	
	public String helpMenu() {
		ArrayList<String> list = new ArrayList<>();
		list.add("Mình hỗ trợ các lệnh trợ giúp:");
		list.add("TT:Tên thành phố/tỉnh => Tra cứu thời tiết");
		list.add("LC:IP => Xác định vị trí IP");
		list.add("SCAN:IP:x:y => Quét port, với IP là địa chỉ ip hoặc tên miền; x, y là port bắt đầu và kết thúc");
		list.add("OI:Tên miền => Tra cứu thông tin tên miền");
		list.add("ENC:(Text) => Encrypt Text bằng MD5");
		list.add("DEC:(Hash) => Decrypt Hash bằng MD5");
		list.add("CUR:x:y:(Số tiền cần chuyển đổi) => Chuyển đổi ngoại tệ từ x sang y, vd: CUR:USD:VND:10");
		list.add("Để xem lại các lệnh trợ giúp: /help");

		String message = covertMessageToJSON(list);
		
		return message;
	}
	
	public String broadcast(String line) {
		String response = "";
		String[] con = line.split(":");
		if (con.length >= 2) {
			String branch = con[0];
			switch (branch) {
				case "TT":
					String city = con[1];
					response = handleWeather(city);
					break;
				case "LC":
					if (con.length >= 2) {
						String ip = con[1].trim();
						response = handleIPLocation(ip);
					} else {
						response = covertMessageToJSON(
								new ArrayList<String>() {
									{
										add("Cú pháp xác định vị trí IP không hợp lệ!");
									}
								}
								);
					}
					
					break;
				case "SCAN":
					if (con.length >= 4) {
						try {
							String ipadd = con[1];
							int x = Integer.parseInt(con[2]);
							int y = Integer.parseInt(con[3]);
							response = handleScanPort(ipadd, x, y);	
						} catch (Exception e) {
							response = covertMessageToJSON(
									new ArrayList<String>() {
										{
											add("port là số nguyên!");
										}
									}
									);
						}
					} else {
						response = covertMessageToJSON(
								new ArrayList<String>() {
									{
										add("Cú pháp quét port không hợp lệ!");
									}
								}
								);
					}
					break;
				case "OI":
					if (con.length >= 2) {
						String domain = con[1];
						response = handleWhoIs(domain);
					} else {
						response = covertMessageToJSON(
								new ArrayList<String>() {
									{
										add("Cú pháp tra cứu thông tin tên miền không hợp lệ!");
									}
								}
								);
					}
					break;
				case "ENC":
					if (con.length >= 2) {
						String text = con[1];
						response = handleENC(text);
					} else {
						response = covertMessageToJSON(
								new ArrayList<String>() {
									{
										add("Cú pháp Encrypt không hợp lệ!");
									}
								}
								);
					}
					break;
				case "DEC":
					if (con.length >= 2) {
						String hash = con[1];
						response = handleDEC(hash);
					} else {
						response = covertMessageToJSON(
								new ArrayList<String>() {
									{
										add("Cú pháp Decrypt không hợp lệ!");
									}
								}
								);
					}
					break;
				case "CUR":
					if (con.length >= 4) {
						try {
							String currency = con[1];
							String covert = con[2];
							double money = Double.parseDouble(con[3]);
							response = handleCurrencyCoverter(currency, covert, money);	
						} catch (Exception e) {
							response = covertMessageToJSON(
										new ArrayList<String>() {
											{
												add("Số tiền là số");
											}
										}
									);
						}
					} else {
						response = covertMessageToJSON(
								new ArrayList<String>() {
									{
										add("Cú pháp chuyển đổi ngoại tệ không hợp lệ!");
									}
								}
								);
					}
					break;
				default: 
					response = handleChat(line);
					break;
			}
		} else if (line.equalsIgnoreCase("/help")){
			response = helpMenu();
		} else {
			response = handleChat(line);
		}
		return response;
	}

	
	public String handleChat(String line) {
		String url = "https://simsimi.info/api/?text="+ line +"&lc=vn";
//		String url = "https://tuanxuong.com/api/simsimi/index.php?text=" + line;
		
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.execute()
					.parse();
			
			JSONObject json = new JSONObject(doc.text());
			
//			message = json.get("message").toString();
//			message = json.get("response").toString();
			
			message = covertMessageToJSON(
						new ArrayList<String>() {
							{
								add(json.get("message").toString());
							}
						}
					);
					
		} catch (Exception e) {
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không thể kết nối!");
						}
					}
				);
		}
		return message;
	}
	
	public String handleWeather(String city) {
		String key = "5UNML9P4YWTK7RFPX37FGU754";
		String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/"
				+ "rest/services/timeline/"+ city
				+ "?unitGroup=metric&key="+ key 
				+ "&contentType=json";
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.execute()
					.parse();
			
			JSONObject json = new JSONObject(doc.text());
			JSONArray days = new JSONArray(json.get("days").toString()); 

			json = new JSONObject();
			json.put("type", "weather");
			json.put("address", city);
			
			JSONArray data = new JSONArray();
			int DAY = 5;
			
			for (int i=0;i<DAY;i++) {
				JSONObject day = new JSONObject(days.get(i).toString());
				JSONObject subDay = new JSONObject();
				subDay.put("datetime", day.get("datetime"));
				subDay.put("temp", day.get("temp"));
				subDay.put("feelslike", day.get("feelslike"));
				subDay.put("humidity", day.get("humidity"));
				subDay.put("conditions", day.get("conditions"));
				data.put(subDay);
			}
			json.put("data", data);
			
			message = json.toString();
					
		} catch (Exception e) {
			// TODO Auto-generated catch block
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không có thông tin của khu vực!");
						}
					}
				);
		}
		return message;
	}
	
	public String handleIPLocation(String ip) {
		String url = "https://www.whatismyip.com/" + ip;
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreHttpErrors(true)
					.ignoreContentType(true)
					.userAgent("Mozilla")
					.execute()
					.parse();
			Element ele = doc.getElementsByAttributeValue("class", "list-group list-group-flush").first();
			
			if (ele != null) {
				String content = ele.getElementsByTag("li").get(0).text();
				String[] name = content.split(": ");
				
				JSONObject json = new JSONObject();
				json.put("type", "location");
				
				JSONObject data = new JSONObject();
				data.put("ip", ip);
				
				data.put("city", name[1]);
				
				content = ele.getElementsByTag("li").get(1).text();
				name = content.split(" ");
				data.put("region", name[1]);
				
				content = ele.getElementsByTag("li").get(2).text();
				name = content.split(" ");
				data.put("country", name[1]);
				
				json.put("data", data);
				
				message = json.toString();
						
			} else {
				message = covertMessageToJSON(
						new ArrayList<String>() {
							{
								add("IP không hợp lệ hoặc Đây là Private IP");
							}
						}
					);
			}				
		} catch (Exception e) {
			
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không thể tìm thấy thông tin!");
						}
					}
				);
		}
		return message;
	}
	
	public static Future<Boolean> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
		  return es.submit(new Callable<Boolean>() {
		      @Override public Boolean call() {
		        try {
		          Socket socket = new Socket();
		          socket.connect(new InetSocketAddress(ip, port), timeout);
		          socket.close();
		          return true;
		        } catch (Exception ex) {
		          return false;
		        }
		      }
		   });
		}
	
	public String handleScanPort(String ip, int x, int y) {
		String message = "";
		try {
			InetAddress add = InetAddress.getByName(ip);
			if (add.isReachable(5000)) {
				ExecutorService es = Executors.newFixedThreadPool(20);
				int timeout = 200;
				List<Future<Boolean>> futures = new ArrayList<>();
				for (int port = x; port <= y; port++) {
					futures.add(portIsOpen(es, ip, port, timeout));
				}
				es.shutdown();
				JSONObject json = new JSONObject();
				json.put("type", "scan");
				JSONArray array = new JSONArray();
				int openPort = x;
				for (Future<Boolean> f : futures) {
					try {
						if (f.get()) {
							array.put(openPort);		
						}
						openPort++;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (array.isEmpty()) {
					message = covertMessageToJSON(
							new ArrayList<String>() {
								{
									add("Không có port được mở trong khoảng từ " 
											+ x + " đến " + y + " !");
								}
							}
						);
				} else {
					json.put("data", array);
					message = json.toString();					
				}
				
			} else {
				message = covertMessageToJSON(
						new ArrayList<String>() {
							{
								add("Địa chỉ IP không thể kết nối!");
							}
						}
					);
			}
		} catch (Exception e) {
			
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không tìm thấy địa chỉ " + ip);
						}
					}
				);
		}
		return message;
			
	}
	
	
	public String handleWhoIs(String domain) {
		String url = "https://whois.inet.vn/api/whois/domainspecify/" + domain;
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.execute()
					.parse();
			
			JSONObject json = new JSONObject(doc.text());
			
			if (json.get("code").toString().equals("0")) {
				String domainName = json.get("domainName").toString();
				String registrar = json.get("registrar").toString();
				String creationDate = json.get("creationDate").toString();
				String expirationDate = json.get("expirationDate").toString();
				String owner = json.get("registrantName").toString();
				JSONArray nameServers = json.getJSONArray("nameServer");
				
				json = new JSONObject();
				json.put("type", "whois");
				JSONObject data = new JSONObject();
				data.put("domainName", domainName);
				data.put("registrar", registrar);
				data.put("creationDate", creationDate);
				data.put("expirationDate", expirationDate);
				data.put("registrantName", owner);
				data.put("nameServers", nameServers);
				
				json.put("data", data);
				
				message = json.toString();
			} else {
				message = covertMessageToJSON(
						new ArrayList<String>() {
							{
								add("Tên miền chưa được đăng ký!");
							}
						}
					);
			}
			
//					
		} catch (Exception e) {
			
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không thể kết nối!");
						}
					}
				);
		}
		
		return message;
	}
	
	public String handleENC(String text) {
		String url = "https://hashtoolkit.com/generate-hash/?text=" + text;
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.execute()
					.parse();
			String hash = doc.getElementsByAttributeValue("class", "res-hash").first().text();
			
			JSONObject json = new JSONObject();
			json.put("type", "enc");
			json.put("data", hash);

			message = json.toString();
			
		} catch (Exception e) {
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không thể kết nối!");
						}
					}
				);
		}
		return message;
	}
	
	public String handleDEC(String hash) {
		String url = "https://hashtoolkit.com/decrypt-hash/?hash=" + hash;
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.execute()
					.parse();
			Element ele = doc.getElementsByAttributeValue("class", "alert alert-warning").first();
			if (ele == null) {
				String text = doc.getElementsByAttributeValue("class", "res-text").first().text();
				JSONObject json = new JSONObject();
				json.put("type", "dec");
				json.put("data", text);

				message = json.toString();
				
			} else {
				message = covertMessageToJSON(
						new ArrayList<String>() {
							{
								add("Không tìm thấy mã hash này!");
							}
						}
					);
			}
			
		} catch (Exception e) {
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không thể kết nối!");
						}
					}
				);
		} 
		return message;
	}
	
	public String handleCurrencyCoverter(String currency, String convert, double money) {
		String key ="3ccb8b5d36dcffaf565db0c5";
		String url = "https://v6.exchangerate-api.com/v6/" + key + "/latest/" + currency.toUpperCase();
		String message = "";
		try {
			Document doc = Jsoup.connect(url)
					.method(Method.GET)
					.ignoreContentType(true)
					.execute()
					.parse();
			JSONObject json = new JSONObject(doc.text());
			JSONObject rates = (JSONObject) json.get("conversion_rates");
			double x = rates.getDouble(convert.toUpperCase());
			
			json = new JSONObject();
			json.put("type", "cur");
			JSONObject data = new JSONObject();
			data.put("base_code", currency);
			data.put("convert_code", convert);
			data.put("conversion_rate", x);
			data.put("conversion_money", x*money);
			
			json.put("data", data);

			message = json.toString();
		
		} catch (Exception e) {
			message = covertMessageToJSON(
					new ArrayList<String>() {
						{
							add("Không thể chuyển đổi!");
						}
					}
				);
		}
		return message;
	}

}
