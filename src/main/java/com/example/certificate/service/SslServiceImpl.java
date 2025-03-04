package com.example.certificate.service;

import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.springframework.stereotype.Service;

import com.example.certificate.exception.ServiceException;

@Service
public class SslServiceImpl implements SslService {

    @Override
    public String getSslCertificatePay(String webUrl, String name) throws ServiceException {
        try {
            X509Certificate certificate = getSslCertificate(webUrl);
            if (certificate != null) {
                return formatCertificateDetails(certificate, webUrl, name);
            }
        } catch (IOException e) {
            return formatErrorCertDetails(webUrl, name);
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

    private String formatCertificateDetails(X509Certificate certificate, String urlString, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n")
                .append("Хост: ").append(urlString).append("\n")
                .append("Истикает: ").append(certificate.getNotAfter()).append("\n\n");

        return sb.toString();
    }

    private String formatErrorCertDetails(String url, String name) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("\n")
                .append("Для хоста: ").append(url).append("\n")
                .append("Информация о сертификате не найдена или сертификата не требуется проверка.\n\n");

        return sb.toString();
    }
}