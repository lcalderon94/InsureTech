package com.insurtech.catastro.service;

import com.insurtech.catastro.model.PropertyInfo;

public interface CatastroService {
    PropertyInfo getProperty(String id);
}
