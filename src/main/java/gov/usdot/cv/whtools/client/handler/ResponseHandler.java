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

import gov.usdot.cv.whtools.client.config.WarehouseConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

public class ResponseHandler {

	private static final Logger logger = Logger.getLogger(ResponseHandler.class
			.getName());
	
	private static final String NEW_LINE = System.getProperty("line.separator");
	private static final String CONNECTED_TAG = "CONNECTED:";
	private static final String START_TAG = "START:";
	private static final String STOP_TAG = "STOP:";
	private static final String ERROR_TAG = "ERROR:";
	private static final Map<Integer,String> dialogIDPrefixLookup = new HashMap<Integer,String>();
	private static int fileCounter = 0;
	
	static {
		dialogIDPrefixLookup.put(-1, "all");
		dialogIDPrefixLookup.put(154, "vsd");
		dialogIDPrefixLookup.put(156, "adv");
		dialogIDPrefixLookup.put(162, "isd");
	}
	
	private WarehouseConfig wsConfig;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
	private FileOutputStream textFileOutStream;
	
	private ObjectMapper mapper = new ObjectMapper();
	private String messageTypePrefix = "msg";
	private String resultEncoding = "full";
	
	public ResponseHandler(WarehouseConfig wsConfig) {
		this.wsConfig = wsConfig;
		if (wsConfig.writeToDisk) {
			File dir = new File(wsConfig.responseDir);
			if (!dir.exists())
				dir.mkdir();
		}
	}
	
	public void handleMessage(String message) {
		if (wsConfig.logMessages) {
			logger.info(message);
		}
		
		if (wsConfig.writeToDisk) {
			writeMessageToDisk(message);
		}
		
		// Add whatever additional processing you want here
	}
	
	private void writeMessageToDisk(String message) {
		if (message.startsWith(CONNECTED_TAG) || message.startsWith(START_TAG) || 
			message.startsWith(STOP_TAG) || message.startsWith(ERROR_TAG)) {
			if (message.startsWith(START_TAG)) {
				processStartTag(message);
			} else if (message.startsWith(ERROR_TAG)) {
				logger.error(message);
			}
		} else {
			OutputStream binaryOutputStream = null;
			OutputStream textOutputStream = null;
			try {
				if (wsConfig.binaryFiles) {
					if (resultEncoding.equalsIgnoreCase("full")) {
						textOutputStream = getTextFileOutputStream("msg");
						IOUtils.write(message + NEW_LINE, textOutputStream);
					} else {
						byte[] bytes = null;
						if (resultEncoding.equalsIgnoreCase("hex")) {
							try {
								bytes = Hex.decodeHex(message.toCharArray());
							} catch (DecoderException e) {
								logger.error("Hex to byte conversion failed" + e);
							}
						} else if (resultEncoding.equalsIgnoreCase("base64")) {
							bytes = Base64.decodeBase64(message);
						} else {
							logger.warn("Unexpected resultEncoding of " + resultEncoding);
						}
						
						if (bytes != null) {
							binaryOutputStream = getBinaryFileOutputStream(messageTypePrefix);
							IOUtils.write(bytes, binaryOutputStream);
						} else {
							logger.warn("Failed to decode bytes, Not writing out file!");
						}
					}
				} else {
					textOutputStream = getTextFileOutputStream("msg");
					IOUtils.write(message + NEW_LINE, textOutputStream);
				}
			} catch (IOException e) {
				logger.error("Failed to write received message to disk", e);
			} finally {
				if (textOutputStream != null) {
					try {
						textOutputStream.flush();
					} catch (IOException e) {
						logger.warn(e);
					}
				}
				if (binaryOutputStream != null) {
					IOUtils.closeQuietly(binaryOutputStream);
				}
			}
		}
	}
	
	private void processStartTag(String message) {
		if (message.startsWith(START_TAG)) {
			// default values in case parsing fails
			messageTypePrefix = "msg";
			resultEncoding = "full";
			String jsonMessage = message.substring(START_TAG.length());
			try {
				JsonNode rootNode = mapper.readTree(jsonMessage);
				messageTypePrefix = dialogIDPrefixLookup.get(rootNode.get("dialogID").asInt());
				resultEncoding = rootNode.get("resultEncoding").getTextValue();
			} catch (JsonProcessingException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}
	
	private OutputStream getBinaryFileOutputStream(String prefix) {
		FileOutputStream fos = null;
		try {
			String fileName = String.format("%s_%s_%s_%s.ber", prefix, sdf.format(new Date()), 
					System.currentTimeMillis(), fileCounter++);
			File f = new File(wsConfig.responseDir,fileName);
			fos = new FileOutputStream(f);
		} catch (FileNotFoundException e) {
			logger.error("Failed to create binary output file", e);
		}
		return fos;
	}
	
	private OutputStream getTextFileOutputStream(String prefix) {
		if (textFileOutStream != null) {
			return textFileOutStream;
		} else {
			try {
				String fileName = String.format("%s_%s_%s.txt", prefix, sdf.format(new Date()), System.currentTimeMillis());
				File f = new File(wsConfig.responseDir,fileName);
				textFileOutStream = new FileOutputStream(f);
			} catch (FileNotFoundException e) {
				logger.error("Failed to create text output file", e);
			}
			return textFileOutStream;
		}
	}
}
