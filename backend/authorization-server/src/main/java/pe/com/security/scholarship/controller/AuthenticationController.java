package pe.com.security.scholarship.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.security.scholarship.dto.request.RegisterUserRequest;
import pe.com.security.scholarship.dto.request.UpdatePasswordRequest;
import pe.com.security.scholarship.dto.response.RegisteredUserResponse;
import pe.com.security.scholarship.service.AuthenticationService;
import pe.com.security.scholarship.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
//@Tag(name = "Autenticación", description = "Endpoints para el registro, login y cambio de contraseña")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  //@Operation(summary = "Registro de usuario", description = "Crear una cuenta activa con un JWT válido")
  @PostMapping("/register/estudiante")
  public ResponseEntity<ApiResponse<RegisteredUserResponse>> registerStudent(@Valid @RequestBody RegisterUserRequest request) {
    RegisteredUserResponse response = authenticationService.registerStudent(request);
    return ResponseEntity.ok(new ApiResponse<>("Registro exitoso. Ya puede iniciar sesión.", "200", response));
  }

  //@Operation(summary = "Cambio de contraseña", description = "Actualizar las credenciales del usuario")
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/password")
  public ResponseEntity<ApiResponse<String>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
    authenticationService.updatePassword(request);
    return ResponseEntity.ok(new ApiResponse<>("Contraseña actualizada. Su sesión actual expirará pronto.", "200", null));
  }

}
