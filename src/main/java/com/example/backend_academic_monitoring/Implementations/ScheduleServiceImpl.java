package com.example.backend_academic_monitoring.Implementations;

import com.example.backend_academic_monitoring.Entity.ScheduleEntity;
import com.example.backend_academic_monitoring.Repository.ScheduleRepository;
import com.example.backend_academic_monitoring.Service.ScheduleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleServiceImpl(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public List<ScheduleEntity> findAllByClassId(Integer classId) {
        return scheduleRepository.findAllByClassId(classId);
    }
}
