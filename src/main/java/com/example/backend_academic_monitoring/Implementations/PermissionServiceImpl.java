package com.example.backend_academic_monitoring.Implementations;

import com.example.backend_academic_monitoring.DTO.PermissionCreateDTO;
import com.example.backend_academic_monitoring.DTO.PermissionDTO;
import com.example.backend_academic_monitoring.Entity.ImageEntity;
import com.example.backend_academic_monitoring.Entity.PermissionEntity;
import com.example.backend_academic_monitoring.Entity.RejectedPermissionEntity;
import com.example.backend_academic_monitoring.Repository.PermissionRepository;
import com.example.backend_academic_monitoring.Repository.RejectPermissionRepository;
import com.example.backend_academic_monitoring.Service.ImageService;
import com.example.backend_academic_monitoring.Service.PermissionService;
import com.example.backend_academic_monitoring.Service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


@Service
public class PermissionServiceImpl implements PermissionService {

    public static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);
    private final PermissionRepository permissionRepository;
    private final ImageService imageService;
    private final StudentService studentService;
    private final RejectPermissionRepository rejectPermissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository, ImageService imageService, StudentService studentService, RejectPermissionRepository rejectPermissionRepository) {
        this.permissionRepository = permissionRepository;
        this.imageService = imageService;
        this.studentService = studentService;

        this.rejectPermissionRepository = rejectPermissionRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void savePermission(PermissionCreateDTO permissionCreateDTO, MultipartFile[] images) {

        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setDate(permissionCreateDTO.getDate());
        permissionEntity.setPermissionStartDate(permissionCreateDTO.getPermissionStartDate());
        permissionEntity.setPermissionEndDate(permissionCreateDTO.getPermissionEndDate());
        permissionEntity.setReason(permissionCreateDTO.getReason());
        permissionEntity.setStatus(1);
        permissionEntity.setPermissionStatus(0);
        permissionEntity.setStudentId(permissionCreateDTO.getStudentId());
        List<ImageEntity> imageEntities = new ArrayList<>();
        for (MultipartFile image : images) {
            ImageEntity imageEntity = imageService.saveFile(image);
            imageEntities.add(imageEntity);
        }
        permissionEntity.setImages(imageEntities);
        permissionRepository.save(permissionEntity);
    }

    @Override
    public List<PermissionDTO> getAllPermissions() {
        List<PermissionEntity> permissionEntities = permissionRepository.findAll();
        return getPermissionDTOS(permissionEntities);
    }

    @Override
    public List<PermissionDTO> getPermissionStatus(Integer statusId) {
        List<PermissionEntity> permissionEntities =
                permissionRepository.findAllByPermissionStatusAndDateAfter(statusId, new Date(System.currentTimeMillis()));
        return getPermissionDTOS(permissionEntities);
    }

    private List<PermissionDTO> getPermissionDTOS(List<PermissionEntity> permissionEntities) {
        List<PermissionDTO> permissionDTOS = new ArrayList<>();
        for (PermissionEntity permissionEntity : permissionEntities) {
            PermissionDTO permissionDTO = new PermissionDTO();
            permissionDTO.setId(permissionEntity.getId());
            permissionDTO.setDate(permissionEntity.getDate());
            permissionDTO.setPermissionStartDate(permissionEntity.getPermissionStartDate());
            permissionDTO.setPermissionEndDate(permissionEntity.getPermissionEndDate());
            permissionDTO.setPermissionStatus(permissionEntity.getPermissionStatus());
            permissionDTO.setReason(permissionEntity.getReason());
            permissionDTO.setStudent(studentService.getStudent(permissionEntity.getStudentId()));
            List<String> images = new ArrayList<>();
            for (ImageEntity imageEntity : permissionEntity.getImages()) {
                images.add(imageService.getImageURL(imageEntity.getUuid()));
            }
            permissionDTO.setImages(images);
            permissionDTOS.add(permissionDTO);
            logger.info("Permission DTO {}", permissionDTO);
        }
        return permissionDTOS;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void approvePermission(Integer permissionId) {
        PermissionEntity permissionEntity = permissionRepository.findById(permissionId).orElseThrow(
                () -> new RuntimeException("Permission not found")
        );
        permissionEntity.setPermissionStatus(1);
        permissionRepository.save(permissionEntity);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void rejectPermission(RejectedPermissionEntity rejectedPermissionEntity) {
        PermissionEntity permissionEntity = permissionRepository.findById(rejectedPermissionEntity.getPermissionId()).orElseThrow(
                () -> new RuntimeException("Permission not found")
        );
        permissionEntity.setPermissionStatus(2);
        permissionRepository.save(permissionEntity);
        RejectedPermissionEntity rejectedPermissionEntity1 = new RejectedPermissionEntity();
        rejectedPermissionEntity1.setPermissionId(rejectedPermissionEntity.getPermissionId());
        rejectedPermissionEntity1.setReason(rejectedPermissionEntity.getReason());
        rejectPermissionRepository.save(rejectedPermissionEntity1);
    }
}