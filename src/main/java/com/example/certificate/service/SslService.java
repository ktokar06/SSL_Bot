package com.example.certificate.service;

import com.example.certificate.exception.ServiceException;

public interface SslService {
    String getSslCertificatePay(String webUrl, String name) throws ServiceException;
}