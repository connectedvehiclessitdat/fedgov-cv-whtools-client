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
package gov.usdot.cv.whtools.client.config;


public class WarehouseConfig {

	public static final String DEFAULT_CONFIG_FILE = "config/whconfig.json";
	public static final String DEFAULT_OUTPUT_DIR = "responses";
	public static final String JSESSIONID_KEY = "JSESSIONID";
	
	public String warehouseURL;
	public String httpWarehouseURL;
	public String keystoreFile;
	public String keystorePassword;
	public String casURL;
	public String casUserName;
	public String casPassword;
	public String jSessionID;
	public String requestDir;
	public boolean logMessages;
	public boolean writeToDisk;
	public boolean binaryFiles;
	public String responseDir;
	public String systemDepositName;
	public String encodeType;
	public String depositFileDir;
	public int depositDelay;
    	
	@Override
	public String toString() {
		return "WarehouseConfig [warehouseURL=" + warehouseURL
				+ ", httpWarehouseURL=" + httpWarehouseURL + ", keystoreFile="
				+ keystoreFile + ", keystorePassword=" + keystorePassword
				+ ", casURL=" + casURL + ", casUserName=" + casUserName
				+ ", casPassword=" + casPassword + ", jSessionID=" + jSessionID
				+ ", requestDir=" + requestDir + ", logMessages=" + logMessages
				+ ", writeToDisk=" + writeToDisk + ", binaryFiles="
				+ binaryFiles + ", responseDir=" + responseDir
				+ ", systemDepositName=" + systemDepositName + ", encodeType="
				+ encodeType + ", depositFileDir=" + depositFileDir
				+ ", depositDelay=" + depositDelay + "]";
	}

	public void postLoadCalculateValues() {
		httpWarehouseURL = "https" + warehouseURL.substring(warehouseURL.indexOf(":"));
		if (responseDir == null || responseDir.isEmpty()) {
			responseDir = DEFAULT_OUTPUT_DIR;
		}
	}
	
}
