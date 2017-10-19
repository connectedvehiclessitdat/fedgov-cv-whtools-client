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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;

public class SSLBuilder {

	public static SSLContext buildSSLContext(String keystoreFile,
			String storePassword) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException,
			KeyManagementException {

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream instream = new FileInputStream(new File(keystoreFile));
		try {
			trustStore.load(instream, storePassword.toCharArray());
		} finally {
			instream.close();
		}

		// Trust own CA and all self-signed certs
		SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
				.build();
		return sslcontext;
	}
	
	public static SSLContext buildSSLContext() throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException,
			KeyManagementException {
		SSLContext sslcontext = SSLContexts.createDefault();
		return sslcontext;
	}

	public static SSLConnectionSocketFactory buildSSLConnectionSocketFactory(
			SSLContext sslcontext) {
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslcontext, new String[] { "TLSv1", "TLSv1.1", "TLSv1.2" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		return sslsf;
	}
}
