package com.insurtech.catastro.client;

import com.insurtech.catastro.model.PropertyInfo;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MockCatastroClient implements CatastroClient {

    private final Map<String, PropertyInfo> data = new HashMap<>();

    @PostConstruct
    public void init() {
        data.put("1", new PropertyInfo("1", "Calle Falsa 123", "Juan Perez"));
        data.put("2", new PropertyInfo("2", "Avenida Siempreviva 742", "Lisa Gomez"));
    }

    @Override
    public PropertyInfo findById(String id) {
        return data.get(id);
    }
}
