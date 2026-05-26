package com.solidsign.examples.service;

import com.solidsign.examples.response.PreparedHashesResponse;
import com.solidsign.examples.response.SignResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * [EN]    Service for two-step PAdES (PDF) signing using PKCS#1 (external private key).
 *         The private key never leaves the client device; only the public certificate PEM is sent here.
 *         Visual signature image and field configuration are also supported.
 *         Flow:
 *           1. prepareSignature — sends documents + certificate to SolidSign; receives hashes + finalNonce.
 *           2. finalizeSignature — sends finalNonce + signed hashes; receives download links.
 *
 * [PT-BR] Serviço para assinatura PAdES (PDF) em dois passos com PKCS#1 (chave privada externa).
 *         A chave privada nunca sai do dispositivo do cliente; apenas o PEM do certificado público é enviado aqui.
 *         Imagem de assinatura visual e configuração de campo também são suportados.
 *         Fluxo:
 *           1. prepareSignature — envia documentos + certificado ao SolidSign; recebe hashes + finalNonce.
 *           2. finalizeSignature — envia finalNonce + hashes assinados; recebe links para download.
 *
 * [ES]    Servicio para firma PAdES (PDF) en dos pasos con PKCS#1 (clave privada externa).
 *         La clave privada nunca sale del dispositivo del cliente; solo se envía el PEM del certificado público.
 *         La imagen de firma visual y la configuración de campo también son compatibles.
 *         Flujo:
 *           1. prepareSignature — envía documentos + certificado a SolidSign; recibe hashes + finalNonce.
 *           2. finalizeSignature — envía finalNonce + hashes firmados; recibe enlaces de descarga.
 */
