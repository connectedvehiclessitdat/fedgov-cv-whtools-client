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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.codehaus.jackson.map.ObjectMapper;

public class ConfigUtils {

	private static final Logger logger = Logger.getLogger(ConfigUtils.class
			.getName());
	
	private static final ObjectMapper mapper;
	
	static {
		mapper = new ObjectMapper();
	}

	public static <T> T loadConfigBean(String fileName, Class<T> genericType) throws ConfigException {
		InputStream is = ConfigUtils.getFileAsStream(fileName);
		if (is == null) {
			throw new ConfigException("Config file " + fileName + " not found");
		}
		T configBean = null;
		try {
			configBean = mapper.readValue(is, genericType);
		} catch (Exception e) {
			throw new ConfigException(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.warn("Couldn't close input stream", e);
			}
			is = null;
		}
		return configBean;
	}

	private static InputStream getFileAsStream(String fileName)  {
		InputStream is = null;
		File f = new File(fileName);
		logger.info("Attempting to find file " + fileName);
		if (f.exists()) {
			logger.debug("Loading file from the file system " + f.getAbsolutePath());
			try {
				is = new FileInputStream(f);
			} catch (FileNotFoundException e) {
				logger.warn(e);
			}
		} else {
			logger.debug("File not found on file system, checking on classpath...");
			is = ConfigUtils.class.getClassLoader().getResourceAsStream(fileName);
		}
		if (is == null) {
			logger.error("File " + fileName + " could not be found");
		}
		return is;
	}
	
	public static void initLogger() {
		@SuppressWarnings("rawtypes")
		Enumeration appenders = LogManager.getRootLogger().getAllAppenders();
        if (!appenders.hasMoreElements()) {
        	Logger rootLogger = Logger.getRootLogger();
        	rootLogger.setLevel(Level.INFO);
        	
        	PatternLayout layout = new PatternLayout("%d{ISO8601} %5p %c{1}:%L - %m%n");
        	rootLogger.addAppender(new ConsoleAppender(layout));
        	rootLogger.info("Log4J not configured! Setting up default console configuration");
        }
	}
}
