package com.example.backend_academic_monitoring.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
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
    private Integer status;
    private String rude;
}
