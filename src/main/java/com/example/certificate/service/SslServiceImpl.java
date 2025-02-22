package com.example.certificate.service;

import com.example.certificate.exception.ServiceException;
import org.springframework.stereotype.Service;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Service
public class SslServiceImpl implements SslService  {

    @Override
    public String getSslCertificatePay() throws ServiceException {
        String urlString = "WebSite";
        try {
            X509Certificate certificate = getSslCertificate(urlString);
            if (certificate != null) {
                return formatCertificateDetails(certificate, urlString);
            }
        } catch (IOException e) {
            throw new ServiceException("Не удалось получить SSL сертификат.", e);
        }
        return "SSL сертификат не найден.";
    }

    @Override
    public String getSslCertificateIft() throws ServiceException {
        String urlString = "WebSite";
        try {
            X509Certificate certificate = getSslCertificate(urlString);
            if (certificate != null) {
                return formatCertificateDetails(certificate, urlString);
            }
        } catch (IOException e) {
            throw new ServiceException("Не удалось получить SSL сертификат.", e);
        }
        return "SSL сертификат не найден.";
    }

    private static X509Certificate getSslCertificate(String urlString) throws IOException {
        HttpsURLConnection connection = null;
        try {
            connection = (HttpsURLConnection) new URL(urlString).openConnection();
            connection.connect();

            Certificate[] certificates = connection.getServerCertificates();
            if (certificates != null && certificates.length > 0) {
                return (X509Certificate) certificates[0];
            }
        } catch (SSLPeerUnverifiedException e) {
            throw new RuntimeException("Не удалось проверить сертификат узла.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private String formatCertificateDetails(X509Certificate certificate, String urlString) {
        StringBuilder sb = new StringBuilder();
        sb.append("Certificate Viewer: ").append(urlString).append("\n")
                .append("Issued On: ").append(certificate.getNotBefore()).append("\n")
                .append("Expires On: ").append(certificate.getNotAfter()).append("\n");

        return sb.toString();
    }
}