# 🇧🇷 SolidSign API - Exemplo de Assinatura PDF com Certificado no Navegador (PKCS#1)

Este projeto demonstra a integração com a **SolidSign API** para realizar a assinatura digital PAdES em **dois passos** utilizando a **Solid Web Extension** com token A3 (eToken) ou certificado no repositório do sistema operacional. A chave privada **nunca sai do dispositivo** do assinante.

## Estrutura do Projeto

* **Controller:** Expõe dois endpoints REST para o fluxo de assinatura em dois passos. **Passo 1:** recebe o(s) documento(s) e solicita à SolidSign API o cálculo dos hashes a serem assinados. **Passo 2:** recebe os valores de assinatura gerados pela Solid Web Extension e finaliza a assinatura PAdES.
* **Service:** Orquestra as chamadas de preparação e finalização para a SolidSign API, tratando erros em ambas as etapas.

## Configuração (application.properties)

| Atributo | Descrição | Exemplo / Valor |
| :--- | :--- | :--- |
| `solidsign.api.base-url` | URL base da SolidSign API (sem o caminho). | `https://solidsign.com.br` |
| `solidsign.api.authorization` | Token JWT de autorização (Bearer). | `Bearer eyJhbGciOiJIUzI1...` |
| `solidsign.cert.pem` | Certificado público do assinante em formato PEM (Base64, **sem** os marcadores `BEGIN/END CERTIFICATE`). A chave privada permanece no dispositivo (eToken A3). | `MIICpDCCAYwCCQD...` |
| `solidsign.sig.hashAlgorithm` | Algoritmo de hash criptográfico (SHA256, SHA384, SHA512). | `SHA256` |
| `solidsign.sig.profile` | Perfil/padrão da assinatura PAdES (Adobe/ICP-Brasil/ETSI). | `PDF_BASIC`, `ADRT`, `PADES_B`, etc. |
| `solidsign.sig.sigFieldMeasurementUnit` | Unidade de medida para as coordenadas do campo de assinatura. | `PIXELS` ou `MILLIMETERS` |
| `solidsign.sig.signatureFieldConfig` | JSON com coordenadas e dimensões do campo de assinatura visual. | `[{"pageNumber":1,"coordinateX":200,"coordinateY":380,"width":200,"height":80}]` |
| `solidsign.sig.signatureImagePaths` | Caminho(s) para a(s) imagem(ns) de carimbo de assinatura. | `C:/img/stamp.png` |
| `solidsign.sig.reason` | Motivo da assinatura (visível no painel de assinaturas do PDF). | `Assinatura digital` |
| `solidsign.sig.location` | Local geográfico onde o documento foi assinado. | `Brasil` |
| `solidsign.sig.contact` | Informações de contato do assinante. | `contato@empresa.com.br` |
| `solidsign.sig.signatureFieldName` *(opcional)* | Nome de um campo de assinatura pré-existente no PDF. | `SignatureField1` |
| `solidsign.sig.signatureTextConfig` *(opcional)* | JSON com texto personalizado exibido no campo de assinatura visual. | `[{"pageNumber":1,"coordinateX":200,"coordinateY":460,"text":"Assinado","fontSize":10,"textColor":"BLACK"}]` |
| `solidsign.sig.mdpPermissionLevel` *(opcional)* | Nível MDP para assinaturas de certificação (1=sem alterações, 2=formulários, 3=anotações). | `1` |
| `solidsign.sig.passwordsForDecryption` *(opcional)* | Array JSON de senhas para abrir PDFs criptografados. | `["senha1"]` |
| `solidsign.sig.documentInfoMetadata` *(opcional)* | JSON com metadados do documento PDF (título, autor, assunto, palavras-chave). | `{"title":"Contrato","author":"Empresa"}` |
| `solidsign.sig.signatureQrCodeConfig` *(opcional)* | JSON com configurações do QR Code a ser embutido no campo de assinatura visual. | `{"pageNumber":1,"coordinateX":10,"coordinateY":10,"width":60,"height":60}` |
| `solidsign.sig.policyVersion` *(opcional)* | OID da política de assinatura ICP-Brasil. | `2.16.76.1.7.1.11.1.3` |

