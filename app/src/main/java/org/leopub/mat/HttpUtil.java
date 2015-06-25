/*
 * Copyright (C) 2015 Liang Jing
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.leopub.mat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.leopub.mat.exception.AuthException;
import org.leopub.mat.exception.NetworkException;

public class HttpUtil {
    private static void authByHeaders(User user, Map<String, List<String>> headers) throws AuthException {
        user.setCookieId(null);
        if (headers.containsKey("Set-Cookie")) {
            List<String> values = headers.get("Set-Cookie");
            for(String value: values) {
                String[] items = value.split(";");
                for (String item : items) {
                    if (item.startsWith("PHPSESSID")) {
                        user.setSessionId(item.split("=")[1]);
                    } else if (item.startsWith("COOKIEID")) {
                        user.setCookieId(item.split("=")[1]);
                    }
                }
            }
        }
        if (!user.isLogedIn()) {
            throw new AuthException();
        }
    }

    private static String getCookieString(User user) {
        StringBuilder sb = new StringBuilder();
        if (user.getSessionId() != null) {
            sb.append("PHPSESSID=");
            sb.append(user.getSessionId());
        }
        if (user.getCookieId() != null) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append("COOKIEID=");
            sb.append(user.getCookieId());
            sb.append("; USERNAME=");
            sb.append(user.getUsername());
        }
        return sb.toString();
    }

    public static void auth(User user, String password) throws NetworkException, AuthException {
        HttpURLConnection conn = null;
        String params = "username=" + user.getUsername() + "&password=" + password;
        try {
            conn = (HttpURLConnection) new URL(Configure.LOGIN_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
            conn.setRequestProperty("Content-Language", "en-us");
            //conn.setRequestProperty("Cookie", "PHPSESSION=value");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream outputStream = conn.getOutputStream();
            DataOutputStream sendStream = new DataOutputStream(outputStream);
            sendStream.writeBytes(params);
            sendStream.flush();
            sendStream.close();
            authByHeaders(user, conn.getHeaderFields());
        } catch (IOException e) {
            throw new NetworkException(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String getUrl(User user, String url) throws NetworkException, AuthException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Language", "en-us");
            conn.setRequestProperty("Cookie", getCookieString(user));
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            DataOutputStream sendStream = new DataOutputStream(conn.getOutputStream());
            sendStream.flush();
            sendStream.close();

            InputStream responseStream = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = responseStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            responseStream.close();
            authByHeaders(user, conn.getHeaderFields());
            return new String(baos.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new NetworkException(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static String postURL(User user, String url, String params) throws NetworkException, AuthException {
        StringBuilder sb = new StringBuilder();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Cookie", getCookieString(user));
            conn.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
            conn.setUseCaches(false);

            DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
            writer.writeBytes(params);
            writer.flush();
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            while (reader.ready()) {
                sb.append(reader.readLine());
                sb.append("\n");
            }
            reader.close();
            authByHeaders(user, conn.getHeaderFields());
        } catch (IOException e) {
            throw new NetworkException(e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return sb.toString();
    }
}
