/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.androidumloader.download;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Provides retrieving of {@link InputStream} of image by URI.<br />
 * Implementations have to be thread-safe.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.4.0
 */
public interface ImageDownloader {

	InputStream getStream(String imageUri, Object extra) throws IOException;

	/** Represents supported schemes(protocols) of URI. Provides convenient methods for work with schemes and URIs. */
	public enum Scheme {
		HTTP("http"), HTTPS("https"), FILE("file"), CONTENT("content"), ASSETS("assets"), DRAWABLE("drawable"), UNKNOWN("");

		private String scheme;
		private String uriPrefix;

		Scheme(String scheme) {
			this.scheme = scheme;
			uriPrefix = scheme + "://";
		}

		/**
		 * Defines scheme of incoming URI
		 *
		 * @param uri URI for scheme detection
		 * @return Scheme of incoming URI
		 */
		public static Scheme ofUri(String uri) {
			if (uri != null) {
				for (Scheme s : values()) {
					if (s.belongsTo(uri)) {
						return s;
					}
				}
			}
			return UNKNOWN;
		}

		private boolean belongsTo(String uri) {
			return uri.toLowerCase(Locale.US).startsWith(uriPrefix);
		}

		/** Appends scheme to incoming path */
		public String wrap(String path) {
			return uriPrefix + path;
		}

		/** Removed scheme part ("scheme://") from incoming URI */
		public String crop(String uri) {
			if (!belongsTo(uri)) {
				throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected scheme [%2$s]", uri, scheme));
			}
			return uri.substring(uriPrefix.length());
		}
	}
}
