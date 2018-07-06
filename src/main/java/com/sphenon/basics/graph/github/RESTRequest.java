package com.sphenon.basics.graph.github;

/****************************************************************************
  Copyright 2001-2018 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import com.sphenon.basics.context.*;
import com.sphenon.basics.data.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URLEncoder;
import java.util.regex.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;

public class RESTRequest {

    public RESTRequest(CallContext context, int verbose) {
        this.verbose = verbose;
        Data_MediaObject_URL.initialise(context);
    }

    protected String sessioncookie;

    static public class Result {
        public InputStream is;
        public byte[]      bytes;
        public String      content;
        public Matcher[]   matcher;
        public int         count;
        public int         status_code;
        public Throwable   exception;
        public JsonNode    node;
    }

    static public String proxy_host;
    static public String proxy_port;
    static public String proxy_user;
    static public String proxy_passwd;

    public Result result;
    public String disposition_filename;
    public String content_type;

    public Result sendRequest(CallContext context, String url_string, String request_method, Object content, File write_to_file, boolean write_to_string, boolean parse_as_json, boolean use_disposition_name, boolean throw_exceptions, String... checks) {

        this.disposition_filename = null;
        this.result = new Result();

        try {
            if (verbose >= 1) {
                System.err.print("get: " + url_string + "\n[");
            }

            if (checks.length > 0 || parse_as_json) {
                write_to_string = true;
            }

            URL url = new URL(url_string);
            URLConnection connection = url.openConnection();
            if (sessioncookie != null && sessioncookie.length() != 0) {
                connection.addRequestProperty("Cookie",sessioncookie);
                if (verbose >= 1) {
                    System.err.print("sid=" + sessioncookie + ",");
                }
            }

            if (content != null) {
                if (request_method == null || request_method.isEmpty()) { request_method = "POST"; }
                ((HttpURLConnection)connection).setRequestMethod(request_method);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                if (content instanceof MimeMultipart) {
                    MimeMultipart mbp = (MimeMultipart) content;

                    connection.setRequestProperty("Content-Type", mbp.getContentType());
                    if (verbose >= 1) {
                        System.err.print("MIME,");
                    }
                    OutputStream out = connection.getOutputStream();
                    mbp.writeTo(out);
                    out.close();
                    if (verbose >= 2) {
                        System.err.println("");
                        mbp.writeTo(System.err);
                        System.err.println("");
                    }
                } else if (content instanceof String) {
                    String formdata = (String) content;

                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    if (verbose >= 1) {
                        System.err.print("FORMDATA,");
                    }
                    OutputStream out = connection.getOutputStream();
                    out.write(formdata.getBytes("UTF-8"));
                    out.close();
                    if (verbose >= 2) {
                        System.err.println("");
                        System.err.println(formdata);
                        System.err.println("");
                    }
                }
            } else {
                if (request_method != null && request_method.isEmpty() == false) {
                    ((HttpURLConnection)connection).setRequestMethod(request_method);
                }
            }

            Map<String,List<String>> headers = connection.getHeaderFields();

            // schoen waers.... (in java 1.5 gibts nur das interface, impl erst ab 1.6)
            // CookieHandler.setDefault(new CookieManager());

            for (String key : headers.keySet()) {
                String sep="";
                if (verbose >= 1) {
                  System.err.print(key + "=[");
                }
                for (String value : headers.get(key)) {
                    if (verbose >= 1) {
                      System.err.print(value + sep);
                      sep = ",";
                    }
                    if (key != null) {
                        if (key.equals("Set-Cookie")) {
                            if (sessioncookie == null) {
                                sessioncookie = "";
                            } else {
                                sessioncookie += "; ";
                            }
                            sessioncookie += value.replaceFirst(";.*$","");
                            if (verbose >= 1) {
                                System.err.print(sessioncookie+",");
                            }
                        } else if (key.equals("Content-Disposition")) {
                            Pattern p = null;
                            try {
                                p = Pattern.compile(".*filename=\"([^\"]+)\".*");
                            } catch (PatternSyntaxException pse) {
                                pse.printStackTrace();
                                if (throw_exceptions) { throw pse; }
                            }
                            Matcher matcher = p.matcher(value);
                            if (matcher.find()) {
                                this.disposition_filename = matcher.group(1);
                            }
                        } else if (key.equals("Content-Type")) {
                            this.content_type = value;
                        }
                    }
                }
                if (verbose >= 1) {
                  System.err.println("]");
                }

            }

            if (verbose >= 1) {
                System.err.print("tx");
            }

            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                this.result.status_code = httpConnection.getResponseCode();
            }

            if (this.result.status_code == 204) {
                // NO_CONTENT
            } else if (this.result.status_code != 200) {
                InputStream is = connection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writeData(is, null, baos);
                this.result.content = baos.toString("UTF-8");
            } else {
                InputStream is = connection.getInputStream();

                if (write_to_string || write_to_file != null) {
                    this.result.count = 0;
                    FileOutputStream fos = null;
                    ByteArrayOutputStream baos = null;
                    if (write_to_string) {
                        baos = new ByteArrayOutputStream();
                    }
                    if (use_disposition_name && write_to_file != null && this.content_type.matches("multipart/.*")) {
                        if (baos == null) {
                            baos = new ByteArrayOutputStream();
                        }
                        writeData(is, fos, baos);
                        this.result.content = baos.toString("UTF-8");
                        javax.mail.internet.MimeMultipart mmp = new javax.mail.internet.MimeMultipart(new javax.mail.util.ByteArrayDataSource(this.result.content.getBytes("UTF-8"), "text/plain"));
                        int nop = mmp.getCount();
                        for (int bpi = 0; bpi < nop; bpi++) {
                            javax.mail.BodyPart bp = mmp.getBodyPart(bpi);
                            InputStream isp = bp.getInputStream();
                            File out = new File(write_to_file, bp.getFileName());
                            if (verbose >= 1) {
                                System.err.print(" File:" + out.getPath());
                            }
                            fos = new FileOutputStream(out);
                            writeData(isp, fos, null);
                            fos.close();
                            isp.close();
                        }
                    } else {
                        File out = use_disposition_name ? new File(write_to_file, this.disposition_filename) : write_to_file;
                        if (write_to_file != null) {
                            if (verbose >= 1) {
                                System.err.print(" File:" + out.getPath());
                            }
                            fos = new FileOutputStream(out);
                        }
                        writeData(is, fos, baos);
                        if (fos != null) { fos.close(); }
                        if (baos != null) { 
                            this.result.content = baos.toString("UTF-8");
                        }
                    }
                    is.close();

                    if (verbose >= 1) {
                        System.err.println("" + this.result.count + "]");
                    }
                    if (baos != null) { 
                        if (verbose >= 2) {
                            System.err.println(this.result.content);
                        }

                        Pattern[] p = new Pattern[checks == null ? 0 : checks.length];
                        Matcher[] m = new Matcher[checks == null ? 0 : checks.length];

                        int i = 0;
                        for (String check : checks) {
                            p[i] = Pattern.compile("(?s)"+check);
                            m[i] = p[i].matcher(this.result.content);
                            if (m[i].find() == false) {
                                System.err.println("ERROR: invalid answer (does not match \"" + check + "\"");
                                System.exit(2);
                            }
                            i++;
                        }

                        this.result.bytes   = baos.toByteArray();
                        this.result.matcher = m;
                    }
                } else {
                    this.result.is = is;
                }
            }

            if (parse_as_json) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    result.node = mapper.readTree(result.content);
                } catch (java.io.IOException ioe) {
                    System.err.println("ERROR: invalid answer (could not parser json): " + ioe);
                    System.exit(2);
                }
            }
        } catch (Throwable exception) {
            this.result.exception = exception;
        }

        return this.result;
    }

    protected void writeData(InputStream is, FileOutputStream fos, ByteArrayOutputStream baos) throws IOException {
        byte[] buf = new byte[4096];
        int bread;
        while ((bread = is.read(buf, 0, 4096)) != -1) {
            if (baos != null) { baos.write(buf, 0, bread); }
            if (fos != null) { fos.write(buf, 0, bread); }
            this.result.count += bread;
            if (verbose >= 1) {
                System.err.print("."); 
            }
        }
    }    

    protected int verbose;

    static protected void attachFile(MimeMultipart multiPart, String name, String filename) throws Throwable {
         MimeBodyPart bodyPart = new MimeBodyPart();
         bodyPart.setDisposition("form-data; name=\"" + name + "\";");
         // bodyPart.setFileName(filename);
         FileDataSource fds = new FileDataSource(filename);
         // fds.setFileTypeMap(new FileTypeMap() {
         // public String getContentType(File file) { return "text/plain"; }
         // public String getContentType(String filename) { return "text/plain"; }
         // });
         // System.err.println("fds ContentType: " + fds.getContentType());
         bodyPart.setHeader("Content-Type", fds.getContentType());
         bodyPart.setDataHandler(new DataHandler(fds));
         multiPart.addBodyPart(bodyPart);
    }

    static protected void attachText(MimeMultipart multiPart, String name, String text) throws Throwable {
         MimeBodyPart bodyPart = new MimeBodyPart();
         bodyPart.setDisposition("form-data; name=\"" + name + "\";");
         bodyPart.setText(text);
         multiPart.addBodyPart(bodyPart);
    }

    // -------------------------------------------------------------------------------------

    static public String recode_UTF8_URI(String string) {
        byte[] bytes = null;
        try {
            bytes = string.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
        StringBuffer sb = new StringBuffer();
        for (int b : bytes) {
            if (b < 0) { b += 256; }
            int code = (b > 0x007f ? 5 : URICharCode[b]);
            sb.append(code >= 2 ? ("%" + hex[b]) : ((char)b));
        }
        return sb.toString();
    }

    final static public String[] hex =
    {
        "00", "01", "02", "03", "04", "05", "06", "07",
        "08", "09", "0A", "0B", "0C", "0D", "0E", "0F",
        "10", "11", "12", "13", "14", "15", "16", "17",
        "18", "19", "1A", "1B", "1C", "1D", "1E", "1F",
        "20", "21", "22", "23", "24", "25", "26", "27",
        "28", "29", "2A", "2B", "2C", "2D", "2E", "2F",
        "30", "31", "32", "33", "34", "35", "36", "37",
        "38", "39", "3A", "3B", "3C", "3D", "3E", "3F",
        "40", "41", "42", "43", "44", "45", "46", "47",
        "48", "49", "4A", "4B", "4C", "4D", "4E", "4F",
        "50", "51", "52", "53", "54", "55", "56", "57",
        "58", "59", "5A", "5B", "5C", "5D", "5E", "5F",
        "60", "61", "62", "63", "64", "65", "66", "67",
        "68", "69", "6A", "6B", "6C", "6D", "6E", "6F",
        "70", "71", "72", "73", "74", "75", "76", "77",
        "78", "79", "7A", "7B", "7C", "7D", "7E", "7F",
        "80", "81", "82", "83", "84", "85", "86", "87",
        "88", "89", "8A", "8B", "8C", "8D", "8E", "8F",
        "90", "91", "92", "93", "94", "95", "96", "97",
        "98", "99", "9A", "9B", "9C", "9D", "9E", "9F",
        "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7",
        "A8", "A9", "AA", "AB", "AC", "AD", "AE", "AF",
        "B0", "B1", "B2", "B3", "B4", "B5", "B6", "B7",
        "B8", "B9", "BA", "BB", "BC", "BD", "BE", "BF",
        "C0", "C1", "C2", "C3", "C4", "C5", "C6", "C7",
        "C8", "C9", "CA", "CB", "CC", "CD", "CE", "CF",
        "D0", "D1", "D2", "D3", "D4", "D5", "D6", "D7",
        "D8", "D9", "DA", "DB", "DC", "DD", "DE", "DF",
        "E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7",
        "E8", "E9", "EA", "EB", "EC", "ED", "EE", "EF",
        "F0", "F1", "F2", "F3", "F4", "F5", "F6", "F7",
        "F8", "F9", "FA", "FB", "FC", "FD", "FE", "FF"
    };

    final static public int [] URICharCode = {
        /*
          Character codes according to RFC 2396 - URI Generic Syntax
          see http://www.ics.uci.edu/pub/ietf/uri/rfc2396.txt

          code  charset       escape in URLs

           0     alnum         could
           1     mark          could
           2     reserved      used in URL-part: must
                               as URL seperator: must not
           3     unsafe        should
           4     veryunsafe    must
       */

        4 /* ? 00 */, 4 /* ? 01 */, 4 /* ? 02 */, 4 /* ? 03 */, 4 /* ? 04 */, 4 /* ? 05 */, 4 /* ? 06 */, 4 /* ? 07 */,
        4 /* ? 08 */, 4 /* ? 09 */, 4 /* ? 0A */, 4 /* ? 0B */, 4 /* ? 0C */, 4 /* ? 0D */, 4 /* ? 0E */, 4 /* ? 0F */,
        4 /* ? 10 */, 4 /* ? 11 */, 4 /* ? 12 */, 4 /* ? 13 */, 4 /* ? 14 */, 4 /* ? 15 */, 4 /* ? 16 */, 4 /* ? 17 */,
        4 /* ? 18 */, 4 /* ? 19 */, 4 /* ? 1A */, 4 /* ? 1B */, 4 /* ? 1C */, 4 /* ? 1D */, 4 /* ? 1E */, 4 /* ? 1F */,
        3 /*   20 */, 1 /* ! 21 */, 3 /* " 22 */, 3 /* # 23 */, 2 /* $ 24 */, 3 /* % 25 */, 2 /* & 26 */, 1 /* ' 27 */,
        1 /* ( 28 */, 1 /* ) 29 */, 1 /* * 2A */, 2 /* + 2B */, 2 /* , 2C */, 1 /* - 2D */, 1 /* . 2E */, 2 /* / 2F */,
        0 /* 0 30 */, 0 /* 1 31 */, 0 /* 2 32 */, 0 /* 3 33 */, 0 /* 4 34 */, 0 /* 5 35 */, 0 /* 6 36 */, 0 /* 7 37 */,
        0 /* 8 38 */, 0 /* 9 39 */, 2 /* : 3A */, 2 /* ; 3B */, 3 /* < 3C */, 2 /* = 3D */, 3 /* > 3E */, 2 /* ? 3F */,
        2 /* @ 40 */, 0 /* A 41 */, 0 /* B 42 */, 0 /* C 43 */, 0 /* D 44 */, 0 /* E 45 */, 0 /* F 46 */, 0 /* G 47 */,
        0 /* H 48 */, 0 /* I 49 */, 0 /* J 4A */, 0 /* K 4B */, 0 /* L 4C */, 0 /* M 4D */, 0 /* N 4E */, 0 /* O 4F */,
        0 /* P 50 */, 0 /* Q 51 */, 0 /* R 52 */, 0 /* S 53 */, 0 /* T 54 */, 0 /* U 55 */, 0 /* V 56 */, 0 /* W 57 */,
        0 /* X 58 */, 0 /* Y 59 */, 0 /* Z 5A */, 3 /* [ 5B */, 3 /* \ 5C */, 3 /* ] 5D */, 3 /* ^ 5E */, 1 /* _ 5F */,
        3 /* ` 60 */, 0 /* a 61 */, 0 /* b 62 */, 0 /* c 63 */, 0 /* d 64 */, 0 /* e 65 */, 0 /* f 66 */, 0 /* g 67 */,
        0 /* h 68 */, 0 /* i 69 */, 0 /* j 6A */, 0 /* k 6B */, 0 /* l 6C */, 0 /* m 6D */, 0 /* n 6E */, 0 /* o 6F */,
        0 /* p 70 */, 0 /* q 71 */, 0 /* r 72 */, 0 /* s 73 */, 0 /* t 74 */, 0 /* u 75 */, 0 /* v 76 */, 0 /* w 77 */,
        0 /* x 78 */, 0 /* y 79 */, 0 /* z 7A */, 3 /* { 7B */, 3 /* | 7C */, 3 /* } 7D */, 1 /* ~ 7E */, 4 /*   7F */
    };

    // -------------------------------------------------------------------------------------

    public static void main(String[] args) {

        String  host     = "https://api.github.com"; 
        String  user     = null;
        String  repo     = null;
        String  path     = null;
        String  password = null;
        boolean invalid  = false;
        int     verbose  = 0;

        for (int i=0; i<args.length; i++) {
            String arg=args[i]; 
            if      (arg.equals("-v")  && verbose < 1) { verbose = 1; }
            else if (arg.equals("-vv") && verbose < 2) { verbose = 2; }
            else if (arg.equals("-host"))              { host = args[++i]; }
            else if (arg.equals("-proxyhost"))         { proxy_host   = args[++i]; }
            else if (arg.equals("-proxyport"))         { proxy_port   = args[++i]; }
            else if (arg.equals("-proxyuser"))         { proxy_user   = args[++i]; }
            else if (arg.equals("-proxypassword"))     { proxy_passwd = args[++i]; }
            else if (arg.equals("-password"))          { password = args[++i]; }
            else if (user == null)                     { user = arg; }
            else if (repo == null)                     { repo = arg; }
            else if (path == null)                     { path = arg; }
            else                                       { invalid = true; }
        }

        if (    user == null
             || user.length() == 0
             || repo == null
             || repo.length() == 0
             || invalid
           ) {
            System.err.println("Usage: github [-host -password] <user> <repo> <path>");
            System.exit(10);
        }

        try {

            RESTRequest rr = new RESTRequest(null, verbose);
            Result result = rr.sendRequest(null, host + "/" + user + "/" + repo + (path == null || path.isEmpty() ? "" : ("/" + path)), "GET", null, null, true, true, false, false);

            System.err.println(result.content);

        } catch (Throwable t) {
            t.printStackTrace();
        }        
    }
}
