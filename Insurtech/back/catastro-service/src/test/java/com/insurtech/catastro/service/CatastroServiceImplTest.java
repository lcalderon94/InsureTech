package com.insurtech.catastro.service;

import com.insurtech.catastro.client.CatastroClient;
import com.insurtech.catastro.model.PropertyInfo;
import com.insurtech.catastro.service.impl.CatastroServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatastroServiceImplTest {

    @Test
    void getPropertyReturnsClientResult() {
        CatastroClient client = mock(CatastroClient.class);
        PropertyInfo property = new PropertyInfo("1", "Street 1", "Owner");
        when(client.findById("1")).thenReturn(property);
        CatastroService service = new CatastroServiceImpl(client);
        PropertyInfo result = service.getProperty("1");
        assertEquals(property, result);
    }
}
