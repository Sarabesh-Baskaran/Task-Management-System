package com.taskflow.taskManager.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity  //Entity annotation simply represent the Role class as mapped as database table and managed by hibernate.
@Table(name="roles")  //Table annotation tells us which table we have to use
@Data  //Data annotation automatically generates lombok and automatically reduce boiler plate code
@AllArgsConstructor  //It takes all fields to generate it as parametrized constructor
@NoArgsConstructor  //In entity classes default constructor is must.
public class Role {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)  //We tells hibernate to store it as string in database
    @Column(nullable = false, unique = true)
    private RoleName name;

    public enum RoleName{     //RoleName is a variable
        ROLE_ADMIN,
        ROLE_MANAGER,
        ROLE_DEVELOPER
    }
}
