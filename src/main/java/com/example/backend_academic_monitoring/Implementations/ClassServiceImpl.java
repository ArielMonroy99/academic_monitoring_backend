package com.example.backend_academic_monitoring.Implementations;

import com.example.backend_academic_monitoring.DTO.ClassDTO;
import com.example.backend_academic_monitoring.DTO.ClassListDTO;
import com.example.backend_academic_monitoring.Entity.ActivityGradeEntity;
import com.example.backend_academic_monitoring.Entity.ClassEntity;
import com.example.backend_academic_monitoring.Entity.StudentEntity;
import com.example.backend_academic_monitoring.Repository.ClassRepository;
import com.example.backend_academic_monitoring.Service.ActivityGradeService;
import com.example.backend_academic_monitoring.Service.ClassService;
import com.example.backend_academic_monitoring.Service.GradeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final GradeService gradeService;
    private final ActivityGradeService activityGradeService;

    public ClassServiceImpl(ClassRepository classRepository, GradeService gradeService, ActivityGradeService activityGradeService) {
        this.classRepository = classRepository;
        this.gradeService = gradeService;
        this.activityGradeService = activityGradeService;
    }

    @Override
    public ClassEntity getClass(Integer classId) {
        return classRepository.getReferenceById(classId);
    }

    @Override
    public void addStudentToClass(Integer classId, StudentEntity student) {
        ClassEntity classEntity = classRepository.getReferenceById(classId);
        classEntity.getStudents().add(student);
        classRepository.save(classEntity);
    }


    @Override
    public List<ClassListDTO> getClassByGradeAndYearAndShift(Integer gradeId, Integer year, Integer shift) {
        List<ClassEntity> classEntities = classRepository.findByGrade_IdAndYearAndShift(gradeId, year, shift);
        return classEntities.stream().map(
                this::getClassDTO
        ).toList();
    }

    @Override
    public String getClassName(Integer classId) {
        ClassEntity classEntity = classRepository.getReferenceById(classId);
        return classEntity.getGrade().getNumber() + "°" + classEntity.getGrade().getSection() + " " + classEntity.getIdentifier();
    }

    @Override
    public ClassEntity getClassByStudentId(Integer studentId) {
        return classRepository.findByStudentsId(studentId);
    }

    @Override
    public ClassEntity getClassByAssignationId(Integer assignationId) {
        return classRepository.findByAssignation(assignationId);
    }

    @Override
    public List<ClassEntity> getClassByGradeAndYear(Integer gradeId, Integer year) {
        return classRepository.findByGrade_IdAndYearAndShift(gradeId, year, 1);
    }

    @Override
    public boolean removeStudentFromClass(ClassEntity newClass, StudentEntity student) {
        ClassEntity prevClass = classRepository.findByStudentIdAndYearAndShift(student.getId(), newClass.getYear(), newClass.getShift());
        if (prevClass == null) {
            return true;
        }
        prevClass.getStudents().remove(student);
        classRepository.save(newClass);
        List<ActivityGradeEntity> activityGradeEntities = activityGradeService.activityGradesByStudentAndYear(student.getId(), prevClass.getYear());
        for (ActivityGradeEntity activityGradeEntity : activityGradeEntities) {
            activityGradeEntity.setStatus(0);
        }
        activityGradeService.updateGrades(activityGradeEntities);
        return true;
    }

    @Override
    public void saveClass(ClassDTO classDTO) {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setYear(classDTO.getYear());
        classEntity.setShift(classDTO.getShift());
        classEntity.setIdentifier(classDTO.getIdentifier());
        classEntity.setGrade(gradeService.getById(classDTO.getGradeId()));
        classEntity.setStatus(1);
        classRepository.save(classEntity);
    }

    @Override
    public List<ClassEntity> getStudentClasses(Integer studentId) {
        return List.of();
    }

    @Override
    public void setApprovalStatus(Integer studentId, Integer classId, Integer approvalStatus) {

    }

    @Override
    public ClassListDTO getClassDTOByStudentIdAndYear(Integer studentId, Integer year) {
        ClassEntity classEntity = classRepository.findByStudentIdAndYear(studentId, year);
        if (classEntity == null) return null;
        return getClassDTO(classEntity);
    }

    public ClassListDTO getClassDTO(ClassEntity classEntity) {
        ClassListDTO classListDTO = new ClassListDTO();
        classListDTO.setId(classEntity.getId());
        classListDTO.setYear(classEntity.getYear());
        classListDTO.setShift(classEntity.getShift());
        classListDTO.setIdentifier(classEntity.getIdentifier());
        classListDTO.setGrade(classEntity.getGrade().getNumber() + "°" + classEntity.getGrade().getSection());
        classListDTO.setStudentCount(classRepository.getStudentCount(classEntity.getId()));
        return classListDTO;
    }


}
