package rss.feed.extractor.app.config;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.ser.XmlSerializerProvider;
import com.fasterxml.jackson.dataformat.xml.util.XmlRootNameLookup;

@Configuration
public class RSSFeedExtractorAppConfig {
	
	@Bean("xmlMapper")
    XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        XmlSerializerProvider provider = new XmlSerializerProvider(new XmlRootNameLookup());
        provider.setNullValueSerializer(new NullSerializer());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        mapper.setSerializerProvider(provider);
        return mapper;
    }
}

class NullSerializer extends JsonSerializer<Object> {

    /**
     * Serialize.
     *
     * @param value the value
     * @param jsonGenerator the json generator
     * @param provider the provider
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void serialize(Object value, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        jsonGenerator.writeString("");
    }
}