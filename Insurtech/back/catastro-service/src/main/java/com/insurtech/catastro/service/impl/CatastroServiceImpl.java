package com.insurtech.catastro.service.impl;

import com.insurtech.catastro.client.CatastroClient;
import com.insurtech.catastro.model.PropertyInfo;
import com.insurtech.catastro.service.CatastroService;
import org.springframework.stereotype.Service;

@Service
public class CatastroServiceImpl implements CatastroService {

    private final CatastroClient client;

    public CatastroServiceImpl(CatastroClient client) {
        this.client = client;
    }

    @Override
    public PropertyInfo getProperty(String id) {
        return client.findById(id);
    }
}
