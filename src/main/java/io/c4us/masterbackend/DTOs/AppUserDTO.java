package io.c4us.masterbackend.DTOs;



import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
 //   @Column(name = "last_sync_date")
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime lastSyncDate;
    // On n'inclut PAS la liste 'structures' ici pour éviter les boucles et les proxies
}
