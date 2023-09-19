 
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.text.AttributedCharacterIterator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;

public class Client extends JFrame{
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private PublicKey publicKey;
    private SecretKeySpec skeySpec;
    private String secretKey;
    public static ExecutorService executor;
    
    private JTextPane boxMesseage;
    private JTextField text;
    private JButton btnSend;
    private ArrayList<String> listMessage = new ArrayList<String>();
    private int index = 0;
    
    
    public static void main(String[] args) { 
    	Client client = new Client(getIpServer(), 8321);
    	client.start();
    }
    
    public Client(String host, int port) {
    	try {
			this.socket = new Socket(host, 8321);
	    	in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
	        out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
	        initial();
		} catch (Exception e) {
			
		}
    	
    }
    
    public static String getIpServer() {
    	String api = "https://retoolapi.dev/Fhiu4o/data/1";
    	String ipAPI = "";
    	Document doc;
		try {
			doc = Jsoup.connect(api)
					.ignoreContentType(true)
					.ignoreHttpErrors(true)
					.method(Method.GET)
					.execute()
					.parse();
			JSONObject json = new JSONObject(doc.text());
		
			ipAPI = json.get("ip").toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ipAPI;
    }
    
    public void start() {
    	try {
    		String line = in.readLine();
//    		System.out.println("Public key: " +  line);
    		getPublicKey(line);
    		createEncryptionKey();
    		encryptDataByPublicKeyAndSend();
    		
    		executor = Executors.newCachedThreadPool();
    	    Send s = new Send(socket, out);
    	    Receive r = new Receive(socket, in);
    	    executor.execute(s);
    	    executor.execute(r);
        } catch (Exception e) { 
        	JOptionPane.showMessageDialog(this,"Không thể kết nối tới server!" ,"Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
   class Send implements Runnable {

        private Socket socket;
        private BufferedWriter out;

        public Send(Socket s, BufferedWriter o) {
            this.socket = s;
            this.out = o;
        }

        @Override
        public void run() {	
        	text.addActionListener(new ActionListener() {	
        		@Override
        		public void actionPerformed(ActionEvent e) {
        			// TODO Auto-generated method stub
        			String request =  text.getText();
        			listMessage.add(request);
        			index = listMessage.size();
        			sendData(request);
        		}
        	});
        	
        	text.addKeyListener(new KeyListener() {
				
				@Override
				public void keyTyped(KeyEvent e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void keyReleased(KeyEvent e) {
					// TODO Auto-generated method stub
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						if (listMessage.size() != 0) {
							if (index != 0) {
								text.setText(listMessage.get(--index));					
							} 
						}
					}
					if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						if (listMessage.size() != 0) {
							if (index == listMessage.size()-1 ) {
								++index;
								text.setText("");	
							} else if (index != listMessage.size()) {
								text.setText(listMessage.get(++index));
							}
						}
					}
				}
				
				@Override
				public void keyPressed(KeyEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
        	
        	btnSend.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					String content =  text.getText();
					listMessage.add(content);
        			index = listMessage.size();
					sendData(content);
				}
			});
        }
        
        public void sendData(String request) {
        	if (!request.isEmpty()) {			
				appendToPane(boxMesseage, styleChatSend(request));
				text.setText("");	
				try {
					out.write(encryptData(request));
					out.newLine();
					out.flush();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}				
			}	
        }
    }

    class Receive implements Runnable {

        private Socket socket;
        private BufferedReader in;

        public Receive(Socket s, BufferedReader i) {
            this.socket = s;
            this.in = i;
        }

        @Override
        public void run() {
            try {
            	while (true) {
					String response = decryptData(in.readLine());
					handleUIData(response);
//					appendToPane(boxMesseage, styleChatReceive(decryptData(line)));
            	}
            } catch (IOException e) {
            	System.out.println(e);
            }
        }

    }
    
    public void initial() {
    	new JFrame();
    	setSize(640, 700);
    	setLayout(new BorderLayout());
    	getContentPane().setBackground(Color.WHITE);
    	setTitle("AI Chatbot");
    	
    	Font font =  new Font("Serif", Font.PLAIN, 16);
    	
    	Icon icon = new ImageIcon("./send.png");
    	ImageIcon img = new ImageIcon("bot.png");
    	
    	setIconImage(img.getImage());
    	
    	
    	boxMesseage = new JTextPane();
    	boxMesseage.setContentType("text/html");
    	boxMesseage.setEditable(false);
    	boxMesseage.setBorder(new EmptyBorder(20, 16, 0, 16));

    	JScrollPane scrollPane = new JScrollPane(boxMesseage);
    	scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    	scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    	scrollPane.setComponentZOrder(scrollPane.getVerticalScrollBar(), 0);
        scrollPane.setComponentZOrder(scrollPane.getViewport(), 1);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        scrollPane.setLayout(new ScrollPaneLayout() {
          @Override
          public void layoutContainer(Container parent) {
            JScrollPane scrollPane = (JScrollPane)parent;

            Rectangle availR = scrollPane.getBounds();
            availR.x = availR.y = 0;

            Insets insets = parent.getInsets();
            availR.x = insets.left;
            availR.y = insets.top;
            availR.width  -= insets.left + insets.right;
            availR.height -= insets.top  + insets.bottom;

            Rectangle vsbR = new Rectangle();
            vsbR.width  = 12;
            vsbR.height = availR.height;
            vsbR.x = availR.x + availR.width - vsbR.width;
            vsbR.y = availR.y;

            if(viewport != null) {
              viewport.setBounds(availR);
            }
            if(vsb != null) {
              vsb.setVisible(true);
              vsb.setBounds(vsbR);
            }
          }
        });
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
          private final Dimension d = new Dimension();
          @Override protected JButton createDecreaseButton(int orientation) {
            return new JButton() {
              @Override public Dimension getPreferredSize() {
                return d;
              }
            };
          }
          @Override protected JButton createIncreaseButton(int orientation) {
            return new JButton() {
              @Override public Dimension getPreferredSize() {
                return d;
              }
            };
          }
          @Override
          protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
          @Override
          protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = null;
            JScrollBar sb = (JScrollBar)c;
            if(!sb.isEnabled() || r.width>r.height) {
              return;
            }else if(isDragging) {
              color = new Color(236,242,247);
            }else if(isThumbRollover()) {
              color = new Color(1,132,255);
            }else {
              color = new Color(236,242,247);
            }
            g2.setPaint(color);
            g2.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
            g2.setPaint(Color.WHITE);
            g2.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
            g2.dispose();
          }
          @Override
          protected void setThumbBounds(int x, int y, int width, int height) {
            super.setThumbBounds(x, y, width, height);
            scrollbar.repaint();
          }
        });
        
        JPanel panel1 = new JPanel();
    	panel1.setPreferredSize(new Dimension(500,90));
    	panel1.setLayout(new FlowLayout(FlowLayout.CENTER,20,0));
    	panel1.setBorder(new EmptyBorder(30,20,20,20));
    	panel1.setBackground(new Color(0xfff7f9fa));
    	
    	text = new RoundJTextField(40);
    	text.setPreferredSize(new Dimension(100,36));
    	text.setMargin(new Insets(0,15,0,0));
    	text.setBackground(new Color(0xfff7f9fa));
    	text.setFont(font);
    	text.setText("Aa");
    	text.setForeground(Color.GRAY);
    		
    	text.addFocusListener(new FocusListener() {
    	    @Override
    	    public void focusGained(FocusEvent e) {
    	        if (text.getText().equals("Aa")) {
    	            text.setText("");
    	            text.setForeground(Color.BLACK);
    	        }
    	    }
    	    @Override
    	    public void focusLost(FocusEvent e) {
    	        if (text.getText().isEmpty()) {
    	            text.setForeground(Color.GRAY);
    	            text.setText("Aa");
    	        }
    	    }
    	});
    	  	
    	btnSend = new JButton(icon); 
    	btnSend.setPreferredSize(new Dimension(36,36));
    	btnSend.setOpaque(false);
    	btnSend.setContentAreaFilled(false);
    	btnSend.setBorderPainted(false); 
    	btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
    	
    	appendToPane(boxMesseage, "<div style='text-align:center; margin-bottom:30px;"
    			+ "font-family: Arial, Helvetica, sans-serif;"
    			+ "background-color: white;'>"
    				+ "<img height='100' width='150' src='file:./bot.png'/>" 
    				+	"<div style='font-size:14px'>AI ChatBot</div>"
    			+ "</div>");
    
    		
    	panel1.add(text);
    	panel1.add(btnSend);
    	
    	add(scrollPane, BorderLayout.CENTER);
    	add(panel1, BorderLayout.SOUTH);
    	
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	setResizable(false);
    	setLocationRelativeTo(null);
    	setVisible(true);
    	
    }
    
    
    public void getPublicKey(String msg) {
    	try {
    		byte[] decodedString = Base64.getDecoder().decode(msg.getBytes("UTF-8"));
    		X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedString);
    		KeyFactory factory = KeyFactory.getInstance("RSA");
    		this.publicKey = factory.generatePublic(spec);			
    	} catch (Exception e) { System.err.println(e); }
    }
    
    
    // Tạo key mã hóa đối xứng
    public void createEncryptionKey() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 16;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int) 
              (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String generatedString = buffer.toString();
        this.secretKey = generatedString;
    	this.skeySpec = new SecretKeySpec(generatedString.getBytes(), "AES");
//    	System.out.println(this.secretKey);
    }
    
    
    // Mã hóa key đối xứng bằng key công khai và gửi lại server
    public void encryptDataByPublicKeyAndSend() {
		try {
//			System.out.println("Encrypt Key: " + strKey);
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, this.publicKey);
			byte[] encryptOut = c.doFinal(this.secretKey.getBytes());
			String strEncrypt = Base64.getEncoder().encodeToString(encryptOut);
//			System.out.println("Encrypt Key đã mã hoá: " + strEncrypt);
			out.write(strEncrypt);
			out.newLine();
			out.flush();
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
	
	public void handleUIData(String line) {
		JSONObject json = new JSONObject(line);
		String type = json.get("type").toString();
		JSONObject data;
		JSONArray arrData;
		String context = "";
		String result = "";
		int len;
		switch (type) {
			case "chat":
				arrData = json.getJSONArray("data");
				len = arrData.length();
				for (int i=0;i<len;i++) {
					appendToPane(boxMesseage, styleChatReceive(arrData.get(i).toString()));
				}
				break;
			case "weather":
				String address = json.get("address").toString();
				arrData = json.getJSONArray("data");
				len = arrData.length();
				for (int i=0;i<len;i++) {
					data = new JSONObject(arrData.get(i).toString());
					context = "<div style='margin-bottom:10px;'><b>Vị trí: </b>" + address + "</div>";
					context += "<div style='margin-bottom:10px;'><b>Ngày: </b>"+ data.get("datetime") +"</div>"; 
					context += "<div style='margin-bottom:10px;'><b>Nhiệt độ: </b>"+ data.get("temp") + "'C"+"</div>"; 
					context += "<div style='margin-bottom:10px;'><b>Cảm giác: </b>"+ data.get("feelslike") + "'C"+"</div>"; 
					context += "<div style='margin-bottom:10px;'><b>Độ ẩm: </b>"+ data.get("humidity") +" %</div>"; 
					context += "<div style='margin-bottom:10px;'><b>Dự báo: </b>"+ data.get("conditions") +"</div>"; 
					appendToPane(boxMesseage, styleChatReceive(context));
				}
				break;
			case "location":
				data = json.getJSONObject("data");
				context += "<div style='margin-bottom:10px;'><b>IP: </b>" + data.get("ip") + "</div>";
				context += "<div style='margin-bottom:10px;'><b>Thành phố: </b>" + data.get("city") + "</div>";
				context += "<div style='margin-bottom:10px;'><b>Khu vực: </b>" + data.get("region") + "</div>";
				context += "<div style='margin-bottom:10px;'><b>Quốc gia: </b>" + data.get("country") + "</div>";
				appendToPane(boxMesseage, styleChatReceive(context));
				break;
			case "scan":
				arrData = json.getJSONArray("data");
				context += "Các port đang mở  là: ";
				len = arrData.length();
				for (int i=0;i<len;i++) {
					context += arrData.get(i).toString() + ", ";
				}
				context = context.substring(0, context.length()-2);
				appendToPane(boxMesseage, styleChatReceive(context));
				break;
			case "whois":
				data = json.getJSONObject("data");
				context += "<div style='margin-bottom:10px;'><b>Domain Name: </b>" + data.get("domainName") + "</div>";
				context += "<div style='margin-bottom:10px;'><b>Công ty đăng ký: </b>" + data.get("registrar") + "</div>";
				context += "<div style='margin-bottom:10px;'><b>Ngày đăng ký: </b>" + data.get("creationDate") + "</div>";
				context += "<div style='margin-bottom:10px;'><b>Ngày hết hạn: </b>" + data.get("expirationDate") + "</div>";
				context += "<div style='margin-bottom:10px;' ><b>Người sở hữu: </b>" + data.get("registrantName") + "</div>";
				JSONArray nameServers = new JSONArray(data.get("nameServers").toString());
				context += "<div style='margin-bottom:10px;'><b>NameServers: </b>";
				for (int i=0;i<nameServers.length();i++) {
					context += nameServers.get(i).toString() + ", ";
				}
				context = context.substring(0, context.length()-2);
				context += "</div>";
				appendToPane(boxMesseage, styleChatReceive(context));
				break;
			case "enc":
				result = json.get("data").toString();
				context += "<div style='margin-bottom:10px;'><b>Hash: </b><div>" + result + "</div></div>";
				appendToPane(boxMesseage, styleChatReceive(context));
				break;
			case "dec":
				result = json.get("data").toString();
				context += "<div style='margin-bottom:10px;'><b>Text: </b><div>" + result + "</div></div>";
				appendToPane(boxMesseage, styleChatReceive(context));
				break;
			case "cur":
				Locale localeEN = new Locale("en", "EN");
			    NumberFormat en = NumberFormat.getInstance(localeEN);
				data = json.getJSONObject("data");
				context += "<div style='margin-bottom:10px;'><b>Tỉ lệ chuyển đổi: </b><div>"
							+ en.format(data.get("conversion_rate")) + " " 
							+ data.get("convert_code").toString().toUpperCase() +"</div></div>";
				context += "<div style='margin-bottom:10px;'><b>Số tiền chuyển đổi: </b><div>"
							+ en.format(data.get("conversion_money")) + " " 
							+ data.get("convert_code").toString().toUpperCase() +"</div></div>";
				appendToPane(boxMesseage, styleChatReceive(context));
				break;
		}
	}
    
    
    public String styleChatReceive(String msg) {
    	int length = msg.length() > 26 ? 26 : msg.length(); 
    	int width = 7*length + 40; 
    	return "<div "
    				+ "style='width:" + width + "px;background-color: #0184ff;padding: 10px;margin-bottom:20px;"
    					+ "color:white;font-family: Arial, Helvetica, sans-serif;'>"
    						+ msg 
    			+ "</div>";
    }
    
    
    public String styleChatSend(String msg) {
    	int length = msg.length() > 26 ? 26 : msg.length(); 
    	int width = 480 - 7*length - 40; 
    	return "<div style='width:100%;margin-left:"+width+"px;background-color: #d2eaff;padding: 10px;"
    			+ "font-family: Arial, Helvetica, sans-serif;"
    			+ "margin-bottom:20px;'>"
    				+ msg 
			+ "</div>";
    }
      
    public void appendToPane(JTextPane tp, String msg) {
    	HTMLDocument doc = (HTMLDocument) tp.getDocument();
		HTMLEditorKit editorKit = (HTMLEditorKit) tp.getEditorKit();
		try {

			editorKit.insertHTML(doc, doc.getLength(), msg, 0, 0, null);
			tp.setCaretPosition(doc.getLength());

		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public class RoundJTextField extends JTextField {
        private Shape shape;
        public RoundJTextField(int size) {
            super(size);
            setOpaque(false); // As suggested by @AVD in comment.
        }
        protected void paintComponent(Graphics g) {
             g.setColor(getBackground());
             g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
             super.paintComponent(g);
        }
        protected void paintBorder(Graphics g) {
             g.setColor(getForeground());
             g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
        }
        public boolean contains(int x, int y) {
             if (shape == null || !shape.getBounds().equals(getBounds())) {
                 shape = new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 20, 20);
             }
             return shape.contains(x, y);
        }
    }
}
