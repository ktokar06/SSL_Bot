package com.example.certificate.service;

import com.example.certificate.exception.ServiceException;

public interface SslService {
    String getSslCertificatePay() throws ServiceException;
    String getSslCertificateIft() throws ServiceException;
}