## Stack
1. Java 17
2. SpringBoot 3.4.x+
3. Maven 3.x.x+
4. Logback (para logging dos erros)

## Como Executar

1. **Configurar:** Defina `solidsign.cert.pem` com o corpo Base64 do certificado público e os demais parâmetros em `src/main/resources/application.properties`.
2. **Compilar:** `mvn clean install`
3. **Iniciar:** `mvn spring-boot:run`

### Passo 1 — Preparação

Envie um POST para `http://localhost:8080/api/pdf/prepare` com o(s) arquivo(s) como parâmetro multipart `document`.

A resposta conterá o `finalNonce` e os hashes que a **Solid Web Extension** irá assinar com a chave privada do token A3. Nenhuma chave privada é transmitida nesta etapa.

### Passo 2 — Finalização

Após a Solid Web Extension assinar os hashes, envie um POST para `http://localhost:8080/api/pdf/finalize` incluindo:
- `finalNonce` — identificador da sessão de assinatura retornado no Passo 1.
- `signatureValues[0]`, `signatureValues[1]`, etc. — valores de assinatura gerados para cada documento.

O sistema retornará os links de download dos PDFs assinados.

## Tratamento de Erros
O sistema intercepta erros **400 Bad Request** em ambas as etapas e loga o JSON detalhado da SolidSign para facilitar o debug.

---

# 🇬🇧 SolidSign API - PDF Browser-based Signature Example (PKCS#1)

This project demonstrates the integration with the **SolidSign API** to perform PAdES digital signatures in a **two-step flow** using the **Solid Web Extension** with an A3 token (eToken) or OS certificate store. The private key **never leaves the signer's device**.

## Project Structure

* **Controller:** Exposes two REST endpoints for the two-step signing flow. **Step 1:** receives document(s) and requests SolidSign API to compute the hashes to be signed. **Step 2:** receives the signature values generated by the Solid Web Extension and finalizes the PAdES signature.
* **Service:** Orchestrates the preparation and finalization calls to the SolidSign API, handling errors in both steps.

## Configuration (application.properties)

| Attribute | Description | Example / Value |
| :--- | :--- | :--- |
| `solidsign.api.base-url` | Base URL of the SolidSign API (without path). | `https://solidsign.com.br` |
| `solidsign.api.authorization` | Authorization JWT Token (Bearer). | `Bearer eyJhbGciOiJIUzI1...` |
| `solidsign.cert.pem` | Signer's public certificate in PEM format (Base64, **without** `BEGIN/END CERTIFICATE` markers). The private key stays on the device (A3 eToken). | `MIICpDCCAYwCCQD...` |
| `solidsign.sig.hashAlgorithm` | Cryptographic hash algorithm (SHA256, SHA384, SHA512). | `SHA256` |
| `solidsign.sig.profile` | PAdES signature profile/standard (Adobe/ICP-Brasil/ETSI). | `PDF_BASIC`, `ADRT`, `PADES_B`, etc. |
| `solidsign.sig.sigFieldMeasurementUnit` | Measurement unit for signature field coordinates. | `PIXELS` or `MILLIMETERS` |
| `solidsign.sig.signatureFieldConfig` | JSON with visual signature field coordinates and dimensions. | `[{"pageNumber":1,"coordinateX":200,"coordinateY":380,"width":200,"height":80}]` |
| `solidsign.sig.signatureImagePaths` | Path(s) to the signature stamp image(s). | `C:/img/stamp.png` |
| `solidsign.sig.reason` | Reason for the signature (visible in the PDF signature panel). | `Digital signature` |
| `solidsign.sig.location` | Geographic location where the document was signed. | `Brazil` |
| `solidsign.sig.contact` | Signer contact information. | `contact@company.com` |
| `solidsign.sig.signatureFieldName` *(optional)* | Name of a pre-existing signature field in the PDF. | `SignatureField1` |
| `solidsign.sig.signatureTextConfig` *(optional)* | JSON with custom text displayed in the visual signature field. | `[{"pageNumber":1,"coordinateX":200,"coordinateY":460,"text":"Signed","fontSize":10,"textColor":"BLACK"}]` |
| `solidsign.sig.mdpPermissionLevel` *(optional)* | MDP level for certification signatures (1=no changes, 2=forms, 3=annotations). | `1` |
| `solidsign.sig.passwordsForDecryption` *(optional)* | JSON array of passwords to open encrypted PDFs. | `["password1"]` |
| `solidsign.sig.documentInfoMetadata` *(optional)* | JSON with PDF document metadata (title, author, subject, keywords). | `{"title":"Contract","author":"Company"}` |
| `solidsign.sig.signatureQrCodeConfig` *(optional)* | JSON with QR Code configuration to embed in the visual signature field. | `{"pageNumber":1,"coordinateX":10,"coordinateY":10,"width":60,"height":60}` |
| `solidsign.sig.policyVersion` *(optional)* | ICP-Brasil signature policy OID. | `2.16.76.1.7.1.11.1.3` |

