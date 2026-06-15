package io.c4us.masterbackend.DTOs;



import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppUserDTO {
    private String id;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String codeUser;
    private Boolean active;
    private String userProfile;
    // On n'inclut PAS la liste 'structures' ici pour éviter les boucles et les proxies
}
