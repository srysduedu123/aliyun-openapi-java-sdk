package com.aliyuncs;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.aliyuncs.auth.Credential;
import com.aliyuncs.auth.LegacyCredentials;
import com.aliyuncs.ecs.v20140526.model.DescribeRegionsResponse;
import com.aliyuncs.endpoint.DefaultEndpointResolver;
import com.aliyuncs.endpoint.ResolveEndpointRequest;
import com.aliyuncs.endpoint.location.model.v20150612.DescribeEndpointsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ErrorCodeConstant;
import com.aliyuncs.exceptions.ErrorMessageConstant;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpClientConfig;
import com.aliyuncs.http.HttpClientFactory;
import com.aliyuncs.http.HttpRequest;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.IHttpClient;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.http.clients.CompatibleUrlConnClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.regions.ProductDomain;
import com.aliyuncs.transform.UnmarshallerContext;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest(HttpClientFactory.class)
public class DefaultAcsClientTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor() throws ClientException, IOException {
        DefaultAcsClient client = new DefaultAcsClient();
    }

    @Test
    public void testConstructorWithProfile() throws ClientException, IOException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
    }

    @Test
    public void testConstructorWithProfileAndCredentials() throws ClientException, IOException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        LegacyCredentials legacyCredentials = new LegacyCredentials(new Credential());
        DefaultAcsClient client = new DefaultAcsClient(profile, legacyCredentials);
    }

    @Test
    public void testIsAutoRetry() {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        Assert.assertTrue(client.isAutoRetry());
        client.setAutoRetry(false);
        Assert.assertFalse(client.isAutoRetry());
    }

    @Test
    public void testMaxRetryNumber() {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        Assert.assertTrue(3 == client.getMaxRetryNumber());
        client.setMaxRetryNumber(1);
        Assert.assertTrue(1 == client.getMaxRetryNumber());
    }

    @Test
    public void testShutdown() {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        client.shutdown();
    }

    @Test
    public void testGetProfile() {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        Assert.assertTrue(client.getProfile() instanceof DefaultProfile);
    }

    private DefaultAcsClient initDefaultAcsClient()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        Field httpClient = client.getClass().getDeclaredField("httpClient");
        httpClient.setAccessible(true);
        CompatibleUrlConnClient compatibleUrlConnClient = Mockito.mock(CompatibleUrlConnClient.class);
        httpClient.set(client, compatibleUrlConnClient);
        return client;
    }

    private IHttpClient getHttpClient(DefaultAcsClient client)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field httpClient = client.getClass().getDeclaredField("httpClient");
        httpClient.setAccessible(true);
        return (IHttpClient) httpClient.get(client);
    }

    private AcsRequest initRequest(Class<? extends AcsResponse> responseClass) {
        AcsRequest request = Mockito.mock(AcsRequest.class);
        Mockito.when(request.getSysProduct()).thenReturn("product");
        Mockito.when(request.getSysLocationProduct()).thenReturn("locationProduct");
        Mockito.when(request.getSysEndpointType()).thenReturn("endpointType");
        Mockito.doReturn(ProtocolType.HTTP).when(request).getSysProtocol();
        Mockito.when(request.getResponseClass()).thenReturn(responseClass);
        return request;
    }

    private HttpResponse initHttpResponse() throws ClientException {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.isSuccess()).thenReturn(true);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        String httpContentString = "<?xml version=\"1.0\" encoding=\"utf-8\"?><DescribeRegionsResponse>"
                + "<RequestId>RequestId</RequestId>" + "<Regions>" + "<Region>" + "<RegionId>RegionId</RegionId>"
                + "<LocalName>LocalName</LocalName>" + "</Region>" + "<Region>" + "<RegionId>RegionId</RegionId>"
                + "<LocalName>LocalName</LocalName>" + "</Region>" + "</Regions>" + "</DescribeRegionsResponse>";
        Mockito.when(response.getHttpContentString()).thenReturn(httpContentString);
        return response;
    }

    @Test
    public void testRestoreSSLCertificate()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        DefaultAcsClient client = initDefaultAcsClient();
        client.restoreSSLCertificate();
        client.ignoreSSLCertificate();
        Mockito.verify(getHttpClient(client), Mockito.times(1)).ignoreSSLCertificate();
        Mockito.verify(getHttpClient(client), Mockito.times(1)).restoreSSLCertificate();
    }

    @Test
    public void testGetAcsResponse() throws NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException, ClientException, IOException {
        DefaultAcsClient client = initDefaultAcsClient();
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        HttpResponse response = initHttpResponse();
        Mockito.doReturn(response).when(getHttpClient(client)).syncInvoke(Mockito.any(HttpRequest.class));
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);

        Assert.assertTrue(client.getAcsResponse(request) instanceof DescribeEndpointsResponse);
        Mockito.when(request.getResponseClass()).thenReturn(DescribeRegionsResponse.class);
        Assert.assertTrue(client.getAcsResponse(request) instanceof DescribeRegionsResponse);

        Assert.assertTrue(client.getAcsResponse(request, true, 3) instanceof DescribeRegionsResponse);
        Assert.assertTrue(client.getAcsResponse(request, client.getProfile()) instanceof DescribeRegionsResponse);
        Assert.assertTrue(client.getAcsResponse(request, "regionId",
                client.getProfile().getCredential()) instanceof DescribeRegionsResponse);
        Assert.assertTrue(client.getAcsResponse(request, "regionId") instanceof DescribeRegionsResponse);

        Mockito.when(request.getSysRegionId()).thenReturn("regionId");
        Assert.assertTrue(client.getAcsResponse(request, "regionId") instanceof DescribeRegionsResponse);
    }

    @Test
    public void testDoActionBranch() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultAcsClient client = initDefaultAcsClient();
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(response).when(getHttpClient(client)).syncInvoke(Mockito.any(HttpRequest.class));
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);
        Mockito.when(request.getSysAcceptFormat()).thenReturn(FormatType.JSON);
        Mockito.when(request.getSysProductDomain()).thenReturn(new ProductDomain("productName", "domianName"));
        Mockito.when(request.getSysProtocol()).thenReturn(null);

        Assert.assertTrue(client.doAction(request) instanceof HttpResponse);
    }

    @Test
    public void testGetAcsResponseNoProfile() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultAcsClient client = initDefaultAcsClient();
        Field clientProfile = client.getClass().getDeclaredField("clientProfile");
        clientProfile.setAccessible(true);
        clientProfile.set(client, null);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.InvalidProfile : No active profile found.");
        client.getAcsResponse(request);
    }

    @Test
    public void testReadResponseUnmarshaller() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultProfile spyProfile = Mockito.spy(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        Field field = client.getClass().getDeclaredField("clientProfile");
        field.setAccessible(true);
        field.set(spyClient, spyProfile);
        HttpClientConfig httpClientConfig = Mockito.mock(HttpClientConfig.class);
        Mockito.when(spyProfile.getHttpClientConfig()).thenReturn(httpClientConfig);
        AcsRequest request = initRequest(DescribeRegionsResponse.class);
        HttpResponse response = initHttpResponse();
        Mockito.doReturn(response).when(spyClient).doAction(request);

        Mockito.when(httpClientConfig.isCompatibleMode()).thenReturn(false);
        Assert.assertTrue(spyClient.getAcsResponse(request) instanceof DescribeRegionsResponse);

        Mockito.when(httpClientConfig.isCompatibleMode()).thenReturn(true);
        Assert.assertTrue(spyClient.getAcsResponse(request) instanceof DescribeRegionsResponse);

        Mockito.when(request.getResponseClass()).thenReturn(DescribeEndpointsResponse.class);
        Assert.assertTrue(spyClient.getAcsResponse(request) instanceof DescribeEndpointsResponse);

        Mockito.when(httpClientConfig.isCompatibleMode()).thenReturn(false);
        Assert.assertTrue(spyClient.getAcsResponse(request) instanceof DescribeEndpointsResponse);
    }

    @Test
    public void testReadResponseClientException() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = initHttpResponse();
        AcsRequest request = initRequest(DescribeRegionsResponse.class);
        Mockito.doReturn(String.class).when(request).getResponseClass();
        Mockito.doReturn(response).when(spyClient).doAction(request);

        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.InvalidResponseClass : Unable to allocate " + String.class.getName() + " class");
        spyClient.getAcsResponse(request);
    }

    static class TestResponse extends CommonResponse {
        @Override
        public boolean checkShowJsonItemName() {
            return false;
        }
    }

    @Test
    public void testReadResponseForHideArrayItem() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = initHttpResponse();
        AcsRequest request = initRequest(TestResponse.class);
        Mockito.doReturn(response).when(spyClient).doAction(request);

        Assert.assertTrue(spyClient.getAcsResponse(request) instanceof TestResponse);
    }

    @Test
    public void testGetAcsResponseInvalidServerResponseException() throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException, ClientException, IOException {
        DefaultAcsClient client = initDefaultAcsClient();

        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.isSuccess()).thenReturn(true);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        Mockito.when(response.getHttpContentString()).thenReturn(null);
        Mockito.doReturn(response).when(getHttpClient(client)).syncInvoke(Mockito.any(HttpRequest.class));
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);

        thrown.expect(ClientException.class);
        thrown.expectMessage(ErrorCodeConstant.SDK_INVALID_SERVER_RESPONSE + " : "
                + ErrorMessageConstant.SERVER_RESPONSE_HTTP_BODY_EMPTY);
        client.getAcsResponse(request);
    }

    @Test
    public void testDoActionEndpointTestabilityException() throws ClientException, IOException,
            IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        String endPoint = "endpoint-test.exception.com";
        Mockito.doReturn(endPoint).when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);
        thrown.expect(ClientException.class);
        thrown.expectMessage(ErrorCodeConstant.SDK_ENDPOINT_TESTABILITY + " : " + endPoint);
        client.doAction(request);
    }

    @Test
    public void testDoActionSocketTimeoutException() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultAcsClient client = initDefaultAcsClient();
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        Mockito.doThrow(new SocketTimeoutException()).when(getHttpClient(client))
                .syncInvoke(Mockito.any(HttpRequest.class));
        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.ServerUnreachable : SocketTimeoutException has occurred on a socket read or accept.");
        client.doAction(request);
    }

    @Test
    public void testDoActionIOException() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultAcsClient client = initDefaultAcsClient();
        HttpResponse response = Mockito.mock(HttpResponse.class);
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        Mockito.doReturn(ProtocolType.HTTP).when(request).getSysProtocol();
        Mockito.doThrow(new IOException()).when(getHttpClient(client)).syncInvoke(Mockito.any(HttpRequest.class));
        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.ServerUnreachable : Server unreachable: " + new IOException().toString());
        client.doAction(request);
    }

    @Test
    public void testDoActionInvalidKeyException() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultAcsClient client = initDefaultAcsClient();
        HttpResponse response = Mockito.mock(HttpResponse.class);
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        Mockito.doThrow(InvalidKeyException.class).when(getHttpClient(client))
                .syncInvoke(Mockito.any(HttpRequest.class));
        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.InvalidAccessSecret : Specified access secret is not valid.");
        client.doAction(request);
    }

    @Test
    public void testDoActionNoSuchAlgorithmException() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultAcsClient client = initDefaultAcsClient();
        HttpResponse response = Mockito.mock(HttpResponse.class);
        AcsRequest request = initRequest(DescribeEndpointsResponse.class);
        DefaultEndpointResolver endpointResolver = Mockito.mock(DefaultEndpointResolver.class);
        client.setEndpointResolver(endpointResolver);
        Mockito.doReturn("endpoint").when(endpointResolver).resolve(Mockito.any(ResolveEndpointRequest.class));
        Mockito.doThrow(NoSuchAlgorithmException.class).when(getHttpClient(client))
                .syncInvoke(Mockito.any(HttpRequest.class));
        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.InvalidMD5Algorithm : MD5 hash is not supported by client side.");
        client.doAction(request);
    }

    @Test
    public void testGetCommonResponse() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        CommonRequest commonRequest = Mockito.mock(CommonRequest.class);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        Mockito.when(commonRequest.buildRequest()).thenReturn(request);
        Mockito.doReturn(response).when(spyClient).doAction(request);
        Mockito.when(response.isSuccess()).thenReturn(true);
        Assert.assertTrue(spyClient.getCommonResponse(commonRequest) instanceof CommonResponse);
    }

    @Test
    public void testGetCommonResponseServerException() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        CommonRequest commonRequest = Mockito.mock(CommonRequest.class);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        Mockito.when(commonRequest.buildRequest()).thenReturn(request);
        Mockito.doReturn(response).when(spyClient).doAction(request);
        Mockito.when(response.isSuccess()).thenReturn(false);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        Mockito.when(response.getHttpContentString()).thenReturn(makeAcsErrorXML("", "", "500", "ServerException", ""));
        Mockito.when(response.getStatus()).thenReturn(500);
        thrown.expect(ServerException.class);
        spyClient.getCommonResponse(commonRequest);
    }

    @Test
    public void testGetCommonResponseClientException() throws ClientException, IOException, IllegalArgumentException,
            IllegalAccessException, NoSuchFieldException, SecurityException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        CommonRequest commonRequest = Mockito.mock(CommonRequest.class);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        Mockito.when(commonRequest.buildRequest()).thenReturn(request);
        Mockito.doReturn(response).when(spyClient).doAction(request);

        Mockito.when(response.isSuccess()).thenReturn(false);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        Mockito.when(response.getHttpContentString()).thenReturn(makeAcsErrorXML("", "", "500", "ServerException", ""));
        Mockito.when(response.getStatus()).thenReturn(401);
        thrown.expect(ClientException.class);
        spyClient.getCommonResponse(commonRequest);
    }

    private String makeAcsErrorXML(String requestId, String hostId, String code, String message, String recommend) {
        return String.format(
                "<Error><RequestId>%s</RequestId><HostId>%s</HostId><Code>%s</Code>"
                        + "<Message><![CDATA[%s]]></Message><Recommend><![CDATA[%s]]></Recommend></Error>",
                requestId, hostId, code, message, recommend);
    }

    @Test
    public void testResponseServerExceptionError() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getStatus()).thenReturn(500);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        Mockito.when(response.getHttpContentString()).thenReturn(makeAcsErrorXML("", "", "500", "ServerException", ""));
        AcsRequest request = Mockito.mock(AcsRequest.class);
        Mockito.doReturn(response).when(spyClient).doAction(request);
        thrown.expect(ServerException.class);
        thrown.expectMessage("500 : ServerException");
        spyClient.getAcsResponse(request);
    }

    @Test
    public void testResponseNotIncompleteSignatureError() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getStatus()).thenReturn(401);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        Mockito.when(response.getHttpContentString())
                .thenReturn(makeAcsErrorXML("", "", "Not IncompleteSignature", "ClientException", ""));
        AcsRequest request = Mockito.mock(AcsRequest.class);
        Mockito.doReturn(response).when(spyClient).doAction(request);
        thrown.expect(ClientException.class);
        thrown.expectMessage("Not IncompleteSignature : ClientException");
        spyClient.getAcsResponse(request);
    }

    @Test
    public void testResponseInvalidAccessKeySecretError() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getStatus()).thenReturn(401);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        request.strToSign = "GET&%2F%3DDescriat%3DXML%26";
        String errorMessage = "signature does not conform to standards. server string to sign is:" + request.strToSign;
        Mockito.when(response.getHttpContentString())
                .thenReturn(makeAcsErrorXML("", "", "IncompleteSignature", errorMessage, ""));
        Mockito.doReturn(response).when(spyClient).doAction(request);
        thrown.expect(ClientException.class);
        thrown.expectMessage("SDK.InvalidAccessKeySecret : Specified Access Key Secret is not valid.");
        spyClient.getAcsResponse(request);
    }

    @Test
    public void testResponseSignatureNullError() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        DefaultAcsClient spyClient = Mockito.spy(client);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getStatus()).thenReturn(401);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        request.strToSign = "GET&%2F&scribeInstancesDXML%26";
        String errorMessage = "signature does not conform to standards. server sgn is:" + request.strToSign;
        Mockito.when(response.getHttpContentString())
                .thenReturn(makeAcsErrorXML("", "", "IncompleteSignature", errorMessage, ""));
        Mockito.doReturn(response).when(spyClient).doAction(request);
        thrown.expect(ClientException.class);
        thrown.expectMessage("IncompleteSignature : " + errorMessage);
        spyClient.getAcsResponse(request);
    }

    @Test
    public void testResponseSignatureError() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        DefaultAcsClient spyClient = Mockito.spy(client);
        Mockito.when(response.getStatus()).thenReturn(401);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        request.strToSign = "GET&%2F&Action%3DDescribeInances%26Format%3DXML%26";
        String errorMessage = "signature does not conform to standards. server string to sign is:Error Signature";
        Mockito.when(response.getHttpContentString())
                .thenReturn(makeAcsErrorXML("", "", "IncompleteSignature", errorMessage, ""));
        Mockito.doReturn(response).when(spyClient).doAction(request);
        thrown.expect(ClientException.class);
        thrown.expectMessage("IncompleteSignature : " + errorMessage);
        spyClient.getAcsResponse(request);
    }

    @Test
    public void testResponseStringContentNull() throws ClientException {
        DefaultProfile profile = DefaultProfile.getProfile("regionId", "accessKeyId", "secret");
        DefaultAcsClient client = new DefaultAcsClient(profile);
        HttpResponse response = Mockito.mock(HttpResponse.class);
        DefaultAcsClient spyClient = Mockito.spy(client);
        Mockito.when(response.getStatus()).thenReturn(401);
        Mockito.when(response.getHttpContentType()).thenReturn(FormatType.XML);
        AcsRequest request = Mockito.mock(AcsRequest.class);
        request.strToSign = "GET&%2F&ssddddfgfK";
        String errorMessage = "signature does not conform to standards. server string to sign is:Error Signature";
        Mockito.when(response.getHttpContentString()).thenReturn(null);
        Mockito.doReturn(response).when(spyClient).doAction(request);
        thrown.expect(ClientException.class);
        thrown.expectMessage(" : ");
        spyClient.getAcsResponse(request);
    }
}
