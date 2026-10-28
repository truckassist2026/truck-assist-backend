package com.truckassist.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TwoFactorService {

    private final RestClient restClient;
    private final String apiKey;
    private final String templateName;

    public TwoFactorService(
            @Value("${twofactor.api-key}") String apiKey,
            @Value("${twofactor.template-name:OTP1}") String templateName) {

        this.apiKey = apiKey;
        this.templateName = templateName;

        this.restClient = RestClient.builder()
                .baseUrl("https://2factor.in")
                .build();
    }

    // =====================================================
    // SEND OTP
    // =====================================================

    public String sendOtp(String phone) {

        String uri = UriComponentsBuilder
                .fromPath("/API/V1/{apiKey}/SMS/{phone}/AUTOGEN/{templateName}")
                .encode()
                .buildAndExpand(
                        apiKey,
                        phone,
                        templateName
                )
                .toUriString();

        TwoFactorResponse response = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TwoFactorResponse.class);

        if (response == null ||
                !"Success".equalsIgnoreCase(response.Status())) {

            String message = response != null
                    ? response.Details()
                    : "No response from 2Factor";

            throw new IllegalStateException(
                    "Unable to send OTP: " + message
            );
        }

        if (response.Details() == null ||
                response.Details().isBlank()) {

            throw new IllegalStateException(
                    "2Factor did not return an OTP session ID"
            );
        }

        return response.Details();
    }

    // =====================================================
    // VERIFY OTP
    // =====================================================

    public boolean verifyOtp(String sessionId, String otp) {

        String uri = UriComponentsBuilder
                .fromPath(
                        "/API/V1/{apiKey}/SMS/VERIFY/{sessionId}/{otp}"
                )
                .encode()
                .buildAndExpand(
                        apiKey,
                        sessionId,
                        otp
                )
                .toUriString();

        TwoFactorResponse response = restClient
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TwoFactorResponse.class);

        return response != null &&
                "Success".equalsIgnoreCase(response.Status());
    }

    // =====================================================
    // 2FACTOR RESPONSE
    // =====================================================

    private record TwoFactorResponse(
            String Status,
            String Details,
            String StatusCode
    ) {
    }

}