## Stack
1. Java 17
2. SpringBoot 3.4.x+
3. Maven 3.x.x+
4. Logback (for error logging)

## How to Run

1. **Configure:** Set `solidsign.cert.pem` with the Base64 body of the public certificate and configure the remaining parameters in `src/main/resources/application.properties`.
2. **Build:** `mvn clean install`
3. **Start:** `mvn spring-boot:run`

### Step 1 — Preparation

Send a POST to `http://localhost:8080/api/pdf/prepare` with the file(s) as multipart parameter `document`.

The response will contain the `finalNonce` and the hashes that the **Solid Web Extension** will sign using the A3 token private key. No private key is transmitted in this step.

### Step 2 — Finalization

After the Solid Web Extension signs the hashes, send a POST to `http://localhost:8080/api/pdf/finalize` including:
- `finalNonce` — signing session identifier returned in Step 1.
- `signatureValues[0]`, `signatureValues[1]`, etc. — signature values generated for each document.

The system will return download links for the signed PDFs.

## Error Handling
The system intercepts **400 Bad Request** errors in both steps and logs the detailed JSON response from SolidSign to assist in debugging.

---

# 🇪🇸 SolidSign API - Ejemplo de Firma PDF en el Navegador (PKCS#1)

Este proyecto demuestra la integración con la **SolidSign API** para realizar la firma digital PAdES en **dos pasos** usando la **Solid Web Extension** con token A3 (eToken) o certificado en el almacén del sistema operativo. La clave privada **nunca sale del dispositivo** del firmante.

## Estructura del Proyecto

* **Controller:** Expone dos endpoints REST para el flujo de firma en dos pasos. **Paso 1:** recibe el/los documento(s) y solicita a la API SolidSign el cálculo de los hashes a firmar. **Paso 2:** recibe los valores de firma generados por la Solid Web Extension y finaliza la firma PAdES.
* **Service:** Orquestra las llamadas de preparación y finalización a la API SolidSign, gestionando errores en ambos pasos.

## Configuración (application.properties)

