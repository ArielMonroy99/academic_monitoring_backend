package com.example.backend_academic_monitoring.Implementations;

import com.example.backend_academic_monitoring.DTO.*;
import com.example.backend_academic_monitoring.Entity.*;
import com.example.backend_academic_monitoring.Mappers.PersonMapper;
import com.example.backend_academic_monitoring.Mappers.UserMapper;
import com.example.backend_academic_monitoring.Repository.UserRepository;
import com.example.backend_academic_monitoring.Service.*;
import com.example.backend_academic_monitoring.Utilities.PasswordGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;

import java.util.List;


@Service
public class UserServiceImpl implements UserService {
    public static final String TEACHER_ROLE = "TEACHER";
    public static final String PARENT_ROLE = "PARENT";
    public static final String ADMINISTRATIVE_ROLE = "ADMINISTRATIVE";
    public static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder bCryptPasswordEncoder;
    private final PasswordGenerator passwordGenerator;
    private final EmailService emailService;
    private final PersonService personService;
    private final AdministrativeService administrativeService;
    private final ParentService parentService;
    private final TeacherService teacherService;
    private final ImageService fileService;
    private final StudentService studentService;
    private final ClassAssignationService classAssignationService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder bCryptPasswordEncoder, PasswordGenerator passwordGenerator, EmailService emailService, PersonService personService, AdministrativeService administrativeService, ParentService parentService, TeacherService teacherService, ImageService fileService, StudentService studentService, ClassAssignationService classAssignationService) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.emailService = emailService;
        this.personService = personService;
        this.administrativeService = administrativeService;
        this.parentService = parentService;
        this.teacherService = teacherService;
        this.fileService = fileService;
        this.studentService = studentService;
        this.classAssignationService = classAssignationService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String saveUser(UserCreateDTO userCreateDTO, MultipartFile image, List<SubjectDTO> subjects, List<ConsultHourDTO> consultHours) {
        if (personService.existsByCi(userCreateDTO.getCi())) {
            throw new RuntimeException("La cedula ya existe");
        }
        if (personService.existsByEmail(userCreateDTO.getEmail())) {
            throw new RuntimeException("El email ya existe");
        }
        if (personService.existsByPhone(userCreateDTO.getPhone())) {
            throw new RuntimeException("El telefono ya existe");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userCreateDTO.getCi());
        String generatedPassword = passwordGenerator.generatePassword();
        userEntity.setPassword(bCryptPasswordEncoder.encode(generatedPassword));
        userEntity.setRole(userCreateDTO.getRoles());
        userEntity.setStatus(1);
        if (image != null) {
            Integer imageId = fileService.saveFile(image).getId();
            userEntity.setImageId(imageId);
        }
        userEntity = userRepository.save(userEntity);
        PersonDTO personDTO = new PersonDTO();
        personDTO.setName(userCreateDTO.getName());
        personDTO.setLastname(userCreateDTO.getLastname());
        personDTO.setAddress(userCreateDTO.getAddress());
        personDTO.setPhone(userCreateDTO.getPhone());
        personDTO.setEmail(userCreateDTO.getEmail());
        personDTO.setCi(userCreateDTO.getCi());
        PersonEntity personEntity = personService.save(personDTO, userEntity.getId());
        LOGGER.info("roles {} ", userCreateDTO.getRoles());
        if (userCreateDTO.getRoles().get(0).getName().equals(ADMINISTRATIVE_ROLE)) {
            administrativeService.save(personEntity);
            LOGGER.info("Administrative saved");
        }
        if (userCreateDTO.getRoles().get(0).getName().equals(TEACHER_ROLE)) {
            teacherService.save(personEntity, userCreateDTO.getAcademicEmail(), subjects, consultHours);
            LOGGER.info("Teacher saved");
        }
        if (userCreateDTO.getRoles().get(0).getName().equals(PARENT_ROLE)) {
            parentService.save(personEntity);
            LOGGER.info("Father saved");

        }
        //TODO:Uncomment to launch in production
        Context context = new Context();
        context.setVariable("password", generatedPassword);
        context.setVariable("username", userCreateDTO.getCi());
        emailService.sendPasswordEmail(userCreateDTO.getEmail(), "Contraseña generada", context);
        return "Usuario guardado correctamente";
    }

    @Override
    public void deleteUser(Integer id) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow();
        userEntity.setStatus(0);
        userRepository.save(userEntity);
        personService.delete(userEntity.getId());
    }

    @Override
    public void updateUser(UserCreateDTO userCreateDTO) {
        PersonEntity personEntity = personService.getById(userCreateDTO.getId());
        UserEntity userEntity = userRepository.findById(personEntity.getUserId()).orElseThrow();
        userEntity.setUsername(userCreateDTO.getCi());
        userRepository.save(userEntity);
        PersonDTO personDTO = new PersonDTO();
        personDTO.setId(personEntity.getId());
        personDTO.setName(userCreateDTO.getName());
        personDTO.setLastname(userCreateDTO.getLastname());
        personDTO.setAddress(userCreateDTO.getAddress());
        personDTO.setPhone(userCreateDTO.getPhone());
        personDTO.setEmail(userCreateDTO.getEmail());
        personDTO.setCi(userCreateDTO.getCi());
        personService.save(personDTO, userEntity.getId());
    }

    @Override
    public void blockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username);
        userEntity.setStatus(2);
        userRepository.save(userEntity);
    }

    @Override
    public void unblockUser(String username) {
        UserEntity userEntity = userRepository.findByUsername(username);
        userEntity.setStatus(1);
        userRepository.save(userEntity);
    }

    @Override
    public void updateUserPassword(String username, String password) {
        UserEntity userEntity = userRepository.findByUsername(username);
        userEntity.setPassword(bCryptPasswordEncoder.encode(password));
        userRepository.save(userEntity);
    }

    @Override
    public void updateUserRole(String username, List<RoleEntity> newRoles) {
        UserEntity userEntity = userRepository.findByUsername(username);
        PersonEntity personEntity = personService.getById(userEntity.getId());
        List<RoleEntity> userRoles = userEntity.getRole();
        LOGGER.info("UserRoles: {}", userRoles);
        for (RoleEntity role : newRoles) {
            if (!userEntity.getRole().contains(role)) {
                if (!updateRoleStatus(role, userEntity, 1)) {
                    createRole(role, personEntity);
                }
            }
            LOGGER.info("Role: {}", role);
            userRoles.remove(role);
            LOGGER.info("UserRoles: {}", userRoles);
        }
        for (RoleEntity role : userRoles) {
            updateRoleStatus(role, userEntity, 0);
        }
        userEntity.setRole(newRoles);
        userRepository.save(userEntity);
    }

    private void createRole(RoleEntity role, PersonEntity personEntity) {
        if (role.getName().equals(TEACHER_ROLE)) {
            TeacherEntity teacherEntity = new TeacherEntity();
            teacherEntity.setPerson(personEntity);
            teacherEntity.setStatus(1);
            teacherService.save(personEntity, "", null, null);
        }
        if (role.getName().equals(PARENT_ROLE)) {
            ParentEntity parent = new ParentEntity();
            parent.setStatus(1);
            parent.setPerson(personEntity);
            parentService.save(personEntity);
        }
        if (role.getName().equals(ADMINISTRATIVE_ROLE)) {
            AdministrativeEntity administrative = new AdministrativeEntity();
            administrative.setStatus(1);
            administrative.setPerson(personEntity);
            administrativeService.save(personEntity);
        }
    }

    private boolean updateRoleStatus(RoleEntity role, UserEntity user, Integer status) {
        if (role.getName().equals(TEACHER_ROLE)) {
            TeacherEntity teacherEntity = teacherService.findTeacherEntityByUserId(user.getId());
            if (teacherEntity != null) {
                teacherEntity.setStatus(status);
                return true;
            }
        }
        if (role.getName().equals(PARENT_ROLE)) {
            ParentEntity parent = parentService.getParentEntityByUserId(user.getId());
            if (parent != null) {
                parent.setStatus(status);
                return true;
            }
        }
        if (role.getName().equals(ADMINISTRATIVE_ROLE)) {
            AdministrativeEntity administrative = administrativeService.findEntityByUserId(user.getId());
            if (administrative != null) {
                administrative.setStatus(status);
                return true;
            }
        }
        return false;
    }


    @Override
    public UserDataDTO getUser(Integer id) {
        UserEntity user = userRepository.findById(id).orElseThrow();
        PersonDTO personDTO;
        personDTO = PersonMapper.entityToDTO(personService.getById(user.getId()));
        LOGGER.info("PersonDTO: {}, UserEntity {}", personDTO.getId(), user.getId());
        UserDataDTO userDataDTO = UserMapper.entityToData(user, personDTO);
        if (user.getImageId() != null) {
            String uuid = fileService.getImage(user.getImageId()).getUuid();
            fileService.getImageURL(uuid);
        }
        LOGGER.info(String.valueOf(user));
        return userDataDTO;
    }


    @Override
    public Page<UserDataDTO> searchUser(String name, String lastname, String role, String ci, Integer page, Integer size) {
        Page<PersonEntity> personList = personService.findAllByNameOrCI(name, lastname, ci, role, page, size);
        LOGGER.info("PersonList: {}", personList.getContent());
        return personList.map(person -> {
            UserEntity user = userRepository.findById(person.getUserId()).orElseThrow();
            UserDataDTO userDataDTO = UserMapper.entityToData(user, PersonMapper.entityToDTO(person));
            if (user.getImageId() != null) {
                String uuid = fileService.getImage(user.getImageId()).getUuid();
                userDataDTO.setImageUrl(fileService.getImageURL(uuid));
            }
            return userDataDTO;
        });
    }

    @Override
    public void updateUserImage(Integer id, MultipartFile image) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow();
        fileService.deleteImage(userEntity.getImageId());
        Integer imageId = fileService.saveFile(image).getId();
        userEntity.setImageId(imageId);
        userRepository.save(userEntity);
    }

    @Override
    public boolean isUsernameAvaiable(String username) {
        return !userRepository.existsByUsername(username);
    }


    public UserDTO getUserByUsername(String username) {
        UserEntity user = userRepository.findByUsernameAndStatus(username, 1);
        if (user == null) return null;
        return UserMapper.entityToDTO(user);
    }

    @Override
    public UserDTO getUserByPersonId(Integer id) {
        return UserMapper.entityToDTO(userRepository.findByPersonId(id));
    }

    @Override
    public UserDetailsDTO getUserRoleDetails(String username, String role) {
        LOGGER.info("Username: {}, Role: {}", username, role);
        UserDTO user = this.getUserByUsername(username);
        UserDetailsDTO userDetails = new UserDetailsDTO();
        ObjectMapper mapper = new ObjectMapper();
        if (!user.getRole().stream().filter(roleEntity -> role.equals(roleEntity.getName())).toList().isEmpty()) {
            if (role.equals(PARENT_ROLE)) {
                LOGGER.info("Parent");
                userDetails.setDetails(parentService.getParentDTOByUserId(user.getId()));
            }
            if (role.equals(TEACHER_ROLE)) {
                userDetails.setDetails(teacherService.findTeacherDTOByUserId(user.getId()));
            }
            if (role.equals(ADMINISTRATIVE_ROLE)) {
                userDetails.setDetails(administrativeService.findDTOByUserId(user.getId()));
            }
        } else {
            throw new RuntimeException("El usuario no tiene el rol solicitado");
        }
        userDetails.setUser(user);
        LOGGER.info("{}", userDetails);
        if (role.equals(PARENT_ROLE)) {
            ParentDTO parent = mapper.convertValue(userDetails.getDetails(), ParentDTO.class);
            userDetails.setStudents(studentService.findAllByParentId(parent.getId()));
        }
        if (role.equals(TEACHER_ROLE)) {
            TeacherDTO teacher = mapper.convertValue(userDetails.getDetails(), TeacherDTO.class);
            userDetails.setClassAssignations(classAssignationService.getClassAssignationByTeacherId(teacher.getId()));
        }
        userDetails.setRole(role);
        return userDetails;
    }


}
