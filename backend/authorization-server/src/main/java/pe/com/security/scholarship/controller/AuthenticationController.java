package pe.com.learning.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.learning.security.dto.request.AuthLoginRequest;
import pe.com.learning.security.dto.request.AuthRegisterRequest;
import pe.com.learning.security.dto.request.TokenRefreshRequest;
import pe.com.learning.security.dto.request.UpdatePasswordRequest;
import pe.com.learning.security.dto.response.AuthResponse;
import pe.com.learning.security.dto.response.NewUserResponse;
import pe.com.learning.security.service.AuthenticationService;
import pe.com.learning.security.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
//@Tag(name = "Autenticación", description = "Endpoints para el registro, login y cambio de contraseña")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  //@Operation(summary = "Registro de usuario", description = "Crear una cuenta activa con un JWT válido")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<NewUserResponse>> register(@Valid @RequestBody AuthRegisterRequest request) {
    NewUserResponse response = authenticationService.register(request);
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
