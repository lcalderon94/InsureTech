package com.insurtech.catastro.client;

import com.insurtech.catastro.model.PropertyInfo;

public interface CatastroClient {
    PropertyInfo findById(String id);
}