| Atributo | Descripción | Ejemplo / Valor |
| :--- | :--- | :--- |
| `solidsign.api.base-url` | URL base de la SolidSign API (sin la ruta). | `https://solidsign.com.br` |
| `solidsign.api.authorization` | Token JWT de autorización (Bearer). | `Bearer eyJhbGciOiJIUzI1...` |
| `solidsign.cert.pem` | Certificado público del firmante en formato PEM (Base64, **sin** los marcadores `BEGIN/END CERTIFICATE`). La clave privada permanece en el dispositivo (eToken A3). | `MIICpDCCAYwCCQD...` |
| `solidsign.sig.hashAlgorithm` | Algoritmo de hash criptográfico (SHA256, SHA384, SHA512). | `SHA256` |
| `solidsign.sig.profile` | Perfil/estándar de firma PAdES (Adobe/ICP-Brasil/ETSI). | `PDF_BASIC`, `ADRT`, `PADES_B`, etc. |
| `solidsign.sig.sigFieldMeasurementUnit` | Unidad de medida para las coordenadas del campo de firma. | `PIXELS` o `MILLIMETERS` |
| `solidsign.sig.signatureFieldConfig` | JSON con coordenadas y dimensiones del campo de firma visual. | `[{"pageNumber":1,"coordinateX":200,"coordinateY":380,"width":200,"height":80}]` |
| `solidsign.sig.signatureImagePaths` | Ruta(s) a la(s) imagen(es) de sello de firma. | `C:/img/sello.png` |
| `solidsign.sig.reason` | Motivo de la firma (visible en el panel de firmas del PDF). | `Firma digital` |
| `solidsign.sig.location` | Ubicación geográfica donde se firmó el documento. | `Brasil` |
| `solidsign.sig.contact` | Información de contacto del firmante. | `contacto@empresa.com` |
| `solidsign.sig.signatureFieldName` *(opcional)* | Nombre de un campo de firma preexistente en el PDF. | `SignatureField1` |
| `solidsign.sig.signatureTextConfig` *(opcional)* | JSON con texto personalizado en el campo de firma visual. | `[{"pageNumber":1,"coordinateX":200,"coordinateY":460,"text":"Firmado","fontSize":10,"textColor":"BLACK"}]` |
| `solidsign.sig.mdpPermissionLevel` *(opcional)* | Nivel MDP para firmas de certificación (1=sin cambios, 2=formularios, 3=anotaciones). | `1` |
| `solidsign.sig.passwordsForDecryption` *(opcional)* | Array JSON de contraseñas para abrir PDFs cifrados. | `["contraseña1"]` |
| `solidsign.sig.documentInfoMetadata` *(opcional)* | JSON con metadatos del documento PDF (título, autor, asunto, palabras clave). | `{"title":"Contrato","author":"Empresa"}` |
| `solidsign.sig.signatureQrCodeConfig` *(opcional)* | JSON con configuración del código QR a incrustar en el campo de firma visual. | `{"pageNumber":1,"coordinateX":10,"coordinateY":10,"width":60,"height":60}` |
| `solidsign.sig.policyVersion` *(opcional)* | OID de la política de firma ICP-Brasil. | `2.16.76.1.7.1.11.1.3` |

## Stack
1. Java 17
2. SpringBoot 3.4.x+
3. Maven 3.x.x+
4. Logback (para el registro de errores)

## Cómo Ejecutar

1. **Configurar:** Defina `solidsign.cert.pem` con el cuerpo Base64 del certificado público y configure los demás parámetros en `src/main/resources/application.properties`.
2. **Compilar:** `mvn clean install`
3. **Iniciar:** `mvn spring-boot:run`

### Paso 1 — Preparación

Envíe un POST a `http://localhost:8080/api/pdf/prepare` con el/los archivo(s) como parámetro multipart `document`.

La respuesta contendrá el `finalNonce` y los hashes que la **Solid Web Extension** firmará con la clave privada del token A3. Ninguna clave privada se transmite en este paso.

### Paso 2 — Finalización

Tras la firma por la Solid Web Extension, envíe un POST a `http://localhost:8080/api/pdf/finalize` incluyendo:
- `finalNonce` — identificador de la sesión de firma devuelto en el Paso 1.
- `signatureValues[0]`, `signatureValues[1]`, etc. — valores de firma generados para cada documento.

El sistema devolverá los enlaces de descarga de los PDFs firmados.

## Gestión de Errores
El sistema intercepta errores **400 Bad Request** en ambos pasos y registra el JSON detallado de SolidSign para facilitar la depuración.
