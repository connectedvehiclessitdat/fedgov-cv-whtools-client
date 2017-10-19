/**
 * Copyright 2014 Leidos
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.usdot.cv.whtools.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;

import gov.usdot.cv.whtools.client.CASClient.CASLoginException;
import gov.usdot.cv.whtools.client.config.ConfigException;
import gov.usdot.cv.whtools.client.config.ConfigUtils;
import gov.usdot.cv.whtools.client.config.WarehouseConfig;
import gov.usdot.cv.whtools.client.handler.DepositHandler;
import gov.usdot.cv.whtools.client.handler.RequestHandler;
import gov.usdot.cv.whtools.client.handler.ResponseHandler;

public class WarehouseClient {

	private static final Logger logger = Logger.getLogger(WarehouseClient.class
			.getName());
	
	private URI serverUri;
	private org.eclipse.jetty.websocket.client.WebSocketClient client;
	private WarehouseWebSocket socket;
	private ResponseHandler handler;

	public static WarehouseClient configure(WarehouseConfig wsConfig, ResponseHandler handler)
			throws URISyntaxException, KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException, Exception {
		
		WarehouseClient wsClient = new WarehouseClient(wsConfig);
		wsClient.handler = handler;
				
		return wsClient;
	}
	
	private WarehouseClient(WarehouseConfig wsConfig) throws 
												URISyntaxException, KeyManagementException, KeyStoreException,
												NoSuchAlgorithmException, CertificateException, IOException, Exception {

		this.socket = new WarehouseWebSocket();
		
		this.serverUri = new URI(wsConfig.warehouseURL);
		
		if (wsConfig.warehouseURL.startsWith("wss")) {
			SSLContext wsSSLContext;
			if(wsConfig.keystoreFile != null && wsConfig.keystorePassword != null) {
				wsSSLContext = SSLBuilder.buildSSLContext(wsConfig.keystoreFile, wsConfig.keystorePassword);
			}
			else {
				wsSSLContext = SSLBuilder.buildSSLContext();
			}
			
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setSslContext(wsSSLContext);
			
			this.client = new org.eclipse.jetty.websocket.client.WebSocketClient(sslContextFactory);
		}
		else {
			this.client = new org.eclipse.jetty.websocket.client.WebSocketClient();
		}
		
		HttpCookieStore cookieStore = new HttpCookieStore();
		cookieStore.add(serverUri, new HttpCookie(WarehouseConfig.JSESSIONID_KEY, wsConfig.jSessionID));
		client.setCookieStore(cookieStore);
		
		this.client.start();
	}
	
	public void connect() throws IOException {
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		client.connect(socket, serverUri, request);
	}
	
	public void send(String message) throws IOException {
		socket.send(message);
	}
	
	public void close() throws Exception {
		socket.close();
		client.stop();
	}
	
	@WebSocket(maxIdleTime=0)
	public class WarehouseWebSocket {
		private Session session;
		
		@OnWebSocketConnect
		public void onOpen(Session session) {
			this.session = session;
			this.session.setIdleTimeout(0);		// Don't timeout
			
			logger.info("Connection opened to " + serverUri.toString());
		}

		@OnWebSocketClose
		public void onClose(int code, String reason) {
			logger.info("Connection to " + serverUri.toString() + " closed.");
		}

		@OnWebSocketMessage
		public void onMessage(String message) {
			logger.debug("Received message: " + message);
			handler.handleMessage(message);
		}

		@OnWebSocketError
		public void onError(Throwable t) {
			t.printStackTrace();
			logger.error("Error:", t);
		}
		
		public void send(String message) throws IOException {
			try {
				// If messages are attempted to be sent by multiple threads(for example, multiple clients)
				// to the same RemoteEndpoint, it can lead to blocking and throws the error:
				//     java.lang.IllegalStateException: Blocking message pending 10000 for BLOCKING
				// To alleviate this, use asynchronous, non-blocking methods that require us to check
				// if the send was successful.
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=474488
				Future<Void> sendFuture = session.getRemote().sendStringByFuture(message);
				sendFuture.get(3, TimeUnit.SECONDS);	// Wait for completion
			} catch (Exception e) {
				throw new IOException("Message failed to send.", e);
			}
		}
		
		public void close() {
			if(session != null) session.close();
			session = null;
		}
	}
	
	public static void main(String[] args) throws URISyntaxException,
			UnrecoverableKeyException, KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException,
			FileNotFoundException, IOException, InterruptedException,
			CASLoginException, IllegalAccessException, InvocationTargetException, ConfigException, Exception {

		ConfigUtils.initLogger();
        
		String configFile = WarehouseConfig.DEFAULT_CONFIG_FILE;
		if (args != null && args.length > 0) {
			configFile = args[0];
		}
		
		WarehouseConfig wsConfig = ConfigUtils.loadConfigBean(configFile, WarehouseConfig.class);
		wsConfig.postLoadCalculateValues();
		logger.info(wsConfig);
		
		CASClient casClient = CASClient.configure(wsConfig);
		String jSessionID = casClient.login();
		wsConfig.jSessionID = jSessionID;
		
		ResponseHandler handler = new ResponseHandler(wsConfig);
		WarehouseClient wsClient = WarehouseClient.configure(wsConfig, handler);
		logger.info("Opening WebSocket to " + wsConfig.warehouseURL);
		wsClient.connect();

		RequestHandler watcher = new RequestHandler(wsConfig.requestDir, wsClient);
		watcher.start();
		
		StringBuilder depositConfigErrors = new StringBuilder();
		if (wsConfig.systemDepositName == null)
			depositConfigErrors.append("systemDepositName is required, ");
		if (wsConfig.encodeType == null)
			depositConfigErrors.append("encodeType is required, ");
		if (wsConfig.depositFileDir == null)
			depositConfigErrors.append("depositFileDir is required ");
		if (depositConfigErrors.length() > 0) {
			logger.error("Configuration errors: " + depositConfigErrors.toString());
			System.exit(-1);
		}
		DepositHandler depositHandler = new DepositHandler(wsConfig.depositFileDir, wsClient, wsConfig);
		depositHandler.start();

		while (true) {
			Thread.sleep(1000);
		}
	}
}
