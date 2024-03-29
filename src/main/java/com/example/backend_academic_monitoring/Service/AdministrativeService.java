package com.example.backend_academic_monitoring.Service;

import com.example.backend_academic_monitoring.DTO.AdministrativeDTO;
import com.example.backend_academic_monitoring.Entity.PersonEntity;

public interface AdministrativeService {
    public void save(PersonEntity personEntity);
    AdministrativeDTO findByUserId(Integer userId);
}
