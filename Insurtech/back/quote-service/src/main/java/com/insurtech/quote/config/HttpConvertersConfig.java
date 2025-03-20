import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.util.Collections;

@Configuration
public class HttpConvertersConfig {
    @Bean
    public HttpMessageConverters messageConverters() {
        // Registra el convertidor de Jackson para JSON
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(Collections.singletonList(converter));
    }
}
