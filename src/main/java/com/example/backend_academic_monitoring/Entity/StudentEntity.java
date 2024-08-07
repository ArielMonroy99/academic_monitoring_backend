package com.example.backend_academic_monitoring.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import javax.annotation.Nullable;
import java.sql.Date;
import java.util.List;

@Entity
@Table(name = "student")
@Data
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String ci;
    private String fatherLastname;
    private String motherLastname;
    @JsonFormat(pattern = "dd-MM-yyyy", locale = "es_BO", timezone = "GMT-4")
    private Date birthdate;
    private String address;
    private String email;
    private Integer status;
    private String rude;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name = "student_parent",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "parent_id")
    )
    private List<ParentEntity> parents;
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "acad_user_id")
    @Nullable
    private UserEntity user;
}
