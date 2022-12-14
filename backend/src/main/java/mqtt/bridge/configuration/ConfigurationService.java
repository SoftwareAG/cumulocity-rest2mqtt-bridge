package mqtt.bridge.configuration;

import com.cumulocity.microservice.context.credentials.MicroserviceCredentials;
import com.cumulocity.model.option.OptionPK;
import com.cumulocity.rest.representation.tenant.OptionRepresentation;
import com.cumulocity.rest.representation.tenant.auth.TrustedCertificateCollectionRepresentation;
import com.cumulocity.rest.representation.tenant.auth.TrustedCertificateRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.option.TenantOptionApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConfigurationService {
    private static final String OPTION_CATEGORY_CONFIGURATION = "mqtt.restBridge.service";

    private static final String OPTION_KEY_CONFIGURATION = "credentials.connection.configuration";
    private static final String OPTION_KEY_SERVICE_CONFIGURATION = "service.configuration";

    private final TenantOptionApi tenantOptionApi;
    
    private final Platform platform;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public ConfigurationService(TenantOptionApi tenantOptionApi, Platform platform) {
        this.tenantOptionApi = tenantOptionApi;
        this.platform = platform;
    }

    public TrustedCertificateRepresentation loadCertificateByName(String certificateName, MicroserviceCredentials credentials) {
        TrustedCertificateRepresentation[] results = { new TrustedCertificateRepresentation() };
        TrustedCertificateCollectionRepresentation certificates = platform.rest().get(String.format("/tenant/tenants/%s/trusted-certificates", credentials.getTenant()), MediaType.APPLICATION_JSON_TYPE, TrustedCertificateCollectionRepresentation.class);        
        certificates.forEach(cert -> {
            if ( cert.getName().equals(certificateName)) {
                results[0] = cert;
            log.debug("Found certificate with fingerprint: {} with name: {}", cert.getFingerprint(), cert.getName() );
            }
        });
        return results[0];
    }

    public void saveServiceConfiguration(final ServiceConfiguration configuration) throws JsonProcessingException {
        if (configuration == null) {
            return;
        }

        final String configurationJson = objectMapper.writeValueAsString(configuration);
        final OptionRepresentation optionRepresentation = new OptionRepresentation();
        optionRepresentation.setCategory(OPTION_CATEGORY_CONFIGURATION);
        optionRepresentation.setKey(OPTION_KEY_SERVICE_CONFIGURATION);
        optionRepresentation.setValue(configurationJson);

        tenantOptionApi.save(optionRepresentation);
    }

    public void saveConnectionConfiguration(final ConfigurationConnection configuration) throws JsonProcessingException {
        if (configuration == null) {
            return;
        }

        final String configurationJson = objectMapper.writeValueAsString(configuration);
        final OptionRepresentation optionRepresentation = new OptionRepresentation();
        optionRepresentation.setCategory(OPTION_CATEGORY_CONFIGURATION);
        optionRepresentation.setKey(OPTION_KEY_CONFIGURATION);
        optionRepresentation.setValue(configurationJson);

        tenantOptionApi.save(optionRepresentation);
    }

    public ConfigurationConnection loadConnectionConfiguration() {
        final OptionPK option = new OptionPK();
        option.setCategory(OPTION_CATEGORY_CONFIGURATION);
        option.setKey(OPTION_KEY_CONFIGURATION);
        try {
            final OptionRepresentation optionRepresentation = tenantOptionApi.getOption(option);
            final ConfigurationConnection configuration = objectMapper.readValue(optionRepresentation.getValue(), ConfigurationConnection.class);
            log.info("Returning configuration found: {}:", configuration.mqttHost );
            return configuration;
        } catch (SDKException exception) {
            log.info("No configuration found, returning empty element!");
            //exception.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ServiceConfiguration loadServiceConfiguration() {
        final OptionPK option = new OptionPK();
        option.setCategory(OPTION_CATEGORY_CONFIGURATION);
        option.setKey(OPTION_KEY_SERVICE_CONFIGURATION);
        try {
            final OptionRepresentation optionRepresentation = tenantOptionApi.getOption(option);
            final ServiceConfiguration configuration = new ObjectMapper().readValue(optionRepresentation.getValue(), ServiceConfiguration.class);
            log.debug("Returning service configuration found: {}:", configuration.logPayload );
            return configuration;
        } catch (SDKException exception) {
            log.warn("No configuration found, returning empty element!");
            return new ServiceConfiguration();
            //exception.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteConfiguration() {
        final OptionPK optionPK = new OptionPK();
        optionPK.setKey(OPTION_KEY_CONFIGURATION);
        optionPK.setCategory(OPTION_CATEGORY_CONFIGURATION);

        tenantOptionApi.delete(optionPK);
    }

    public ConfigurationConnection enableConnection( boolean enabled) {
        final OptionPK option = new OptionPK();
        option.setCategory(OPTION_CATEGORY_CONFIGURATION);
        option.setKey(OPTION_KEY_CONFIGURATION);
        try {
            final OptionRepresentation optionRepresentation = tenantOptionApi.getOption(option);
            final ConfigurationConnection configuration = objectMapper.readValue(optionRepresentation.getValue(), ConfigurationConnection.class);
            configuration.enabled = enabled;
            log.info("Setting connection: {}:", configuration.enabled );
            final String configurationJson = objectMapper.writeValueAsString(configuration);
            optionRepresentation.setCategory(OPTION_CATEGORY_CONFIGURATION);
            optionRepresentation.setKey(OPTION_KEY_CONFIGURATION);
            optionRepresentation.setValue(configurationJson);
            tenantOptionApi.save(optionRepresentation);
            return configuration;
        } catch (SDKException exception) {
            log.info("No configuration found, returning empty element!");
            //exception.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