@Service
public class PdfPkcs1Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPkcs1Service.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // [EN]    Base URL of the SolidSign API
    // [PT-BR] URL base da API SolidSign
    // [ES]    URL base de la API SolidSign
    @Value("${solidsign.api.base-url}")
    private String baseUrl;

    // [EN]    Authorization header value (Bearer token)
    // [PT-BR] Valor do header Authorization (token Bearer)
    // [ES]    Valor del header Authorization (token Bearer)
    @Value("${solidsign.api.authorization}")
    private String authorization;

    // [EN]    Signature profile (e.g. ADRB, ADRT, ADRC, ADRA)
    // [PT-BR] Perfil de assinatura (ex: ADRB, ADRT, ADRC, ADRA)
    // [ES]    Perfil de firma (p.ej. ADRB, ADRT, ADRC, ADRA)
    @Value("${solidsign.sig.profile}")
    private String profile;

    // [EN]    Hash algorithm (SHA256, SHA384, SHA512)
    // [PT-BR] Algoritmo de hash (SHA256, SHA384, SHA512)
    // [ES]    Algoritmo de hash (SHA256, SHA384, SHA512)
    @Value("${solidsign.sig.hashAlgorithm}")
    private String hashAlgorithm;

    // [EN]    Measurement unit for visual signature field (PIXELS or CENTIMETERS)
    // [PT-BR] Unidade de medida para o campo de assinatura visual (PIXELS ou CENTIMETERS)
    // [ES]    Unidad de medida para el campo de firma visual (PIXELS o CENTIMETERS)
    @Value("${solidsign.sig.sigFieldMeasurementUnit}")
    private String sigFieldMeasurementUnit;

    // [EN]    JSON array describing the visual signature field(s)
    // [PT-BR] Array JSON descrevendo o(s) campo(s) de assinatura visual
    // [ES]    Array JSON que describe el/los campo(s) de firma visual
    @Value("${solidsign.sig.signatureFieldConfig}")
    private String signatureFieldConfig;

    // [EN]    Comma-separated list of signature image file paths
    // [PT-BR] Lista de caminhos de imagem de assinatura separados por vírgula
    // [ES]    Lista de rutas de imagen de firma separadas por coma
    @Value("${solidsign.sig.signatureImagePaths}")

    private List<String> signatureImagePaths;

    // [EN]    Reason for signing — displayed in the PDF signature properties
    // [PT-BR] Motivo da assinatura — exibido nas propriedades da assinatura PDF
    // [ES]    Motivo de la firma — se muestra en las propiedades de la firma PDF
    @Value("${solidsign.sig.reason}")
    private String reason;

    // [EN]    Signing location — displayed in the PDF signature properties
    // [PT-BR] Local da assinatura — exibido nas propriedades da assinatura PDF
    // [ES]    Lugar de firma — se muestra en las propiedades de la firma PDF
    @Value("${solidsign.sig.location}")
    private String location;

    // [EN]    Contact information of the signer — displayed in the PDF signature properties
    // [PT-BR] Informações de contato do assinante — exibido nas propriedades da assinatura PDF
    // [ES]    Información de contacto del firmante — se muestra en las propiedades de la firma PDF
    @Value("${solidsign.sig.contact}")
    private String contact;

    // [EN]    Optional: name of a pre-existing signature field in the PDF to target
    // [PT-BR] Opcional: nome de um campo de assinatura pré-existente no PDF a utilizar
    // [ES]    Opcional: nombre de un campo de firma preexistente en el PDF
    // @Value("${solidsign.sig.signatureFieldName:}")
    // private String signatureFieldName;

    // [EN]    Optional: text overlays as JSON array (pageNumber, coordinateX, coordinateY, text, fontSize, textColor)
    // [PT-BR] Opcional: sobreposições de texto como array JSON (pageNumber, coordinateX, coordinateY, text, fontSize, textColor)
    // [ES]    Opcional: superposiciones de texto como array JSON (pageNumber, coordinateX, coordinateY, text, fontSize, textColor)
    // @Value("${solidsign.sig.signatureTextConfig:}")
    // private String signatureTextConfig;

    // [EN]    Optional: MDP permission level for certification signature (1=no changes, 2=forms, 3=annotations)
    // [PT-BR] Opcional: nível de permissão MDP para assinatura de certificação (1=sem alterações, 2=formulários, 3=anotações)
    // [ES]    Opcional: nivel de permiso MDP para firma de certificación (1=sin cambios, 2=formularios, 3=anotaciones)
    // @Value("${solidsign.sig.mdpPermissionLevel:}")
    // private String mdpPermissionLevel;

    // [EN]    Optional: JSON array of per-document open passwords for encrypted PDFs (e.g. ["pwd1","pwd2",null])
    // [PT-BR] Opcional: array JSON de senhas por documento para PDFs criptografados (ex: ["senha1","senha2",null])
    // [ES]    Opcional: array JSON de contraseñas por documento para PDFs cifrados (p.ej. ["pwd1","pwd2",null])
    // @Value("${solidsign.sig.passwordsForDecryption:}")
    // private String passwordsForDecryption;

    // [EN]    Optional: JSON map of PDF document metadata (title, author, subject, keywords, creator)
    // [PT-BR] Opcional: mapa JSON de metadados do documento PDF (title, author, subject, keywords, creator)
    // [ES]    Opcional: mapa JSON de metadatos del documento PDF (title, author, subject, keywords, creator)
    // @Value("${solidsign.sig.documentInfoMetadata:}")
    // private String documentInfoMetadata;

    // [EN]    Optional: QR code config as JSON array — CANNOT be used together with signatureFieldConfig
    // [PT-BR] Opcional: configuração de QR code como array JSON — NÃO pode ser usado junto com signatureFieldConfig
    // [ES]    Opcional: configuración de código QR como array JSON — NO puede usarse junto con signatureFieldConfig
    // @Value("${solidsign.sig.signatureQrCodeConfig:}")
    // private String signatureQrCodeConfig;

    // [EN]    PEM body of the signer's public certificate (no BEGIN/END headers, no private key)
    // [PT-BR] Corpo PEM do certificado público do assinante (sem marcadores BEGIN/END, sem chave privada)
    // [ES]    Cuerpo PEM del certificado público del firmante (sin marcadores BEGIN/END, sin clave privada)
    @Value("${solidsign.cert.pem}")
    private String signerCertPem;

    /**
     * [EN]    Sends PDF documents and the signer certificate to SolidSign sign-preparation endpoint.
     *         Returns hashes and finalNonce for the browser extension to sign.
     *
     * [PT-BR] Envia documentos PDF e o certificado do assinante para o endpoint sign-preparation do SolidSign.
     *         Retorna hashes e finalNonce para a extensão do browser assinar.
     *
     * [ES]    Envía documentos PDF y el certificado del firmante al endpoint sign-preparation de SolidSign.
     *         Devuelve hashes y finalNonce para que la extensión del navegador los firme.
     */
    public PreparedHashesResponse prepareSignature(MultipartFile[] documents) throws IOException {
        // [EN]    Build preparation URL from the base URL
        // [PT-BR] Constrói a URL de preparação a partir da URL base
        // [ES]    Construye la URL de preparación a partir de la URL base
        String prepUrl = baseUrl + "/solidsign/dsig/pdf/pkcs1/sign-preparation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < documents.length; i++) {
            final byte[] bytes = documents[i].getBytes();
            final String name  = documents[i].getOriginalFilename();
            body.add("document[" + i + "]", new ByteArrayResource(bytes) {
                @Override public String getFilename() { return name; }
            });
        }

        body.add("profile",                 profile);
        body.add("hashAlgorithm",           hashAlgorithm);
        body.add("sigFieldMeasurementUnit", sigFieldMeasurementUnit);
        body.add("signatureFieldConfig",    signatureFieldConfig);
        body.add("reason",                  reason);
        body.add("location",                location);
        body.add("contact",                 contact);

        // [EN]    Optional parameters — uncomment to use
        // [PT-BR] Parâmetros opcionais — descomente para usar
        // [ES]    Parámetros opcionales — descomente para usar
        // body.add("signatureFieldName",     signatureFieldName);
        // body.add("signatureTextConfig",    signatureTextConfig);
        // body.add("mdpPermissionLevel",     mdpPermissionLevel);
        // body.add("passwordsForDecryption", passwordsForDecryption);
        // body.add("documentInfoMetadata",   documentInfoMetadata);
        // body.add("signatureQrCodeConfig",  signatureQrCodeConfig);

        // [EN]    Signer's certificate PEM body (no headers) — required by SolidSign to embed it in the signature
        // [PT-BR] Corpo PEM do certificado do assinante (sem marcadores) — obrigatório para SolidSign embutir na assinatura
        // [ES]    Cuerpo PEM del certificado del firmante (sin marcadores) — requerido por SolidSign para incluirlo en la firma
        body.add("certificate",             signerCertPem);

        // [EN]    Optional visual signature images (one per document, indexed)
        // [PT-BR] Imagens de assinatura visual opcionais (uma por documento, indexadas)
        // [ES]    Imágenes de firma visual opcionales (una por documento, indexadas)
        if (signatureImagePaths != null) {
            for (int i = 0; i < signatureImagePaths.size(); i++) {
                File imgFile = new File(signatureImagePaths.get(i).trim());
                if (imgFile.exists()) {
                    body.add("signatureImage[" + i + "]", new FileSystemResource(imgFile));
                }
            }
        }

        try {
            ResponseEntity<PreparedHashesResponse> resp = restTemplate.postForEntity(
                    prepUrl, new HttpEntity<>(body, headers), PreparedHashesResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                LOGGER.info("PDF PKCS1 preparation OK. finalNonce={}, hashCount={}",
                        resp.getBody().finalNonce, resp.getBody().hashCount);
                return resp.getBody();
            }
        } catch (HttpStatusCodeException e) {
            LOGGER.error("SolidSign prep error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during PDF preparation: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * [EN]    Sends finalNonce and signature values to SolidSign sign-finalization endpoint.
     *         allParams must contain: finalNonce, signatureValue[0], signatureValue[1], ...
     *
     * [PT-BR] Envia finalNonce e valores de assinatura para o endpoint sign-finalization do SolidSign.
     *         allParams deve conter: finalNonce, signatureValue[0], signatureValue[1], ...
     *
     * [ES]    Envía finalNonce y valores de firma al endpoint sign-finalization de SolidSign.
     *         allParams debe contener: finalNonce, signatureValue[0], signatureValue[1], ...
     */
    public SignResponse finalizeSignature(Map<String, String> allParams) {
        // [EN]    Build finalization URL from the base URL
        // [PT-BR] Constrói a URL de finalização a partir da URL base
        // [ES]    Construye la URL de finalización a partir de la URL base
        String finalUrl = baseUrl + "/solidsign/dsig/pdf/pkcs1/sign-finalization";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authorization);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        allParams.forEach(body::add);

        try {
            ResponseEntity<SignResponse> resp = restTemplate.postForEntity(
                    finalUrl, new HttpEntity<>(body, headers), SignResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                LOGGER.info("PDF PKCS1 finalization OK. identifier={}", resp.getBody().identifier);
                return resp.getBody();
            }
        } catch (HttpStatusCodeException e) {
            LOGGER.error("SolidSign final error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error("Unexpected error during PDF finalization: {}", e.getMessage(), e);
        }
        return null;
    }

    // ─── Form endpoints (all params from request, properties ignored) ─────────

    /**
     * [EN]    Step 1 form variant for PAdES PKCS#1 — all config from caller.
     *         signatureImages may be null/empty if no visual signature is needed.
     * [PT-BR] Variante de formulário do passo 1 para PAdES PKCS#1 — toda config do chamador.
     * [ES]    Variante de formulario del paso 1 para PAdES PKCS#1 — toda config del llamador.
     */
    public PreparedHashesResponse prepareForm(Map<String, String> params,
                                              MultipartFile[] documents,
                                              MultipartFile[] signatureImages) throws IOException {
        String auth     = params.getOrDefault("authorization", "");
        String apiBase  = params.getOrDefault("baseUrl", "");
        String profile  = params.get("profile");
        String hashAlg  = params.get("hashAlgorithm");
        String policy   = params.get("policyVersion");
        String sfMU     = params.get("sigFieldMeasurementUnit");
        String sfConfig = params.get("signatureFieldConfig");
        String reason   = params.get("reason");
        String location = params.get("location");
        String contact  = params.get("contact");
        String sfName   = params.get("signatureFieldName");
        String stConfig = params.get("signatureTextConfig");
        String mdp      = params.get("mdpPermissionLevel");
        String pwdDec   = params.get("passwordsForDecryption");
        String docMeta  = params.get("documentInfoMetadata");
        String qrCode   = params.get("signatureQrCodeConfig");
        String cert     = params.get("certificate");

        String prepUrl = apiBase + "/solidsign/dsig/pdf/pkcs1/sign-preparation";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", auth);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < documents.length; i++) {
            final byte[] bytes = documents[i].getBytes();
            final String name  = documents[i].getOriginalFilename();
            body.add("document[" + i + "]", new ByteArrayResource(bytes) {
                @Override public String getFilename() { return name; }
            });
        }
        if (signatureImages != null) {
            for (int i = 0; i < signatureImages.length; i++) {
                final byte[] imgBytes = signatureImages[i].getBytes();
                final String imgName  = signatureImages[i].getOriginalFilename();
                body.add("signatureImage[" + i + "]", new ByteArrayResource(imgBytes) {
                    @Override public String getFilename() { return imgName; }
                });
            }
        }
        if (profile  != null && !profile.isBlank())  body.add("profile",                 profile);
        if (hashAlg  != null && !hashAlg.isBlank())  body.add("hashAlgorithm",           hashAlg);
        if (policy   != null && !policy.isBlank())   body.add("policyVersion",           policy);
        if (sfMU     != null && !sfMU.isBlank())     body.add("sigFieldMeasurementUnit", sfMU);
        if (sfConfig != null && !sfConfig.isBlank()) body.add("signatureFieldConfig",    sfConfig);
        if (reason   != null && !reason.isBlank())   body.add("reason",                  reason);
        if (location != null && !location.isBlank()) body.add("location",                location);
        if (contact  != null && !contact.isBlank())  body.add("contact",                 contact);
        if (sfName   != null && !sfName.isBlank())   body.add("signatureFieldName",      sfName);
        if (stConfig != null && !stConfig.isBlank()) body.add("signatureTextConfig",     stConfig);
        if (mdp      != null && !mdp.isBlank())      body.add("mdpPermissionLevel",      mdp);
        if (pwdDec   != null && !pwdDec.isBlank())   body.add("passwordsForDecryption",  pwdDec);
        if (docMeta  != null && !docMeta.isBlank())  body.add("documentInfoMetadata",    docMeta);
        if (qrCode   != null && !qrCode.isBlank())   body.add("signatureQrCodeConfig",   qrCode);
        if (cert     != null && !cert.isBlank())     body.add("certificate",             cert);
        try {
            ResponseEntity<PreparedHashesResponse> resp = restTemplate.postForEntity(
                    prepUrl, new HttpEntity<>(body, headers), PreparedHashesResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                LOGGER.info("PDF PKCS1 form preparation OK. finalNonce={}", resp.getBody().finalNonce);
                return resp.getBody();
            }
        } catch (HttpStatusCodeException e) {
            LOGGER.error("SolidSign prep form error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in PDF PKCS1 form preparation: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * [EN]    Step 2 form variant for PAdES PKCS#1 — auth and baseUrl from allParams map.
     * [PT-BR] Variante de formulário do passo 2 para PAdES PKCS#1 — auth e baseUrl do map.
     * [ES]    Variante de formulario del paso 2 para PAdES PKCS#1 — auth y baseUrl del map.
     */
    public SignResponse finalizeForm(Map<String, String> allParams) {
        String auth       = allParams.remove("authorization");
        String apiBaseUrl = allParams.remove("baseUrl");
        String finalUrl   = apiBaseUrl + "/solidsign/dsig/pdf/pkcs1/sign-finalization";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", auth);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        allParams.forEach(body::add);
        try {
            ResponseEntity<SignResponse> resp = restTemplate.postForEntity(
                    finalUrl, new HttpEntity<>(body, headers), SignResponse.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                LOGGER.info("PDF PKCS1 form finalization OK.");
                return resp.getBody();
            }
        } catch (HttpStatusCodeException e) {
            LOGGER.error("SolidSign final form error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            LOGGER.error("Unexpected error in PDF PKCS1 form finalization: {}", e.getMessage(), e);
        }
        return null;
    }

}
