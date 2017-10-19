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
package gov.usdot.cv.whtools.client.handler;


import gov.usdot.cv.whtools.client.WarehouseClient;
import gov.usdot.cv.whtools.client.config.WarehouseConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;

public class DepositHandler {

	private static final Logger logger = Logger.getLogger(DepositHandler.class
			.getName());
	
	private final static String ENCODE_TYPE_HEX = "hex";
	private final static String ENCODE_TYPE_BASE64 = "base64";
	private final static String ENCODE_TYPE_BER = "ber";
	
	private String watchDirectory;
	private int pollingInterval = 2000;
	private WarehouseClient wsClient;
	private WarehouseConfig whConfig;
	private String depositMessageFormat = "DEPOSIT: { \"systemDepositName\": \"%s\", \"encodeType\": \"%s\", \"encodedMsg\": \"%s\" }";

	public DepositHandler(String watchDirectory, WarehouseClient wsClient, WarehouseConfig whConfig) {
		this.watchDirectory = watchDirectory;
		this.wsClient = wsClient;
		this.whConfig = whConfig;
	}

	public void start() {
		FileAlterationObserver observer = new FileAlterationObserver(this.watchDirectory);
		FileAlterationMonitor monitor = new FileAlterationMonitor(this.pollingInterval);
		FileAlterationListener listener = new FileListener();
		observer.addListener(listener);
		monitor.addObserver(observer);
		try {
			monitor.start();
			logger.info("Watching for files to deposit in: " + new File(this.watchDirectory).getAbsolutePath());
		} catch (Exception e) {
			logger.info("DirectoryWatcher failed to start", e);
		}
	}
	
	private class FileListener extends FileAlterationListenerAdaptor {
		@Override
		public void onFileCreate(File file) {
			logger.info("File created: " + file.getAbsolutePath());
			depositFile(file);
		}

		@Override
		public void onFileChange(File file) {
			if (file.exists()) {
				logger.info("File changed: " + file.getAbsolutePath());
				depositFile(file);
			}
		}
		
		private void depositFile(File file) {
			try {
				if (whConfig.encodeType.equalsIgnoreCase(ENCODE_TYPE_HEX) || whConfig.encodeType.equalsIgnoreCase(ENCODE_TYPE_BASE64)) {
					List<String> fileLines = FileUtils.readLines(file);
					for (String line: fileLines) {
						String depositMessage = String.format(depositMessageFormat, whConfig.systemDepositName, whConfig.encodeType, line);
						logger.info("Sending deposit message " + depositMessage);
						wsClient.send(depositMessage);
						try { Thread.sleep(whConfig.depositDelay); } catch (InterruptedException e) {}
					}
				} else if (whConfig.encodeType.equalsIgnoreCase(ENCODE_TYPE_BER)) {
					char[] message = Hex.encodeHex(FileUtils.readFileToByteArray(file));
					String depositMessage = String.format(depositMessageFormat, whConfig.systemDepositName, whConfig.encodeType, new String(message));
					logger.info("Sending deposit message " + depositMessage);
					wsClient.send(depositMessage);
					try { Thread.sleep(whConfig.depositDelay); } catch (InterruptedException e) {}
				}
			} catch (IOException e) {
				logger.error("Error reading deposit file ", e);
			}
		}
	};
}
